import { NextResponse } from 'next/server';
import { q, exec } from '@/lib/db';
import { writeFile, mkdir, unlink } from 'fs/promises';
import path from 'path';

export const runtime = 'nodejs';

// Delivery clips are short; anything bigger than this is a mistake (or abuse —
// the endpoint is reachable from the public URL).
const MAX_BYTES = 50 * 1024 * 1024;
const ALLOWED_EXT = new Set(['jpg', 'jpeg', 'png', 'webp', 'heic', 'mp4', '3gp', 'mov', 'webm']);

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
    if (file.size > MAX_BYTES) {
      return NextResponse.json({ ok: false, error: 'file too large (max 50MB)' }, { status: 413 });
    }
    const ext = (file.name.split('.').pop() ?? '').toLowerCase().replace(/[^a-z0-9]/g, '');
    if (!ALLOWED_EXT.has(ext)) {
      return NextResponse.json({ ok: false, error: `file type not allowed: .${ext}` }, { status: 400 });
    }

    const stopRows = await q<{ media_path: string | null }>(
      'SELECT media_path FROM route_stops WHERE id=? LIMIT 1',
      [stopId],
    );
    if (!stopRows[0]) {
      return NextResponse.json({ ok: false, error: 'stop not found' }, { status: 404 });
    }

    const dir = path.join(process.cwd(), 'public', 'uploads');
    await mkdir(dir, { recursive: true });
    const fname = `stop-${stopId}-${Date.now()}.${ext}`;
    const buf = Buffer.from(await file.arrayBuffer());
    await writeFile(path.join(dir, fname), buf);
    const urlPath = `/uploads/${fname}`;
    await exec('UPDATE route_stops SET media_path=? WHERE id=?', [urlPath, stopId]);

    // Re-shooting replaces the old clip — remove it so the disk doesn't accumulate
    // orphans. Only ever touch files we wrote ourselves (basename under uploads/).
    const old = stopRows[0].media_path;
    if (old && old.startsWith('/uploads/') && old !== urlPath) {
      await unlink(path.join(dir, path.basename(old))).catch(() => {});
    }

    return NextResponse.json({ ok: true, mediaPath: urlPath });
  } catch (e) {
    return NextResponse.json({ ok: false, error: String(e) }, { status: 500 });
  }
}
