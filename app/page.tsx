'use client';

import { useEffect, useState, useCallback } from 'react';

type Stop = {
  stopId: number;
  customerId: number;
  name: string;
  phoneIntl: string | null;
  address: string | null;
  houseInstructions: string | null;
  dropPreference: string | null;
  status: string;
  cartons: number;
  seq: number;
  hasDetails: boolean;
};
type Progress = { total: number; delivered: number; notHome: number; remaining: number };
type GreetTarget = { customerId: number; name: string; message: string; waUrl: string | null; link: string };

export default function Home() {
  const [stops, setStops] = useState<Stop[]>([]);
  const [progress, setProgress] = useState<Progress | null>(null);
  const [biz, setBiz] = useState<{ name: string; area: string | null; currency: string } | null>(null);
  const [earn, setEarn] = useState<{ today: { deliveries: number; amount: number } } | null>(null);
  const [greet, setGreet] = useState<GreetTarget[]>([]);
  const [tab, setTab] = useState<'route' | 'greeting'>('route');

  const load = useCallback(async () => {
    const r = await fetch('/api/route/today').then((x) => x.json());
    if (r.ok) {
      setStops(r.stops);
      setProgress(r.progress);
      setBiz(r.business);
    }
    const e = await fetch('/api/earnings').then((x) => x.json());
    if (e.ok) setEarn(e);
    const g = await fetch('/api/greeting').then((x) => x.json());
    if (g.ok) setGreet(g.targets);
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  async function setStatus(stopId: number, status: string) {
    await fetch(`/api/stops/${stopId}`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ status, ...(status === 'delivered' ? { dropUsed: 'home' } : {}) }),
    });
    load();
  }

  return (
    <div dir="rtl" className="min-h-screen bg-gray-50 text-gray-900">
      <header className="bg-green-700 text-white p-4 shadow">
        <h1 className="text-xl font-bold">🥬 {biz?.name ?? 'VeggieBox'} {biz?.area ? `· ${biz.area}` : ''}</h1>
        <div className="flex gap-4 mt-2 text-sm">
          {progress && (
            <span data-id="progress-pill" className="bg-white/20 rounded-full px-3 py-1">
              נמסרו {progress.delivered}/{progress.total} · נותרו {progress.remaining}
            </span>
          )}
          {earn && (
            <span data-id="earnings-pill" className="bg-white/20 rounded-full px-3 py-1">
              היום: {earn.today.amount}{biz?.currency ?? '₪'} ({earn.today.deliveries} משלוחים)
            </span>
          )}
        </div>
      </header>

      <nav className="flex border-b bg-white">
        <TabBtn id="tab-route" active={tab === 'route'} onClick={() => setTab('route')} label={`מסלול היום (${stops.length})`} />
        <TabBtn id="tab-greeting" active={tab === 'greeting'} onClick={() => setTab('greeting')} label={`חסרי פרטים (${greet.length})`} />
      </nav>

      <main className="p-3 max-w-3xl mx-auto">
        {tab === 'route' && (
          <ul className="space-y-2">
            {stops.map((s, i) => (
              <li
                key={s.stopId}
                data-id={`stop-${s.stopId}`}
                className={`bg-white rounded-xl p-3 shadow-sm flex items-start justify-between ${
                  s.status === 'delivered' ? 'opacity-60' : ''
                }`}
              >
                <div className="flex-1">
                  <div className={`font-semibold ${s.status === 'delivered' ? 'line-through' : ''}`}>
                    {i + 1}. {s.name} <span className="text-xs text-gray-400">×{s.cartons}</span>
                  </div>
                  <div className="text-sm text-gray-600">{s.address ?? '— אין כתובת —'}</div>
                  {s.houseInstructions && (
                    <div className="text-xs text-amber-700 mt-0.5">📍 {s.houseInstructions}</div>
                  )}
                </div>
                <div className="flex gap-1 shrink-0">
                  {s.status !== 'delivered' ? (
                    <button
                      data-id={`deliver-${s.stopId}`}
                      onClick={() => setStatus(s.stopId, 'delivered')}
                      className="bg-green-600 text-white text-sm rounded-lg px-3 py-1.5 active:bg-green-700"
                    >
                      נמסר ✓
                    </button>
                  ) : (
                    <button
                      data-id={`undo-${s.stopId}`}
                      onClick={() => setStatus(s.stopId, 'pending')}
                      className="border text-sm rounded-lg px-3 py-1.5 text-gray-500"
                    >
                      בטל
                    </button>
                  )}
                </div>
              </li>
            ))}
          </ul>
        )}

        {tab === 'greeting' && (
          <div>
            <p className="text-sm text-gray-600 mb-3">
              {greet.length} לקוחות ללא כתובת. כל לחיצה פותחת וואטסאפ עם הודעת בקשת פרטים מוכנה (נשלח מהמספר שלך).
            </p>
            <ul className="space-y-2">
              {greet.map((g) => (
                <li key={g.customerId} data-id={`greet-${g.customerId}`} className="bg-white rounded-xl p-3 shadow-sm flex items-center justify-between">
                  <span className="font-semibold">{g.name}</span>
                  {g.waUrl ? (
                    <a
                      data-id={`greet-send-${g.customerId}`}
                      href={g.waUrl}
                      target="_blank"
                      className="bg-green-600 text-white text-sm rounded-lg px-3 py-1.5"
                    >
                      שלח בוואטסאפ
                    </a>
                  ) : (
                    <span className="text-xs text-gray-400">אין טלפון</span>
                  )}
                </li>
              ))}
            </ul>
          </div>
        )}
      </main>
    </div>
  );
}

function TabBtn(props: { id: string; active: boolean; onClick: () => void; label: string }) {
  return (
    <button
      data-id={props.id}
      data-active-tab={props.active ? props.label : undefined}
      onClick={props.onClick}
      className={`flex-1 py-3 text-sm font-semibold ${
        props.active ? 'text-green-700 border-b-2 border-green-700' : 'text-gray-500'
      }`}
    >
      {props.label}
    </button>
  );
}
