import { useState } from 'react';
import type { TrafficStore } from '@/store/useTrafficStore';
import type { VehicleType } from '@/types';
import { EmptyState, ErrorBanner, SectionHeader } from '@/components/ui/Common';
import { Modal } from '@/components/ui/Modal';
import { VehicleStatusBadge, VehicleTypeBadge, RouteStatusBadge } from '@/components/ui/Badges';
import {
  Ambulance,
  Plus,
  Search,
  MapPin,
  Navigation,
  ArrowRight,
} from 'lucide-react';

interface EmergencyVehiclesProps {
  store: TrafficStore;
}

export function EmergencyVehicles({ store }: EmergencyVehiclesProps) {
  const { vehicles, junctions, roads, registerVehicle, advanceVehicle } = store;
  const [showAdd, setShowAdd] = useState(false);
  const [error, setError] = useState('');
  const [search, setSearch] = useState('');
  const [typeFilter, setTypeFilter] = useState('ALL');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [advanceVehicleId, setAdvanceVehicleId] = useState<string | null>(null);

  const [vId, setVId] = useState('');
  const [vType, setVType] = useState<VehicleType>('AMBULANCE');
  const [vPosition, setVPosition] = useState('');

  const filtered = vehicles.filter((v) => {
    const matchSearch = !search ||
      v.id.toLowerCase().includes(search.toLowerCase()) ||
      v.lastConfirmedJunction.toLowerCase().includes(search.toLowerCase());
    const matchType = typeFilter === 'ALL' || v.type === typeFilter;
    const matchStatus = statusFilter === 'ALL' || v.status === statusFilter;
    return matchSearch && matchType && matchStatus;
  });

  const handleAdd = () => {
    setError('');
    const id = vId.trim().toUpperCase();
    if (!id) { setError('Vehicle ID is required.'); return; }
    if (!vPosition) { setError('Starting junction is required.'); return; }
    if (vehicles.some((v) => v.id === id)) { setError(`Vehicle ${id} already exists.`); return; }
    if (registerVehicle(id, vType, vPosition)) {
      setShowAdd(false);
      setVId(''); setVType('AMBULANCE'); setVPosition('');
    } else {
      setError('Failed to register vehicle. Ensure the junction exists.');
    }
  };

  const advanceTarget = advanceVehicleId ? vehicles.find((v) => v.id === advanceVehicleId) : null;
  const adjacentJunctions = advanceTarget
    ? roads.filter((r) => r.from === advanceTarget.lastConfirmedJunction && !r.closed).map((r) => r.to)
    : [];

  return (
    <div className="space-y-4 fade-in-up">
      <SectionHeader
        title="Emergency Vehicles"
        subtitle="Register and monitor the emergency vehicle fleet"
        icon={<Ambulance className="w-5 h-5" />}
        action={
          <button
            onClick={() => { setError(''); setShowAdd(true); }}
            className="btn-primary"
            disabled={junctions.length === 0}
          >
            <Plus className="w-4 h-4" /> Register Vehicle
          </button>
        }
      />

      {vehicles.length > 0 && (
        <div className="flex flex-col sm:flex-row gap-3">
          <div className="relative flex-1">
            <Search className="w-4 h-4 text-slate-500 absolute left-3 top-1/2 -translate-y-1/2" />
            <input className="input-field pl-9" placeholder="Search by ID or location..." value={search} onChange={(e) => setSearch(e.target.value)} />
          </div>
          <select className="select-field sm:w-40" value={typeFilter} onChange={(e) => setTypeFilter(e.target.value)}>
            <option value="ALL">All Types</option>
            <option value="AMBULANCE">Ambulance</option>
            <option value="FIRE_ENGINE">Fire Engine</option>
            <option value="RESCUE">Rescue</option>
          </select>
          <select className="select-field sm:w-40" value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
            <option value="ALL">All Statuses</option>
            <option value="IDLE">Idle</option>
            <option value="EN_ROUTE">En Route</option>
            <option value="ARRIVED">Arrived</option>
            <option value="UNREACHABLE">Unreachable</option>
          </select>
        </div>
      )}

      {vehicles.length === 0 ? (
        <div className="card">
          <EmptyState
            icon={<Ambulance className="w-8 h-8" />}
            title="No vehicles registered"
            message="Register emergency vehicles to assign routes and dispatch them through the road network."
            actionLabel={junctions.length > 0 ? 'Register First Vehicle' : undefined}
            onAction={junctions.length > 0 ? () => setShowAdd(true) : undefined}
          />
        </div>
      ) : filtered.length === 0 ? (
        <div className="card p-8 text-center text-slate-500 text-sm">No vehicles match the current filters.</div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
          {filtered.map((v) => {
            const adjacent = roads.filter((r) => r.from === v.lastConfirmedJunction && !r.closed).map((r) => r.to);
            return (
              <div key={v.id} className="card card-hover p-4">
                <div className="flex items-start justify-between mb-3">
                  <div>
                    <div className="flex items-center gap-2 mb-1">
                      <span className="text-base font-bold text-slate-100 font-mono">{v.id}</span>
                      <VehicleTypeBadge type={v.type} />
                    </div>
                    <VehicleStatusBadge status={v.status} />
                  </div>
                </div>

                <div className="space-y-2 text-sm">
                  <div className="flex items-center gap-2 text-slate-400">
                    <MapPin className="w-4 h-4 text-slate-500" />
                    <span className="text-slate-500">Position:</span>
                    <span className="font-mono text-slate-200">{v.lastConfirmedJunction}</span>
                  </div>
                  {v.destination && (
                    <div className="flex items-center gap-2 text-slate-400">
                      <Navigation className="w-4 h-4 text-slate-500" />
                      <span className="text-slate-500">Destination:</span>
                      <span className="font-mono text-slate-200">{v.destination}</span>
                    </div>
                  )}
                  {v.currentRoute && v.currentRoute.status === 'VALID' && (
                    <div className="pt-2 border-t border-slate-800">
                      <div className="flex items-center gap-2 text-xs text-slate-500 mb-1">
                        <RouteStatusBadge status={v.currentRoute.status} />
                        <span>{v.currentRoute.totalTravelTime} min total</span>
                      </div>
                      <div className="flex flex-wrap items-center gap-1 text-xs font-mono text-slate-400">
                        {v.currentRoute.path.map((node, i) => (
                          <span key={i} className="flex items-center gap-1">
                            <span className={i === 0 ? 'text-cyan-400' : i === v.currentRoute!.path.length - 1 ? 'text-emerald-400' : ''}>{node}</span>
                            {i < v.currentRoute!.path.length - 1 && <ArrowRight className="w-3 h-3 text-slate-600" />}
                          </span>
                        ))}
                      </div>
                    </div>
                  )}
                  {v.currentRoute && v.currentRoute.status === 'UNREACHABLE' && (
                    <div className="pt-2 border-t border-slate-800">
                      <RouteStatusBadge status="UNREACHABLE" />
                    </div>
                  )}
                </div>

                {v.status === 'EN_ROUTE' && adjacent.length > 0 && (
                  <div className="mt-3 pt-3 border-t border-slate-800">
                    <button onClick={() => setAdvanceVehicleId(v.id)} className="btn-secondary w-full justify-center text-xs">
                      <ArrowRight className="w-3 h-3" /> Advance Vehicle
                    </button>
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}

      <Modal
        open={showAdd}
        onClose={() => setShowAdd(false)}
        title="Register Emergency Vehicle"
        subtitle="Add a new vehicle to the emergency fleet"
        footer={
          <>
            <button onClick={() => setShowAdd(false)} className="btn-secondary">Cancel</button>
            <button onClick={handleAdd} className="btn-primary">Register</button>
          </>
        }
      >
        <div className="space-y-4">
          {error && <ErrorBanner message={error} />}
          <div>
            <label className="text-sm text-slate-400 mb-1 block">Vehicle ID</label>
            <input className="input-field" placeholder="e.g. AMB1" value={vId} onChange={(e) => setVId(e.target.value)} />
          </div>
          <div>
            <label className="text-sm text-slate-400 mb-1 block">Vehicle Type</label>
            <select className="select-field" value={vType} onChange={(e) => setVType(e.target.value as VehicleType)}>
              <option value="AMBULANCE">Ambulance</option>
              <option value="FIRE_ENGINE">Fire Engine</option>
              <option value="RESCUE">Rescue</option>
            </select>
          </div>
          <div>
            <label className="text-sm text-slate-400 mb-1 block">Starting Junction</label>
            <select className="select-field" value={vPosition} onChange={(e) => setVPosition(e.target.value)}>
              <option value="">Select junction...</option>
              {junctions.map((j) => <option key={j.id} value={j.id}>{j.id}</option>)}
            </select>
          </div>
        </div>
      </Modal>

      <Modal
        open={!!advanceVehicleId}
        onClose={() => setAdvanceVehicleId(null)}
        title="Advance Vehicle"
        subtitle={advanceTarget ? `${advanceTarget.id} is at ${advanceTarget.lastConfirmedJunction}` : ''}
        footer={<button onClick={() => setAdvanceVehicleId(null)} className="btn-secondary">Cancel</button>}
      >
        <div className="space-y-3">
          <p className="text-sm text-slate-400">Select an adjacent junction to move the vehicle to. The road must be open.</p>
          {adjacentJunctions.length === 0 ? (
            <p className="text-sm text-amber-400">No adjacent open roads from current position.</p>
          ) : (
            <div className="space-y-2">
              {adjacentJunctions.map((target) => {
                const road = roads.find((r) => r.from === advanceTarget!.lastConfirmedJunction && r.to === target);
                const isDestination = advanceTarget?.destination === target;
                return (
                  <button
                    key={target}
                    onClick={() => {
                      if (advanceVehicleId) {
                        advanceVehicle(advanceVehicleId, target);
                        setAdvanceVehicleId(null);
                      }
                    }}
                    className="w-full flex items-center justify-between px-4 py-3 rounded-lg bg-slate-800/40 hover:bg-slate-700/40 border border-slate-700/50 hover:border-cyan-500/30 transition-all"
                  >
                    <div className="flex items-center gap-3">
                      <ArrowRight className="w-4 h-4 text-cyan-400" />
                      <span className="font-mono text-sm text-slate-200">
                        {advanceTarget?.lastConfirmedJunction} \u2192 {target}
                      </span>
                    </div>
                    <div className="flex items-center gap-2">
                      {isDestination && (
                        <span className="badge bg-emerald-500/15 text-emerald-400 border border-emerald-500/30">Destination</span>
                      )}
                      <span className="text-xs text-slate-500">{road?.currentTravelTime}m</span>
                    </div>
                  </button>
                );
              })}
            </div>
          )}
        </div>
      </Modal>
    </div>
  );
}
