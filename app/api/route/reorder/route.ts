import { NextResponse } from 'next/server';
import { exec } from '@/lib/db';

// Manual reorder: body { order: [stopId, ...] } in the desired sequence.
export async function POST(req: Request) {
  try {
    const body = await req.json();
    const order: number[] = Array.isArray(body.order) ? body.order.map(Number) : [];
    if (!order.length) return NextResponse.json({ ok: false, error: 'order required' }, { status: 400 });
    let seq = 1;
    for (const stopId of order) {
      await exec('UPDATE route_stops SET seq=? WHERE id=?', [seq++, stopId]);
    }
    return NextResponse.json({ ok: true });
  } catch (e) {
    return NextResponse.json({ ok: false, error: String(e) }, { status: 500 });
  }
}
