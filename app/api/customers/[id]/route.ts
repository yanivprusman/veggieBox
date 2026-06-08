import { NextResponse } from 'next/server';
import { exec } from '@/lib/db';

// Update a customer. body may include: name, phone, address, houseInstructions,
// lat, lon, dropPreference, defaultCartons, active, sortHint.
export async function PATCH(req: Request, ctx: { params: Promise<{ id: string }> }) {
  try {
    const { id } = await ctx.params;
    const b = await req.json();

    const map: Record<string, string> = {
      name: 'name',
      phone: 'phone',
      address: 'address',
      houseInstructions: 'house_instructions',
      lat: 'lat',
      lon: 'lon',
      dropPreference: 'drop_preference',
      defaultCartons: 'default_cartons',
      active: 'active',
      sortHint: 'sort_hint',
    };
    const sets: string[] = [];
    const vals: unknown[] = [];
    for (const [k, col] of Object.entries(map)) {
      if (b[k] !== undefined) {
        sets.push(`${col}=?`);
        vals.push(b[k]);
      }
    }
    // If an address/coords were filled, stamp details_filled_at.
    if (b.address !== undefined || (b.lat !== undefined && b.lon !== undefined)) {
      sets.push('details_filled_at=COALESCE(details_filled_at, NOW())');
    }
    if (!sets.length) return NextResponse.json({ ok: false, error: 'nothing to update' }, { status: 400 });
    vals.push(Number(id));
    await exec(`UPDATE customers SET ${sets.join(', ')} WHERE id=?`, vals);
    return NextResponse.json({ ok: true });
  } catch (e) {
    return NextResponse.json({ ok: false, error: String(e) }, { status: 500 });
  }
}
