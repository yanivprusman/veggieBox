'use client';

import { useEffect, useState, use } from 'react';

type Customer = {
  name: string;
  address: string | null;
  houseInstructions: string | null;
  dropPreference: 'central' | 'beside' | null;
  lat: number | null;
  lon: number | null;
  businessName: string;
  area: string | null;
  centralDrop: string | null;
};

export default function DetailsForm({ params }: { params: Promise<{ token: string }> }) {
  const { token } = use(params);
  const [c, setC] = useState<Customer | null>(null);
  const [err, setErr] = useState<string | null>(null);
  const [saved, setSaved] = useState(false);
  const [saving, setSaving] = useState(false);

  const [address, setAddress] = useState('');
  const [instructions, setInstructions] = useState('');
  const [drop, setDrop] = useState<'central' | 'beside' | ''>('');
  const [coords, setCoords] = useState<{ lat: number; lon: number } | null>(null);
  const [locating, setLocating] = useState(false);

  useEffect(() => {
    fetch(`/api/details/${token}`)
      .then((r) => r.json())
      .then((d) => {
        if (!d.ok) return setErr('הקישור לא תקין');
        const cu: Customer = d.customer;
        setC(cu);
        setAddress(cu.address ?? '');
        setInstructions(cu.houseInstructions ?? '');
        setDrop(cu.dropPreference ?? '');
        if (cu.lat != null && cu.lon != null) setCoords({ lat: cu.lat, lon: cu.lon });
      })
      .catch(() => setErr('שגיאה בטעינה'));
  }, [token]);

  function useMyLocation() {
    if (!navigator.geolocation) return;
    setLocating(true);
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        setCoords({ lat: pos.coords.latitude, lon: pos.coords.longitude });
        setLocating(false);
      },
      () => setLocating(false),
      { enableHighAccuracy: true, timeout: 10000 },
    );
  }

  async function save() {
    setSaving(true);
    const res = await fetch(`/api/details/${token}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        address,
        houseInstructions: instructions,
        dropPreference: drop || null,
        lat: coords?.lat ?? null,
        lon: coords?.lon ?? null,
      }),
    });
    const d = await res.json();
    setSaving(false);
    if (d.ok) setSaved(true);
    else setErr('שמירה נכשלה');
  }

  if (err) return <Shell><p className="text-red-600">{err}</p></Shell>;
  if (!c) return <Shell><p>טוען…</p></Shell>;
  if (saved)
    return (
      <Shell>
        <div className="text-center py-8">
          <div className="text-5xl mb-3">✅</div>
          <h2 className="text-xl font-bold mb-2">תודה רבה, {c.name}!</h2>
          <p className="text-gray-600">הפרטים נשמרו. נתראה ביום שלישי 🥬📦</p>
        </div>
      </Shell>
    );

  return (
    <Shell>
      <h1 className="text-2xl font-bold mb-1">vegi box 🥬</h1>
      <p className="text-gray-500 mb-1">שלום {c.name},</p>
      <p className="text-gray-600 mb-5 text-sm">
        כדי שאדע בדיוק לאן להביא את ארגז הירקות{c.area ? ` ב${c.area}` : ''}, בבקשה למלא:
      </p>

      <label className="block mb-4">
        <span className="text-sm font-semibold">כתובת מלאה</span>
        <input
          data-id="address-input"
          className="mt-1 w-full border rounded-lg p-3"
          placeholder="רחוב ומספר בית / שכונה"
          value={address}
          onChange={(e) => setAddress(e.target.value)}
        />
      </label>

      <label className="block mb-4">
        <span className="text-sm font-semibold">הוראות הגעה לבית</span>
        <textarea
          data-id="instructions-input"
          className="mt-1 w-full border rounded-lg p-3"
          rows={3}
          placeholder="לדוגמה: בית עם פרגולה, שער חום, יש כלב בחצר…"
          value={instructions}
          onChange={(e) => setInstructions(e.target.value)}
        />
      </label>

      <div className="mb-4">
        <span className="text-sm font-semibold">אם אתם לא בבית, איפה להשאיר את הארגז?</span>
        <div className="mt-2 grid gap-2">
          <DropOption
            id="drop-beside"
            checked={drop === 'beside'}
            onClick={() => setDrop('beside')}
            title="ליד הבית / על יד הדלת"
            sub="להשאיר צמוד לבית"
          />
          <DropOption
            id="drop-central"
            checked={drop === 'central'}
            onClick={() => setDrop('central')}
            title="בנקודה המרכזית"
            sub={c.centralDrop ?? 'נקודת איסוף קבועה'}
          />
        </div>
      </div>

      <button
        data-id="use-location"
        onClick={useMyLocation}
        className="mb-5 w-full border rounded-lg p-3 text-sm font-semibold active:bg-gray-100"
      >
        {locating ? 'מאתר מיקום…' : coords ? '📍 המיקום נשמר (לחצו לעדכן)' : '📍 שתפו את המיקום שלי (מהבית)'}
      </button>

      <button
        data-id="save-details"
        onClick={save}
        disabled={saving}
        className="w-full bg-green-700 text-white rounded-lg p-3 font-bold disabled:opacity-50"
      >
        {saving ? 'שומר…' : 'שליחה'}
      </button>
    </Shell>
  );
}

function DropOption(props: {
  id: string;
  checked: boolean;
  onClick: () => void;
  title: string;
  sub: string;
}) {
  return (
    <button
      data-id={props.id}
      onClick={props.onClick}
      className={`text-right border rounded-lg p-3 transition ${
        props.checked ? 'border-green-700 bg-green-50' : 'border-gray-300'
      }`}
    >
      <div className="font-semibold">{props.checked ? '◉ ' : '○ '}{props.title}</div>
      <div className="text-xs text-gray-500 mt-0.5">{props.sub}</div>
    </button>
  );
}

function Shell({ children }: { children: React.ReactNode }) {
  return (
    <div dir="rtl" className="min-h-screen bg-gray-50 flex justify-center p-4">
      <div className="w-full max-w-md bg-white rounded-2xl shadow p-6 mt-6 self-start">{children}</div>
    </div>
  );
}
