import { NextResponse } from 'next/server';
import { q } from '@/lib/db';
import { getBusiness, getWorker, intlPhone, renderTemplate, getTemplate } from '@/lib/biz';

function publicBase(req: Request): string {
  return (process.env.PUBLIC_BASE_URL ?? new URL(req.url).origin).replace(/\/$/, '');
}

// Batch greeting: returns a ready-to-send message + wa.me deep link + self-service
// form link for every customer that's missing details (or an explicit id list).
// ?all=1 targets every customer; default targets only those missing an address.
export async function GET(req: Request) {
  try {
    const url = new URL(req.url);
    const business = await getBusiness(url.searchParams.get('business') ?? 'silverman');
    const worker = await getWorker(
      business.id,
      url.searchParams.get('worker') ? Number(url.searchParams.get('worker')) : undefined,
    );
    const all = url.searchParams.get('all') === '1';
    const ids = (url.searchParams.get('ids') ?? '')
      .split(',')
      .map((x) => Number(x))
      .filter(Boolean);

    let where = 'business_id=? AND active=1';
    const params: unknown[] = [business.id];
    if (ids.length) {
      where += ` AND id IN (${ids.map(() => '?').join(',')})`;
      params.push(...ids);
    } else if (!all) {
      where += ' AND (address IS NULL OR address = "")';
    }

    const rows = await q<{ id: number; name: string; phone: string | null; details_token: string }>(
      `SELECT id, name, phone, details_token FROM customers WHERE ${where} ORDER BY name`,
      params,
    );

    const tmpl =
      (await getTemplate(business.id, 'greeting_missing_details')) ||
      'שלום {name}, נשמח לקבל כתובת והוראות הגעה: {link}';
    const base = publicBase(req);

    const targets = rows.map((r) => {
      const link = `${base}/d/${r.details_token}`;
      const message = renderTemplate(tmpl, {
        name: r.name,
        worker: worker.name,
        business: business.name,
        area: business.area ?? '',
        link,
      });
      const phoneIntl = intlPhone(r.phone);
      return {
        customerId: r.id,
        name: r.name,
        phoneIntl,
        link,
        message,
        waUrl: phoneIntl ? `https://wa.me/${phoneIntl}?text=${encodeURIComponent(message)}` : null,
      };
    });

    return NextResponse.json({ ok: true, count: targets.length, targets });
  } catch (e) {
    return NextResponse.json({ ok: false, error: String(e) }, { status: 500 });
  }
}
