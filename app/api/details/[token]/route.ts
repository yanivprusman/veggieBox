import { NextResponse } from 'next/server';
import { q, exec } from '@/lib/db';
import { geocodeAddress } from '@/lib/geocode';

// Public, token-gated self-service endpoint. The token is the secret; no login.

export async function GET(_req: Request, ctx: { params: Promise<{ token: string }> }) {
  try {
    const { token } = await ctx.params;
    const rows = await q<Record<string, unknown>>(
      `SELECT c.name, c.address, c.house_instructions AS houseInstructions,
              c.drop_preference AS dropPreference, c.lat, c.lon,
              b.name AS businessName, b.area, b.default_central_drop AS centralDrop,
              b.map_center_lat AS mapLat, b.map_center_lon AS mapLon
       FROM customers c JOIN businesses b ON b.id=c.business_id
       WHERE c.details_token=? LIMIT 1`,
      [token],
    );
    if (!rows[0]) return NextResponse.json({ ok: false, error: 'not found' }, { status: 404 });
    return NextResponse.json({ ok: true, customer: rows[0] });
  } catch (e) {
    return NextResponse.json({ ok: false, error: String(e) }, { status: 500 });
  }
}

// Save a customer's own submission.
export async function POST(req: Request, ctx: { params: Promise<{ token: string }> }) {
  try {
    const { token } = await ctx.params;
    const b = await req.json();
    const found = await q<{ id: number; area: string | null; mlat: number | null; mlon: number | null }>(
      `SELECT c.id, b.area, b.map_center_lat AS mlat, b.map_center_lon AS mlon
       FROM customers c JOIN businesses b ON b.id=c.business_id
       WHERE c.details_token=? LIMIT 1`,
      [token],
    );
    if (!found[0]) return NextResponse.json({ ok: false, error: 'not found' }, { status: 404 });

    const dropPref =
      b.dropPreference === 'central' || b.dropPreference === 'beside' ? b.dropPreference : null;

    let lat = b.lat != null ? Number(b.lat) : null;
    let lon = b.lon != null ? Number(b.lon) : null;
    // If the customer typed an address but didn't share their location, try to
    // geocode it (best-effort; small-village streets usually aren't in OSM).
    if ((lat == null || lon == null) && b.address) {
      const center =
        found[0].mlat != null && found[0].mlon != null
          ? { lat: found[0].mlat, lon: found[0].mlon }
          : null;
      const geo = await geocodeAddress(String(b.address), found[0].area, center);
      if (geo) {
        lat = geo.lat;
        lon = geo.lon;
      }
    }

    await exec(
      `UPDATE customers
       SET address=?, house_instructions=?, drop_preference=?, lat=?, lon=?,
           details_filled_at=NOW()
       WHERE id=?`,
      [b.address ?? null, b.houseInstructions ?? null, dropPref, lat, lon, found[0].id],
    );
    return NextResponse.json({ ok: true, geocoded: lat != null });
  } catch (e) {
    return NextResponse.json({ ok: false, error: String(e) }, { status: 500 });
  }
}
