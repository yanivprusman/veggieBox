import { NextResponse } from 'next/server';
import { exec } from '@/lib/db';
import { writeFile, mkdir } from 'fs/promises';
import path from 'path';

export const runtime = 'nodejs';

// Upload a delivery photo/video for a stop (used by the "not home" flow — Yehudit
// asks for a short clip of the access path + where the produce was left).
// multipart/form-data, field name "file". Stored under public/uploads (gitignored).
export async function POST(req: Request, ctx: { params: Promise<{ id: string }> }) {
  try {
    const { id } = await ctx.params;
    const stopId = Number(id);
    const form = await req.formData();
    const file = form.get('file');
    if (!(file instanceof File)) {
      return NextResponse.json({ ok: false, error: 'file field required' }, { status: 400 });
    }
    const ext = (file.name.split('.').pop() || 'jpg').replace(/[^a-zA-Z0-9]/g, '').slice(0, 5) || 'bin';
    const dir = path.join(process.cwd(), 'public', 'uploads');
    await mkdir(dir, { recursive: true });
    const fname = `stop-${stopId}-${Date.now()}.${ext}`;
    const buf = Buffer.from(await file.arrayBuffer());
    await writeFile(path.join(dir, fname), buf);
    const urlPath = `/uploads/${fname}`;
    await exec('UPDATE route_stops SET media_path=? WHERE id=?', [urlPath, stopId]);
    return NextResponse.json({ ok: true, mediaPath: urlPath });
  } catch (e) {
    return NextResponse.json({ ok: false, error: String(e) }, { status: 500 });
  }
}
