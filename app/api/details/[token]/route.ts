import { NextResponse } from 'next/server';
import { q, exec } from '@/lib/db';

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
    const found = await q<{ id: number }>('SELECT id FROM customers WHERE details_token=? LIMIT 1', [token]);
    if (!found[0]) return NextResponse.json({ ok: false, error: 'not found' }, { status: 404 });

    const dropPref =
      b.dropPreference === 'central' || b.dropPreference === 'beside' ? b.dropPreference : null;
    await exec(
      `UPDATE customers
       SET address=?, house_instructions=?, drop_preference=?, lat=?, lon=?,
           details_filled_at=NOW()
       WHERE id=?`,
      [
        b.address ?? null,
        b.houseInstructions ?? null,
        dropPref,
        b.lat != null ? Number(b.lat) : null,
        b.lon != null ? Number(b.lon) : null,
        found[0].id,
      ],
    );
    return NextResponse.json({ ok: true });
  } catch (e) {
    return NextResponse.json({ ok: false, error: String(e) }, { status: 500 });
  }
}
