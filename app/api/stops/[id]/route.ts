import { NextResponse } from 'next/server';
import { exec } from '@/lib/db';

// Update a single stop's delivery state.
// body: { status?, cartons?, dropUsed?, notes?, mediaPath? }
// status 'delivered' counts toward earnings (including when left at central/beside
// because the customer wasn't home — drop_used records where it ended up).
export async function PATCH(req: Request, ctx: { params: Promise<{ id: string }> }) {
  try {
    const { id } = await ctx.params;
    const stopId = Number(id);
    const body = await req.json();

    const sets: string[] = [];
    const vals: unknown[] = [];

    if (body.status !== undefined) {
      const status = String(body.status);
      if (!['pending', 'delivered', 'not_home', 'skipped'].includes(status)) {
        return NextResponse.json({ ok: false, error: 'bad status' }, { status: 400 });
      }
      sets.push('status=?');
      vals.push(status);
      if (status === 'delivered') {
        sets.push('delivered_at=COALESCE(delivered_at, NOW())');
      } else if (status === 'pending') {
        sets.push('delivered_at=NULL');
      }
    }
    if (body.cartons !== undefined) {
      sets.push('cartons=?');
      vals.push(Number(body.cartons));
    }
    if (body.dropUsed !== undefined) {
      const d = body.dropUsed === null ? null : String(body.dropUsed);
      if (d !== null && !['home', 'central', 'beside'].includes(d)) {
        return NextResponse.json({ ok: false, error: 'bad dropUsed' }, { status: 400 });
      }
      sets.push('drop_used=?');
      vals.push(d);
    }
    if (body.notes !== undefined) {
      sets.push('notes=?');
      vals.push(body.notes === null ? null : String(body.notes));
    }
    if (body.mediaPath !== undefined) {
      sets.push('media_path=?');
      vals.push(body.mediaPath === null ? null : String(body.mediaPath));
    }

    if (!sets.length) return NextResponse.json({ ok: false, error: 'nothing to update' }, { status: 400 });

    vals.push(stopId);
    await exec(`UPDATE route_stops SET ${sets.join(', ')} WHERE id=?`, vals);
    return NextResponse.json({ ok: true });
  } catch (e) {
    return NextResponse.json({ ok: false, error: String(e) }, { status: 500 });
  }
}
