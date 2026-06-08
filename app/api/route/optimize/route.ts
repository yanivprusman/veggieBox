import { NextResponse } from 'next/server';
import { exec } from '@/lib/db';
import { getStops, nearestNeighbourOrder } from '@/lib/biz';

// Reorder a route's stops by nearest-neighbour from a start point (the worker's
// current GPS, or the route's first geocoded stop). Writes new seq values.
export async function POST(req: Request) {
  try {
    const body = await req.json();
    const routeId = Number(body.routeId);
    if (!routeId) return NextResponse.json({ ok: false, error: 'routeId required' }, { status: 400 });

    const stops = await getStops(routeId);
    const firstGeo = stops.find((s) => s.lat != null && s.lon != null);
    const start =
      body.startLat != null && body.startLon != null
        ? { lat: Number(body.startLat), lon: Number(body.startLon) }
        : firstGeo
          ? { lat: firstGeo.lat as number, lon: firstGeo.lon as number }
          : null;
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
