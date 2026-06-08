import { q, exec } from './db';

export type Business = {
  id: number;
  slug: string;
  name: string;
  area: string | null;
  default_central_drop: string | null;
  rate_per_delivery: string; // DECIMAL comes back as string
  currency: string;
  public_base_url: string | null;
  map_center_lat: number | null;
  map_center_lon: number | null;
  // Central drop point (where the pallet of cartons is dropped, and where the
  // worker usually starts the route). Used as the default optimise start point.
  central_drop_lat: number | null;
  central_drop_lon: number | null;
};

export type Worker = { id: number; business_id: number; name: string; phone: string | null };

export type Stop = {
  stopId: number;
  customerId: number;
  name: string;
  phone: string | null;
  phoneIntl: string | null;
  address: string | null;
  houseInstructions: string | null;
  lat: number | null;
  lon: number | null;
  dropPreference: 'central' | 'beside' | null;
  detailsToken: string | null;
  hasDetails: boolean;
  seq: number;
  status: 'pending' | 'delivered' | 'not_home' | 'skipped';
  cartons: number;
  dropUsed: 'home' | 'central' | 'beside' | null;
  mediaPath: string | null;
  onMyWaySentAt: string | null;
  deliveredAt: string | null;
};

// Israeli local number (05XXXXXXXX) -> intl (9725XXXXXXXX) for wa.me links.
export function intlPhone(local: string | null | undefined): string | null {
  if (!local) return null;
  const d = local.replace(/\D/g, '');
  if (!d) return null;
  if (d.startsWith('972')) return d;
  if (d.startsWith('0')) return '972' + d.slice(1);
  return d;
}

// {name} {worker} {link} {business} {area} substitution.
export function renderTemplate(body: string, vars: Record<string, string>): string {
  return body.replace(/\{(\w+)\}/g, (_, k) => (k in vars ? vars[k] : `{${k}}`));
}

export async function getBusiness(slug = 'silverman'): Promise<Business> {
  const rows = await q<Business>('SELECT * FROM businesses WHERE slug=? LIMIT 1', [slug]);
  if (!rows[0]) throw new Error(`business not found: ${slug}`);
  return rows[0];
}

export async function getWorker(businessId: number, workerId?: number): Promise<Worker> {
  const rows = workerId
    ? await q<Worker>('SELECT * FROM workers WHERE id=? AND business_id=? LIMIT 1', [workerId, businessId])
    : await q<Worker>('SELECT * FROM workers WHERE business_id=? ORDER BY id LIMIT 1', [businessId]);
  if (!rows[0]) throw new Error('worker not found');
  return rows[0];
}

export async function getTemplate(businessId: number, key: string): Promise<string> {
  const rows = await q<{ body: string }>(
    'SELECT body FROM message_templates WHERE business_id=? AND key_name=? LIMIT 1',
    [businessId, key],
  );
  return rows[0]?.body ?? '';
}

// Resolve the start point for a geographic optimise: the business central drop
// (pallet drop) if it has coords, otherwise the first geocoded stop. Returns null
// when nothing usable exists. Shared by the on-create auto-order and /optimize.
export function resolveStartPoint(
  central: { lat: number | null; lon: number | null },
  stops: { lat: number | null; lon: number | null }[],
): { lat: number; lon: number } | null {
  if (central.lat != null && central.lon != null) return { lat: central.lat, lon: central.lon };
  const firstGeo = stops.find((s) => s.lat != null && s.lon != null);
  return firstGeo ? { lat: firstGeo.lat as number, lon: firstGeo.lon as number } : null;
}

// Find (or create) today's active route for the worker, and make sure every active
// customer has a pending stop on it (so newly added customers show up too).
export async function getOrCreateTodayRoute(businessId: number, workerId: number): Promise<number> {
  const existing = await q<{ id: number }>(
    'SELECT id FROM routes WHERE business_id=? AND worker_id=? AND route_date=CURDATE() LIMIT 1',
    [businessId, workerId],
  );
  let routeId: number;
  let isNewRoute = false;
  if (existing[0]) {
    routeId = existing[0].id;
  } else {
    const res = await exec(
      "INSERT INTO routes (business_id, worker_id, route_date, status) VALUES (?,?,CURDATE(),'active')",
      [businessId, workerId],
    );
    routeId = res.insertId;
    isNewRoute = true;
  }
  // Ensure a stop exists for each active customer not yet on the route.
  await exec(
    `INSERT INTO route_stops (route_id, customer_id, seq, cartons, status)
     SELECT ?, c.id,
            COALESCE(c.sort_hint, 999),
            c.default_cartons, 'pending'
     FROM customers c
     WHERE c.business_id=? AND c.active=1
       AND NOT EXISTS (SELECT 1 FROM route_stops s WHERE s.route_id=? AND s.customer_id=c.id)`,
    [routeId, businessId, routeId],
  );
  // One-time geographic auto-order, only on the day's first creation, so a worker's
  // manual reorder later in the day is never clobbered. Cheap (nearest-neighbour over
  // ~20 stops, in-process) — runs once, not on every load.
  if (isNewRoute) {
    const brows = await q<{ clat: number | null; clon: number | null }>(
      'SELECT central_drop_lat AS clat, central_drop_lon AS clon FROM businesses WHERE id=? LIMIT 1',
      [businessId],
    );
    const stops = await getStops(routeId);
    const start = resolveStartPoint({ lat: brows[0]?.clat ?? null, lon: brows[0]?.clon ?? null }, stops);
    if (start) {
      const order = nearestNeighbourOrder(stops, start);
      let seq = 1;
      for (const stopId of order) {
        await exec('UPDATE route_stops SET seq=? WHERE id=?', [seq++, stopId]);
      }
    }
  }
  return routeId;
}

