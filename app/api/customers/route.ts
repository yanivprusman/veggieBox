import { NextResponse } from 'next/server';
import { q, exec } from '@/lib/db';
import { getBusiness, intlPhone } from '@/lib/biz';

// List all customers for a business (admin / overview).
export async function GET(req: Request) {
  try {
    const url = new URL(req.url);
    const business = await getBusiness(url.searchParams.get('business') ?? 'silverman');
    const rows = await q<Record<string, unknown>>(
      `SELECT id, name, phone, address, house_instructions AS houseInstructions,
              lat, lon, drop_preference AS dropPreference, default_cartons AS defaultCartons,
              details_token AS detailsToken, details_filled_at AS detailsFilledAt, active
       FROM customers WHERE business_id=? ORDER BY COALESCE(sort_hint,999), name`,
      [business.id],
    );
    const customers = rows.map((r) => ({
      ...r,
      phoneIntl: intlPhone(r.phone as string),
      hasDetails: !!r.address || !!r.detailsFilledAt,
    }));
    return NextResponse.json({ ok: true, customers });
  } catch (e) {
    return NextResponse.json({ ok: false, error: String(e) }, { status: 500 });
  }
}

// Create a customer. body: { name, phone?, address?, houseInstructions?, dropPreference?, defaultCartons? }
export async function POST(req: Request) {
  try {
    const url = new URL(req.url);
    const business = await getBusiness(url.searchParams.get('business') ?? 'silverman');
    const b = await req.json();
    if (!b.name) return NextResponse.json({ ok: false, error: 'name required' }, { status: 400 });
    const res = await exec(
      `INSERT INTO customers (business_id, name, phone, address, house_instructions,
        drop_preference, default_cartons, details_token)
       VALUES (?,?,?,?,?,?,?, REPLACE(UUID(),'-',''))`,
      [
        business.id,
        b.name,
        b.phone ?? null,
        b.address ?? null,
        b.houseInstructions ?? null,
        b.dropPreference ?? null,
        b.defaultCartons ?? 1,
      ],
    );
    return NextResponse.json({ ok: true, id: res.insertId });
  } catch (e) {
    return NextResponse.json({ ok: false, error: String(e) }, { status: 500 });
  }
}
