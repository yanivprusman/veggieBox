// Best-effort geocoding via OpenStreetMap Nominatim.
//
// IMPORTANT: many small-village addresses (e.g. Midreshet Ben-Gurion's internal
// streets like משעול מרווה / שבלול) are NOT in OSM, so this returns null there.
// The reliable source of map coordinates is GPS — the worker's "pin this house"
// button or the customer sharing their location in the self-service form. This
// helper just opportunistically fills coords for addresses that ARE in OSM
// (helpful for other towns / future businesses), and never blocks the save.

export async function geocodeAddress(
  address: string,
  area: string | null,
  center?: { lat: number; lon: number } | null,
): Promise<{ lat: number; lon: number } | null> {
  if (!address || !address.trim()) return null;
  const base = address.split(',')[0].trim();
  const queries = [
    `${address}, ישראל`,
    area ? `${base}, ${area}, ישראל` : null,
  ].filter(Boolean) as string[];

  for (const q of queries) {
    try {
      const url =
        'https://nominatim.openstreetmap.org/search?' +
        new URLSearchParams({ q, format: 'json', limit: '1', 'accept-language': 'he' });
      const ctrl = new AbortController();
      const t = setTimeout(() => ctrl.abort(), 6000);
      const r = await fetch(url, {
        headers: { 'User-Agent': 'veggieBox-geocode/1.0 (yaniv)' },
        signal: ctrl.signal,
      });
      clearTimeout(t);
      if (!r.ok) continue;
      const d = await r.json();
      const hit = Array.isArray(d) ? d[0] : null;
      if (hit) {
        const lat = parseFloat(hit.lat);
        const lon = parseFloat(hit.lon);
        if (Number.isFinite(lat) && Number.isFinite(lon)) {
          // If we know the business centre, reject results more than ~6km away
          // (a wrong-city match is worse than no pin).
          if (center && (Math.abs(lat - center.lat) > 0.06 || Math.abs(lon - center.lon) > 0.06)) {
            return null;
          }
          return { lat, lon };
        }
      }
    } catch {
      // network/timeout — best-effort, ignore
    }
  }
  return null;
}
