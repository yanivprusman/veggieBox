import { NextResponse } from 'next/server';
import { q } from '@/lib/db';
import { getBusiness, getWorker } from '@/lib/biz';

// Earnings = delivered stops × rate_per_delivery, for today / this week / all time.
export async function GET(req: Request) {
  try {
    const url = new URL(req.url);
    const business = await getBusiness(url.searchParams.get('business') ?? 'silverman');
    const workerParam = url.searchParams.get('worker');
    const worker = await getWorker(business.id, workerParam ? Number(workerParam) : undefined);
    const rate = Number(business.rate_per_delivery);

    const counts = await q<{ period: string; n: number }>(
      `SELECT 'today' AS period, COUNT(*) AS n FROM route_stops s
         JOIN routes r ON r.id=s.route_id
         WHERE r.worker_id=? AND s.status='delivered' AND r.route_date=CURDATE()
       UNION ALL
       SELECT 'week', COUNT(*) FROM route_stops s
         JOIN routes r ON r.id=s.route_id
         WHERE r.worker_id=? AND s.status='delivered' AND YEARWEEK(r.route_date,1)=YEARWEEK(CURDATE(),1)
       UNION ALL
       SELECT 'all', COUNT(*) FROM route_stops s
         JOIN routes r ON r.id=s.route_id
         WHERE r.worker_id=? AND s.status='delivered'`,
      [worker.id, worker.id, worker.id],
    );
    const by: Record<string, number> = {};
    for (const c of counts) by[c.period] = c.n;
    const mk = (n: number) => ({ deliveries: n, amount: n * rate });
    return NextResponse.json({
      ok: true,
      rate,
      currency: business.currency,
      today: mk(by.today ?? 0),
      week: mk(by.week ?? 0),
      all: mk(by.all ?? 0),
    });
  } catch (e) {
    return NextResponse.json({ ok: false, error: String(e) }, { status: 500 });
  }
}
