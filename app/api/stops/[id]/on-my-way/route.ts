import { NextResponse } from 'next/server';
import { q, exec } from '@/lib/db';
import { intlPhone, renderTemplate, getTemplate } from '@/lib/biz';

// Build the "I'm on my way" WhatsApp message for a stop and record that it was
// sent. Returns a wa.me deep link so the message goes FROM the worker's own number
// (Yehudit's request: customers should recognise the deliverer). A future bridge
// sender can hook in here behind WHATSAPP_SENDER=bridge.
export async function POST(req: Request, ctx: { params: Promise<{ id: string }> }) {
  try {
    const { id } = await ctx.params;
    const stopId = Number(id);

    const rows = await q<{
      phone: string | null;
      name: string;
      business_id: number;
      business_name: string;
      worker_name: string | null;
    }>(
      `SELECT c.phone, c.name, c.business_id,
              b.name AS business_name,
              w.name AS worker_name
       FROM route_stops s
       JOIN customers c ON c.id=s.customer_id
       JOIN routes r ON r.id=s.route_id
       JOIN businesses b ON b.id=c.business_id
       LEFT JOIN workers w ON w.id=r.worker_id
       WHERE s.id=? LIMIT 1`,
      [stopId],
    );
    const row = rows[0];
    if (!row) return NextResponse.json({ ok: false, error: 'stop not found' }, { status: 404 });

    const tmpl = (await getTemplate(row.business_id, 'on_my_way')) ||
      'שלום {name}, אני בדרך אליכם עם ההזמנה 🚙📦';
    const message = renderTemplate(tmpl, {
      name: row.name,
      worker: row.worker_name ?? '',
      business: row.business_name,
    });
    const phoneIntl = intlPhone(row.phone);
    const waUrl = phoneIntl
      ? `https://wa.me/${phoneIntl}?text=${encodeURIComponent(message)}`
      : null;

    await exec('UPDATE route_stops SET on_my_way_sent_at=NOW() WHERE id=?', [stopId]);

    return NextResponse.json({ ok: true, message, phoneIntl, waUrl });
  } catch (e) {
    return NextResponse.json({ ok: false, error: String(e) }, { status: 500 });
  }
}