export async function getStops(routeId: number): Promise<Stop[]> {
  const rows = await q<Record<string, unknown>>(
    `SELECT s.id AS stopId, s.customer_id AS customerId, c.name, c.phone,
            c.address, c.house_instructions AS houseInstructions, c.lat, c.lon,
            c.drop_preference AS dropPreference, c.details_token AS detailsToken,
            c.details_filled_at AS detailsFilledAt,
            s.seq, s.status, s.cartons, s.drop_used AS dropUsed,
            s.media_path AS mediaPath, s.on_my_way_sent_at AS onMyWaySentAt,
            s.delivered_at AS deliveredAt
     FROM route_stops s JOIN customers c ON c.id=s.customer_id
     WHERE s.route_id=?
     ORDER BY s.seq, c.name`,
    [routeId],
  );
  return rows.map((r) => ({
    stopId: r.stopId as number,
    customerId: r.customerId as number,
    name: r.name as string,
    phone: (r.phone as string) ?? null,
    phoneIntl: intlPhone(r.phone as string),
    address: (r.address as string) ?? null,
    houseInstructions: (r.houseInstructions as string) ?? null,
    lat: r.lat as number | null,
    lon: r.lon as number | null,
    dropPreference: (r.dropPreference as Stop['dropPreference']) ?? null,
    detailsToken: (r.detailsToken as string) ?? null,
    hasDetails: !!r.address || !!r.detailsFilledAt,
    seq: r.seq as number,
    status: r.status as Stop['status'],
    cartons: r.cartons as number,
    dropUsed: (r.dropUsed as Stop['dropUsed']) ?? null,
    mediaPath: (r.mediaPath as string) ?? null,
    onMyWaySentAt: (r.onMyWaySentAt as string) ?? null,
    deliveredAt: (r.deliveredAt as string) ?? null,
  }));
}

// Haversine distance in metres.
function dist(a: { lat: number; lon: number }, b: { lat: number; lon: number }): number {
  const R = 6371000;
  const toRad = (x: number) => (x * Math.PI) / 180;
  const dLat = toRad(b.lat - a.lat);
  const dLon = toRad(b.lon - a.lon);
  const s =
    Math.sin(dLat / 2) ** 2 +
    Math.cos(toRad(a.lat)) * Math.cos(toRad(b.lat)) * Math.sin(dLon / 2) ** 2;
  return 2 * R * Math.asin(Math.sqrt(s));
}

// Nearest-neighbour ordering from a start point. Stops without coords keep their
// relative order and are appended at the end.
export function nearestNeighbourOrder(
  stops: { stopId: number; lat: number | null; lon: number | null }[],
  start: { lat: number; lon: number },
): number[] {
  const withCoords = stops.filter((s) => s.lat != null && s.lon != null) as {
    stopId: number;
    lat: number;
    lon: number;
  }[];
  const without = stops.filter((s) => s.lat == null || s.lon == null);
  const order: number[] = [];
  let cur = start;
  const remaining = [...withCoords];
  while (remaining.length) {
    let bestI = 0;
    let bestD = Infinity;
    for (let i = 0; i < remaining.length; i++) {
      const d = dist(cur, remaining[i]);
      if (d < bestD) {
        bestD = d;
        bestI = i;
      }
    }
    const [picked] = remaining.splice(bestI, 1);
    order.push(picked.stopId);
    cur = picked;
  }
  for (const s of without) order.push(s.stopId);
  return order;
}

export type ProgressInfo = {
  total: number;
  delivered: number;
  notHome: number;
  pending: number;
  remaining: number;
};

export function computeProgress(stops: Stop[]): ProgressInfo {
  const delivered = stops.filter((s) => s.status === 'delivered').length;
  const notHome = stops.filter((s) => s.status === 'not_home').length;
  const pending = stops.filter((s) => s.status === 'pending').length;
  return {
    total: stops.length,
    delivered,
    notHome,
    pending,
    remaining: stops.length - delivered - notHome,
  };
}
