import { useState, useMemo } from 'react';
import type { TrafficStore } from '@/store/useTrafficStore';
import type { Junction } from '@/types';
import { EmptyState, ErrorBanner, SectionHeader } from '@/components/ui/Common';
import { Modal } from '@/components/ui/Modal';
import {
  Network,
  Plus,
  CircleDot,
  ArrowRight,
  Ban,
  Gauge,
  MapPin,
  Move,
} from 'lucide-react';

interface RoadNetworkProps {
  store: TrafficStore;
}

export function RoadNetwork({ store }: RoadNetworkProps) {
  const { junctions, roads, vehicles, addJunction, addRoad } = store;
  const [showAddJunction, setShowAddJunction] = useState(false);
  const [showAddRoad, setShowAddRoad] = useState(false);
  const [error, setError] = useState('');
  const [selectedJunction, setSelectedJunction] = useState<string | null>(null);

  const [jId, setJId] = useState('');
  const [jX, setJX] = useState('');
  const [jY, setJY] = useState('');

  const [rFrom, setRFrom] = useState('');
  const [rTo, setRTo] = useState('');
  const [rTime, setRTime] = useState('');
  const [rOneWay, setROneWay] = useState(false);

  const viewBox = useMemo(() => {
    if (junctions.length === 0) return '0 0 800 500';
    const xs = junctions.map((j) => j.x);
    const ys = junctions.map((j) => j.y);
    const minX = Math.min(...xs) - 60;
    const maxX = Math.max(...xs) + 60;
    const minY = Math.min(...ys) - 60;
    const maxY = Math.max(...ys) + 60;
    return `${minX} ${minY} ${maxX - minX} ${maxY - minY}`;
  }, [junctions]);

  const junctionMap = useMemo(() => {
    const m = new Map<string, Junction>();
    junctions.forEach((j) => m.set(j.id, j));
    return m;
  }, [junctions]);

  const handleAddJunction = () => {
    setError('');
    const id = jId.trim().toUpperCase();
    const x = parseFloat(jX);
    const y = parseFloat(jY);
    if (!id) { setError('Junction ID is required.'); return; }
    if (isNaN(x) || isNaN(y)) { setError('Valid X and Y coordinates are required.'); return; }
    if (junctions.some((j) => j.id === id)) { setError(`Junction ${id} already exists.`); return; }
    if (addJunction(id, x, y)) {
      setShowAddJunction(false);
      setJId(''); setJX(''); setJY('');
    } else {
      setError('Failed to add junction.');
    }
  };

  const handleAddRoad = () => {
    setError('');
    if (!rFrom || !rTo) { setError('Both source and destination junctions are required.'); return; }
    if (rFrom === rTo) { setError('Source and destination must be different.'); return; }
    const time = parseFloat(rTime);
    if (isNaN(time) || time <= 0) { setError('Travel time must be a positive number.'); return; }
    if (roads.some((r) => r.from === rFrom && r.to === rTo)) { setError(`Road ${rFrom}\u2192${rTo} already exists.`); return; }
    if (addRoad(rFrom, rTo, time, rOneWay)) {
      setShowAddRoad(false);
      setRFrom(''); setRTo(''); setRTime(''); setROneWay(false);
    } else {
      setError('Failed to add road. Ensure both junctions exist.');
    }
  };

  return (
    <div className="space-y-4 fade-in-up">
      <SectionHeader
        title="Road Network"
        subtitle="Interactive visualisation of junctions, roads and routes"
        icon={<Network className="w-5 h-5" />}
        action={
          <div className="flex gap-2">
            <button onClick={() => { setError(''); setShowAddJunction(true); }} className="btn-secondary">
              <CircleDot className="w-4 h-4" /> Add Junction
            </button>
            <button
              onClick={() => { setError(''); setShowAddRoad(true); }}
              className="btn-primary"
              disabled={junctions.length < 2}
            >
              <Plus className="w-4 h-4" /> Add Road
            </button>
          </div>
        }
      />

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
        <div className="lg:col-span-2 card p-4">
          <h3 className="text-sm font-semibold text-slate-200 mb-3">Network Map</h3>
          {junctions.length === 0 ? (
            <EmptyState
              icon={<MapPin className="w-8 h-8" />}
              title="No junctions yet"
              message="Add junctions to start building the road network. Each junction needs an ID and a position on the map."
              actionLabel="Add First Junction"
              onAction={() => setShowAddJunction(true)}
            />
          ) : (
            <div className="relative w-full bg-[#0a0f1c] rounded-lg border border-slate-800 overflow-auto" style={{ minHeight: '400px' }}>
              <svg viewBox={viewBox} className="w-full" style={{ minHeight: '400px' }}>
                {roads.map((road) => {
                  const from = junctionMap.get(road.from);
                  const to = junctionMap.get(road.to);
                  if (!from || !to) return null;
                  const stroke = road.closed
                    ? '#ef4444'
                    : road.currentTravelTime > road.baseTravelTime
                    ? '#f59e0b'
                    : '#3b82f6';
                  const strokeWidth = 2;
                  const midX = (from.x + to.x) / 2;
                  const midY = (from.y + to.y) / 2;
                  const angle = Math.atan2(to.y - from.y, to.x - from.x) * (180 / Math.PI);

                  return (
                    <g key={`${road.from}->${road.to}`}>
                      <line
                        x1={from.x}
                        y1={from.y}
                        x2={to.x}
                        y2={to.y}
                        stroke={stroke}
                        strokeWidth={strokeWidth}
                        strokeDasharray={road.closed ? '6 4' : undefined}
                        opacity={road.closed ? 0.5 : 1}
                      />
                      {road.closed ? (
                        <g transform={`translate(${midX}, ${midY})`}>
                          <circle r="10" fill="#0a0f1c" stroke="#ef4444" strokeWidth="1.5" />
                          <line x1="-5" y1="-5" x2="5" y2="5" stroke="#ef4444" strokeWidth="2" />
                          <line x1="5" y1="-5" x2="-5" y2="5" stroke="#ef4444" strokeWidth="2" />
                        </g>
                      ) : (
                        <g transform={`translate(${midX}, ${midY}) rotate(${angle})`}>
                          <polygon points="-4,-4 4,0 -4,4" fill={stroke} opacity="0.7" />
                        </g>
                      )}
                      <g transform={`translate(${midX + 8}, ${midY - 8})`}>
                        <rect x="-2" y="-9" width={(String(road.currentTravelTime).length + 2) * 6} height="14" rx="3" fill="#0d1426" opacity="0.8" />
                        <text x="2" y="1" fill={road.currentTravelTime > road.baseTravelTime ? '#f59e0b' : '#64748b'} fontSize="10" fontFamily="monospace">
                          {road.currentTravelTime}m
                        </text>
                      </g>
                    </g>
                  );
                })}

                {junctions.map((j) => {
                  const isSelected = selectedJunction === j.id;
                  const hasVehicles = vehicles.some((v) => v.lastConfirmedJunction === j.id);
                  return (
                    <g
                      key={j.id}
                      transform={`translate(${j.x}, ${j.y})`}
                      className="cursor-pointer"
                      onClick={() => setSelectedJunction(isSelected ? null : j.id)}
                    >
                      <circle
                        r={isSelected ? 16 : 12}
                        fill={isSelected ? '#22d3ee' : '#0d1426'}
                        stroke={isSelected ? '#22d3ee' : '#3b82f6'}
                        strokeWidth="2"
                        className="transition-all"
                      />
                      {hasVehicles && (
                        <circle r="18" fill="none" stroke="#22d3ee" strokeWidth="1" opacity="0.3" className="pulse-cyan" />
                      )}
                      <text
                        textAnchor="middle"
                        dy="4"
                        fill={isSelected ? '#0a0f1c' : '#94a3b8'}
                        fontSize="10"
                        fontWeight="bold"
                        fontFamily="monospace"
                      >
                        {j.id}
                      </text>
                    </g>
                  );
                })}
              </svg>
            </div>
          )}
        </div>

        <div className="space-y-4">
          <div className="card p-4">
            <h3 className="text-sm font-semibold text-slate-200 mb-3 flex items-center gap-2">
              <CircleDot className="w-4 h-4 text-cyan-400" /> Junctions ({junctions.length})
            </h3>
            {junctions.length === 0 ? (
              <p className="text-xs text-slate-500 py-3 text-center">No junctions added.</p>
            ) : (
              <div className="space-y-1 max-h-48 overflow-y-auto">
                {junctions.map((j) => (
                  <div
                    key={j.id}
                    className={`flex items-center justify-between px-3 py-2 rounded-lg text-sm cursor-pointer transition-colors ${
                      selectedJunction === j.id
                        ? 'bg-cyan-500/10 text-cyan-400 border border-cyan-500/20'
                        : 'text-slate-300 hover:bg-slate-800/40'
                    }`}
                    onClick={() => setSelectedJunction(selectedJunction === j.id ? null : j.id)}
                  >
                    <span className="font-mono">{j.id}</span>
                    <span className="text-xs text-slate-500">({j.x}, {j.y})</span>
                  </div>
                ))}
              </div>
            )}
          </div>

          <div className="card p-4">
            <h3 className="text-sm font-semibold text-slate-200 mb-3 flex items-center gap-2">
              <ArrowRight className="w-4 h-4 text-sky-400" /> Roads ({roads.length})
            </h3>
            {roads.length === 0 ? (
              <p className="text-xs text-slate-500 py-3 text-center">No roads added.</p>
            ) : (
              <div className="space-y-1 max-h-48 overflow-y-auto">
                {roads.map((r) => (
                  <div key={`${r.from}->${r.to}`} className="flex items-center justify-between px-3 py-2 rounded-lg text-sm bg-slate-800/20">
                    <div className="flex items-center gap-2 min-w-0">
                      <span className="font-mono text-slate-300 text-xs">
                        {r.from}\u2192{r.to}
                      </span>
                      {r.closed ? (
                        <Ban className="w-3 h-3 text-red-400 flex-shrink-0" />
                      ) : r.currentTravelTime > r.baseTravelTime ? (
                        <Gauge className="w-3 h-3 text-amber-400 flex-shrink-0" />
                      ) : null}
                    </div>
                    <span className={`text-xs flex-shrink-0 ${r.closed ? 'text-red-400' : r.currentTravelTime > r.baseTravelTime ? 'text-amber-400' : 'text-slate-500'}`}>
                      {r.closed ? 'Closed' : `${r.currentTravelTime}m`}
                    </span>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>

      <Modal
        open={showAddJunction}
        onClose={() => setShowAddJunction(false)}
        title="Add Junction"
        subtitle="Create a new junction in the road network"
        footer={
          <>
            <button onClick={() => setShowAddJunction(false)} className="btn-secondary">Cancel</button>
            <button onClick={handleAddJunction} className="btn-primary">Add Junction</button>
          </>
        }
      >
        <div className="space-y-4">
          {error && <ErrorBanner message={error} />}
          <div>
            <label className="text-sm text-slate-400 mb-1 block">Junction ID</label>
            <input className="input-field" placeholder="e.g. J1" value={jId} onChange={(e) => setJId(e.target.value)} />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="text-sm text-slate-400 mb-1 block flex items-center gap-1">
                <Move className="w-3 h-3" /> X Coordinate
              </label>
              <input className="input-field" type="number" placeholder="e.g. 100" value={jX} onChange={(e) => setJX(e.target.value)} />
            </div>
            <div>
              <label className="text-sm text-slate-400 mb-1 block flex items-center gap-1">
                <Move className="w-3 h-3" /> Y Coordinate
              </label>
              <input className="input-field" type="number" placeholder="e.g. 200" value={jY} onChange={(e) => setJY(e.target.value)} />
            </div>
          </div>
          <p className="text-xs text-slate-500">Coordinates determine the junction's position on the visual map.</p>
        </div>
      </Modal>

      <Modal
        open={showAddRoad}
        onClose={() => setShowAddRoad(false)}
        title="Add Road"
        subtitle="Connect two junctions with a directed or two-way road"
        footer={
          <>
            <button onClick={() => setShowAddRoad(false)} className="btn-secondary">Cancel</button>
            <button onClick={handleAddRoad} className="btn-primary">Add Road</button>
          </>
        }
      >
        <div className="space-y-4">
          {error && <ErrorBanner message={error} />}
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="text-sm text-slate-400 mb-1 block">From Junction</label>
              <select className="select-field" value={rFrom} onChange={(e) => setRFrom(e.target.value)}>
                <option value="">Select source...</option>
                {junctions.map((j) => <option key={j.id} value={j.id}>{j.id}</option>)}
              </select>
            </div>
            <div>
              <label className="text-sm text-slate-400 mb-1 block">To Junction</label>
              <select className="select-field" value={rTo} onChange={(e) => setRTo(e.target.value)}>
                <option value="">Select destination...</option>
                {junctions.map((j) => <option key={j.id} value={j.id}>{j.id}</option>)}
              </select>
            </div>
          </div>
          <div>
            <label className="text-sm text-slate-400 mb-1 block">Travel Time (minutes)</label>
            <input className="input-field" type="number" min="0.1" step="0.1" placeholder="e.g. 5" value={rTime} onChange={(e) => setRTime(e.target.value)} />
          </div>
          <label className="flex items-center gap-2 cursor-pointer">
            <input type="checkbox" checked={rOneWay} onChange={(e) => setROneWay(e.target.checked)} className="w-4 h-4 accent-cyan-500" />
            <span className="text-sm text-slate-300">One-way road (only {rFrom || 'source'} \u2192 {rTo || 'destination'})</span>
          </label>
        </div>
      </Modal>
    </div>
  );
}
