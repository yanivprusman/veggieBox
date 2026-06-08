import { NextResponse } from 'next/server';
import {
  getBusiness,
  getWorker,
  getOrCreateTodayRoute,
  getStops,
  computeProgress,
} from '@/lib/biz';

// The main feed for the Android app: today's route with ordered stops + customer
// data + progress. Creates the route (and back-fills stops) on first call.
export async function GET(req: Request) {
  try {
    const url = new URL(req.url);
    const slug = url.searchParams.get('business') ?? 'silverman';
    const workerParam = url.searchParams.get('worker');
    const business = await getBusiness(slug);
    const worker = await getWorker(business.id, workerParam ? Number(workerParam) : undefined);
    const routeId = await getOrCreateTodayRoute(business.id, worker.id);
    const stops = await getStops(routeId);
    return NextResponse.json({
      ok: true,
      business: {
        id: business.id,
        slug: business.slug,
        name: business.name,
        area: business.area,
        defaultCentralDrop: business.default_central_drop,
        ratePerDelivery: Number(business.rate_per_delivery),
        currency: business.currency,
        mapCenterLat: business.map_center_lat,
        mapCenterLon: business.map_center_lon,
      },
      worker: { id: worker.id, name: worker.name },
      routeId,
      stops,
      progress: computeProgress(stops),
    });
  } catch (e) {
    return NextResponse.json({ ok: false, error: String(e) }, { status: 500 });
  }
}
