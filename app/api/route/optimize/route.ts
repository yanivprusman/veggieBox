import { NextResponse } from 'next/server';
import { exec, q } from '@/lib/db';
import { getStops, nearestNeighbourOrder, resolveStartPoint } from '@/lib/biz';

// Reorder a route's stops by nearest-neighbour from a start point. Priority:
// explicit GPS (startLat/startLon) -> the business central drop (pallet point) ->
// the route's first geocoded stop. Writes new seq values.
export async function POST(req: Request) {
  try {
    const body = await req.json();
    const routeId = Number(body.routeId);
    if (!routeId) return NextResponse.json({ ok: false, error: 'routeId required' }, { status: 400 });

    const stops = await getStops(routeId);
    let start: { lat: number; lon: number } | null =
      body.startLat != null && body.startLon != null
        ? { lat: Number(body.startLat), lon: Number(body.startLon) }
        : null;
    if (!start) {
      const brows = await q<{ clat: number | null; clon: number | null }>(
        `SELECT b.central_drop_lat AS clat, b.central_drop_lon AS clon
         FROM routes r JOIN businesses b ON b.id = r.business_id WHERE r.id=? LIMIT 1`,
        [routeId],
      );
      start = resolveStartPoint({ lat: brows[0]?.clat ?? null, lon: brows[0]?.clon ?? null }, stops);
    }
    if (!start) {
      return NextResponse.json(
        { ok: false, error: 'no start point and no geocoded stops to optimise from' },
        { status: 400 },
      );
    }

    const order = nearestNeighbourOrder(stops, start);
    let seq = 1;
    for (const stopId of order) {
      await exec('UPDATE route_stops SET seq=? WHERE id=?', [seq++, stopId]);
    }
    return NextResponse.json({ ok: true, order });
  } catch (e) {
    return NextResponse.json({ ok: false, error: String(e) }, { status: 500 });
  }
}
