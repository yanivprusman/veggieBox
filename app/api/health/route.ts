import { NextResponse } from 'next/server';
import { q } from '@/lib/db';

export async function GET() {
  try {
    const r = await q<{ n: number }>('SELECT COUNT(*) AS n FROM customers');
    return NextResponse.json({ ok: true, customers: r[0]?.n ?? 0 });
  } catch (e) {
    return NextResponse.json({ ok: false, error: String(e) }, { status: 500 });
  }
}
