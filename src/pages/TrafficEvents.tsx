import { useState } from 'react';
import type { TrafficStore } from '@/store/useTrafficStore';
import type { EventType } from '@/types';
import { EmptyState, ErrorBanner, SectionHeader } from '@/components/ui/Common';
import { EventTypeBadge, EventStatusBadge } from '@/components/ui/Badges';
import {
  CalendarClock,
  Plus,
  Play,
  FastForward,
  Ban,
  RefreshCw,
  AlertTriangle,
  Clock,
  X,
} from 'lucide-react';

interface TrafficEventsProps {
  store: TrafficStore;
}

export function TrafficEvents({ store }: TrafficEventsProps) {
  const { events, roads, queueTrafficEvent, processNextEvent, processAllEvents } = store;
  const [showAdd, setShowAdd] = useState(false);
  const [error, setError] = useState('');

  const [eType, setEType] = useState<EventType>('CLOSURE');
  const [eFrom, setEFrom] = useState('');
  const [eTo, setETo] = useState('');
  const [eFactor, setEFactor] = useState('');

  const pendingEvents = events.filter((e) => e.status === 'PENDING').sort((a, b) => a.sequence - b.sequence);
  const processedEvents = events.filter((e) => e.status === 'PROCESSED');

  const handleAdd = () => {
    setError('');
    if (!eFrom || !eTo) { setError('Select both source and destination junctions.'); return; }
    if (eFrom === eTo) { setError('Source and destination must be different.'); return; }
    if (!roads.some((r) => r.from === eFrom && r.to === eTo)) {
      setError(`Road ${eFrom}\u2192${eTo} does not exist in the network.`);
      return;
    }
    if (eType === 'CONGESTION') {
      const f = parseFloat(eFactor);
      if (isNaN(f) || f < 1.0) { setError('Congestion factor must be a number \u2265 1.0.'); return; }
      queueTrafficEvent('CONGESTION', eFrom, eTo, false, f);
    } else if (eType === 'CLOSURE') {
      queueTrafficEvent('CLOSURE', eFrom, eTo, false, null);
    } else {
      queueTrafficEvent('REOPEN', eFrom, eTo, true, null);
    }
    setShowAdd(false);
    setEFrom(''); setETo(''); setEFactor(''); setEType('CLOSURE');
  };

  const eventIcons: Record<EventType, React.ReactNode> = {
    CLOSURE: <Ban className="w-4 h-4" />,
    REOPEN: <RefreshCw className="w-4 h-4" />,
    CONGESTION: <AlertTriangle className="w-4 h-4" />,
  };

  return (
    <div className="space-y-4 fade-in-up">
      <SectionHeader
        title="Traffic Events"
        subtitle="Queue and process road closures, reopens and congestion events"
        icon={<CalendarClock className="w-5 h-5" />}
        action={
          <div className="flex gap-2 flex-wrap">
            <button onClick={() => processNextEvent()} className="btn-secondary" disabled={pendingEvents.length === 0}>
              <Play className="w-4 h-4" /> Process Next
            </button>
            <button onClick={() => processAllEvents()} className="btn-secondary" disabled={pendingEvents.length === 0}>
              <FastForward className="w-4 h-4" /> Process All
            </button>
            <button onClick={() => { setError(''); setShowAdd(true); }} className="btn-primary" disabled={roads.length === 0}>
              <Plus className="w-4 h-4" /> Queue Event
            </button>
          </div>
        }
      />

      <div className="card p-5">
        <h3 className="text-sm font-semibold text-slate-200 mb-4 flex items-center gap-2">
          <Clock className="w-4 h-4 text-amber-400" /> Pending Events Queue (FIFO)
          {pendingEvents.length > 0 && (
            <span className="badge bg-amber-500/10 text-amber-400 border border-amber-500/20">{pendingEvents.length}</span>
          )}
        </h3>
        {pendingEvents.length === 0 ? (
          <p className="text-sm text-slate-500 py-4 text-center">No pending traffic events.</p>
        ) : (
          <div className="space-y-2">
            {pendingEvents.map((evt, idx) => (
              <div key={evt.id} className="flex items-center justify-between px-4 py-3 rounded-lg bg-slate-800/30 border border-slate-700/30">
                <div className="flex items-center gap-3 min-w-0">
                  <span className={`text-xs font-bold w-6 h-6 rounded flex items-center justify-center ${
                    idx === 0 ? 'bg-cyan-500/20 text-cyan-400' : 'bg-slate-700/30 text-slate-500'
                  }`}>{idx + 1}</span>
                  <span className="text-xs font-mono text-slate-500">#{evt.sequence}</span>
                  <EventTypeBadge type={evt.type} />
                  <span className="text-sm text-slate-300 truncate">{evt.description}</span>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      <div className="card p-5">
        <h3 className="text-sm font-semibold text-slate-200 mb-4 flex items-center gap-2">
          <RefreshCw className="w-4 h-4 text-emerald-400" /> Processed Events
          {processedEvents.length > 0 && (
            <span className="badge bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">{processedEvents.length}</span>
          )}
        </h3>
        {processedEvents.length === 0 ? (
          <p className="text-sm text-slate-500 py-4 text-center">No processed traffic events yet.</p>
        ) : (
          <div className="space-y-2">
            {processedEvents.slice().reverse().map((evt) => (
              <div key={evt.id} className="flex items-center justify-between px-4 py-3 rounded-lg bg-slate-800/20 border border-slate-700/20">
                <div className="flex items-center gap-3 min-w-0">
                  <span className="text-xs font-mono text-slate-500">#{evt.sequence}</span>
                  <EventTypeBadge type={evt.type} />
                  <span className="text-sm text-slate-400 truncate">{evt.description}</span>
                </div>
                <EventStatusBadge status={evt.status} />
              </div>
            ))}
          </div>
        )}
      </div>

      {events.length === 0 && (
        <div className="card">
          <EmptyState
            icon={<CalendarClock className="w-8 h-8" />}
            title="No traffic events yet"
            message="Queue road closures, reopens and congestion events to modify the network state. Events are processed in FIFO order and trigger automatic route recalculation for en-route vehicles."
            actionLabel={roads.length > 0 ? 'Queue First Event' : undefined}
            onAction={roads.length > 0 ? () => setShowAdd(true) : undefined}
          />
        </div>
      )}

      {showAdd && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div className="absolute inset-0 bg-black/60 backdrop-blur-sm" onClick={() => setShowAdd(false)} />
          <div className="relative card w-full max-w-md fade-in-up">
            <div className="flex items-center justify-between p-5 border-b border-slate-800">
              <h3 className="text-base font-semibold text-slate-100">Queue Traffic Event</h3>
              <button onClick={() => setShowAdd(false)} className="text-slate-500 hover:text-slate-300">
                <X className="w-5 h-5" />
              </button>
            </div>
            <div className="p-5 space-y-4">
              {error && <ErrorBanner message={error} />}
              <div>
                <label className="text-sm text-slate-400 mb-1 block">Event Type</label>
                <div className="grid grid-cols-3 gap-2">
                  {(['CLOSURE', 'REOPEN', 'CONGESTION'] as EventType[]).map((t) => (
                    <button
                      key={t}
                      onClick={() => setEType(t)}
                      className={`flex flex-col items-center gap-1 px-3 py-3 rounded-lg text-xs font-medium border transition-all ${
                        eType === t
                          ? t === 'CLOSURE' ? 'bg-red-500/15 text-red-400 border-red-500/40'
                          : t === 'REOPEN' ? 'bg-emerald-500/15 text-emerald-400 border-emerald-500/40'
                          : 'bg-amber-500/15 text-amber-400 border-amber-500/40'
                          : 'bg-slate-800/30 text-slate-400 border-slate-700/30 hover:border-slate-600'
                      }`}
                    >
                      {eventIcons[t]}
                      {t.charAt(0) + t.slice(1).toLowerCase()}
                    </button>
                  ))}
                </div>
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="text-sm text-slate-400 mb-1 block">From Junction</label>
                  <select className="select-field" value={eFrom} onChange={(e) => setEFrom(e.target.value)}>
                    <option value="">Select...</option>
                    {[...new Set(roads.map((r) => r.from))].sort().map((id) => <option key={id} value={id}>{id}</option>)}
                  </select>
                </div>
                <div>
                  <label className="text-sm text-slate-400 mb-1 block">To Junction</label>
                  <select className="select-field" value={eTo} onChange={(e) => setETo(e.target.value)}>
                    <option value="">Select...</option>
                    {eFrom
                      ? roads.filter((r) => r.from === eFrom).map((r) => <option key={r.to} value={r.to}>{r.to}</option>)
                      : [...new Set(roads.map((r) => r.to))].sort().map((id) => <option key={id} value={id}>{id}</option>)}
                  </select>
                </div>
              </div>
              {eType === 'CONGESTION' && (
                <div>
                  <label className="text-sm text-slate-400 mb-1 block">Congestion Factor (\u2265 1.0)</label>
                  <input className="input-field" type="number" min="1.0" step="0.1" placeholder="e.g. 1.5 for 50% slower" value={eFactor} onChange={(e) => setEFactor(e.target.value)} />
                  <p className="text-xs text-slate-500 mt-1">Current travel time = base time \u00d7 factor. Must be at least 1.0.</p>
                </div>
              )}
              <div className="flex justify-end gap-2 pt-2">
                <button onClick={() => setShowAdd(false)} className="btn-secondary">Cancel</button>
                <button onClick={handleAdd} className="btn-primary">Queue Event</button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
