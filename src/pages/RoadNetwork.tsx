import { useCallback, useMemo, useState } from 'react';
import type { TrafficStore } from '@/store/useTrafficStore';
import type { RoadUpdate } from '@/types';
import { BackendError, ErrorBanner, LoadingState, SectionHeader } from '@/components/ui/Common';
import { Modal } from '@/components/ui/Modal';
import { NetworkMap } from '@/components/road-network/NetworkMap';
import { DetailsPanel } from '@/components/road-network/DetailsPanel';
import {
  buildVisualRoads,
  findFreeJunctionPosition,
  formatTravelTime,
  pairKey,
  roadsAtJunction,
} from '@/components/road-network/networkGeometry';
import type { Selection } from '@/components/road-network/networkGeometry';
import { Network, Plus, CircleDot, ArrowRight, Ban, Waypoints } from 'lucide-react';

interface RoadNetworkProps {
  store: TrafficStore;
}

export function RoadNetwork({ store }: RoadNetworkProps) {
  const {
    junctions,
    roads,
    vehicles,
    loading,
    loadError,
    reload,
    addJunction,
    addRoad,
    updateRoad,
    moveJunction,
    updateJunctionPosition,
  } = store;

  const [selection, setSelection] = useState<Selection>(null);
  const [showAddJunction, setShowAddJunction] = useState(false);
  const [showAddRoad, setShowAddRoad] = useState(false);
  const [junctionError, setJunctionError] = useState('');
  const [roadError, setRoadError] = useState('');
  const [panelError, setPanelError] = useState('');
  const [busy, setBusy] = useState(false);
  const [showGrid, setShowGrid] = useState(true);

  const [jId, setJId] = useState('');
  const [rFrom, setRFrom] = useState('');
  const [rTo, setRTo] = useState('');
  const [rTime, setRTime] = useState('');
  const [rType, setRType] = useState<'ONE_WAY' | 'TWO_WAY'>('ONE_WAY');
  const [rStatus, setRStatus] = useState<'OPEN' | 'CLOSED'>('OPEN');

  const visualRoads = useMemo(() => buildVisualRoads(junctions, roads), [junctions, roads]);

  const selectJunction = useCallback((id: string) => setSelection({ kind: 'junction', id }), []);
  const selectRoad = useCallback(
    (from: string, to: string) => setSelection({ kind: 'road', from, to }),
    [],
  );
  const clearSelection = useCallback(() => setSelection(null), []);

  const handleUpdateRoad = useCallback(
    async (from: string, to: string, changes: RoadUpdate) => {
      setPanelError('');
      const result = await updateRoad(from, to, changes);
      if (!result.ok) setPanelError(result.error ?? 'Failed to update the road.');
    },
    [updateRoad],
  );

  const openJunctionModal = () => {
    setJunctionError('');
    setShowAddJunction(true);
  };

  const resetRoadForm = () => {
    setRFrom('');
    setRTo('');
    setRTime('');
    setRType('ONE_WAY');
    setRStatus('OPEN');
    setRoadError('');
  };

  const openRoadModal = (source?: string) => {
    resetRoadForm();
    if (source) setRFrom(source);
    setShowAddRoad(true);
  };

  const handleAddJunction = async () => {
    setJunctionError('');
    const id = jId.trim().toUpperCase();
    if (!id) {
      setJunctionError('Junction ID is required.');
      return;
    }
    if (!/^[A-Z0-9_-]{1,8}$/.test(id)) {
      setJunctionError('Use 1–8 letters, numbers, dashes or underscores (e.g. J1).');
      return;
    }
    if (junctions.some((j) => j.id === id)) {
      setJunctionError(`Junction ${id} already exists.`);
      return;
    }

    setBusy(true);
    try {
      const position = findFreeJunctionPosition(junctions);
      const result = await addJunction(id, position.x, position.y);
      if (!result.ok) {
        setJunctionError(result.error ?? 'Could not add this junction. Please try again.');
        return;
      }
      setSelection({ kind: 'junction', id });
      setShowAddJunction(false);
      setJId('');
    } finally {
      setBusy(false);
    }
  };

  const handleAddRoad = async () => {
    setRoadError('');
    if (!rFrom || !rTo) {
      setRoadError('Choose both a source and a destination junction.');
      return;
    }
    if (rFrom === rTo) {
      setRoadError('Source and destination must be different.');
      return;
    }
    const time = parseFloat(rTime);
    if (isNaN(time) || time <= 0) {
      setRoadError('Travel time must be a positive number.');
      return;
    }
    if (roads.some((r) => (r.from === rFrom && r.to === rTo) || (r.from === rTo && r.to === rFrom))) {
      setRoadError(`A road between ${rFrom} and ${rTo} already exists.`);
      return;
    }

    setBusy(true);
    try {
      const result = await addRoad(rFrom, rTo, time, rType === 'ONE_WAY', rStatus === 'CLOSED');
      if (!result.ok) {
        setRoadError(result.error ?? 'Could not create the road. Make sure both junctions exist.');
        return;
      }
      setSelection({ kind: 'road', from: rFrom, to: rTo });
      setShowAddRoad(false);
      resetRoadForm();
    } finally {
      setBusy(false);
    }
  };

  const isRoadSelected = (from: string, to: string) =>
    selection?.kind === 'road' && pairKey(selection.from, selection.to) === pairKey(from, to);

  return (
    <div className="space-y-4 fade-in-up">
      <SectionHeader
        title="Road Network"
        subtitle="Interactive 2D map of junctions, roads, travel times and closures"
        icon={<Network className="w-5 h-5" />}
        action={
          <div className="flex gap-2">
            <button
              type="button"
              onClick={openJunctionModal}
              className="btn-secondary"
              disabled={loading || busy}
            >
              <CircleDot className="w-4 h-4" /> Add Junction
            </button>
            <button
              type="button"
              onClick={() => openRoadModal()}
              className="btn-primary"
              disabled={loading || busy || junctions.length < 2}
              title={junctions.length < 2 ? 'Add at least two junctions first' : 'Create a road'}
            >
              <Plus className="w-4 h-4" /> Add Road
            </button>
          </div>
        }
      />

      {loadError && <BackendError message={loadError} onRetry={() => void reload()} retrying={loading} />}

      {loading ? (
        <LoadingState label="Loading the road network from the backend&hellip;" />
      ) : (
        <div className="grid grid-cols-1 items-start gap-4 lg:grid-cols-3">
          <div className="lg:col-span-2">
            <NetworkMap
              junctions={junctions}
              vehicles={vehicles}
              visualRoads={visualRoads}
              selection={selection}
              showGrid={showGrid}
              onToggleGrid={() => setShowGrid((v) => !v)}
              onJunctionMoved={moveJunction}
              onJunctionDragEnd={(id, x, y) => void updateJunctionPosition(id, x, y)}
              onJunctionClick={selectJunction}
              onRoadClick={selectRoad}
              onClearSelection={clearSelection}
              onAddJunction={openJunctionModal}
            />
          </div>

          <div className="space-y-4 lg:h-[600px] lg:overflow-y-auto lg:pr-1">
            {panelError && <ErrorBanner message={panelError} />}

            <DetailsPanel
              selection={selection}
              junctions={junctions}
              vehicles={vehicles}
              visualRoads={visualRoads}
              onSelectJunction={selectJunction}
              onSelectRoad={selectRoad}
              onClearSelection={clearSelection}
              onUpdateRoad={handleUpdateRoad}
              onAddRoadFrom={openRoadModal}
            />

          <div className="card p-4">
            <h3 className="mb-3 flex items-center gap-2 text-sm font-semibold text-slate-200">
              <CircleDot className="h-4 w-4 text-cyan-400" /> Junctions ({junctions.length})
            </h3>
            {junctions.length === 0 ? (
              <p className="py-3 text-center text-xs text-slate-500">No junctions added yet.</p>
            ) : (
              <div className="max-h-44 space-y-1 overflow-y-auto">
                {junctions.map((j) => {
                  const active = selection?.kind === 'junction' && selection.id === j.id;
                  const count = roadsAtJunction(visualRoads, j.id).length;
                  const hasVehicle = vehicles.some((v) => v.lastConfirmedJunction === j.id);
                  return (
                    <button
                      type="button"
                      key={j.id}
                      onClick={() => selectJunction(j.id)}
                      className={`flex w-full items-center justify-between rounded-lg px-3 py-2 text-sm transition-colors ${
                        active
                          ? 'border border-cyan-500/30 bg-cyan-500/10 text-cyan-400'
                          : 'border border-transparent text-slate-300 hover:bg-slate-800/40'
                      }`}
                    >
                      <span className="flex items-center gap-2 font-mono">
                        {j.id}
                        {hasVehicle && (
                          <span className="h-1.5 w-1.5 rounded-full bg-cyan-400" aria-label="Vehicle present" />
                        )}
                      </span>
                      <span className="text-xs text-slate-500">
                        {count} road{count === 1 ? '' : 's'}
                      </span>
                    </button>
                  );
                })}
              </div>
            )}
          </div>

          <div className="card p-4">
            <h3 className="mb-3 flex items-center gap-2 text-sm font-semibold text-slate-200">
              <Waypoints className="h-4 w-4 text-sky-400" /> Roads ({visualRoads.length})
            </h3>
            {visualRoads.length === 0 ? (
              <p className="py-3 text-center text-xs text-slate-500">No roads added yet.</p>
            ) : (
              <div className="max-h-44 space-y-1 overflow-y-auto">
                {visualRoads.map((r) => {
                  const active = isRoadSelected(r.from, r.to);
                  return (
                    <button
                      type="button"
                      key={r.edgeId}
                      onClick={() => selectRoad(r.from, r.to)}
                      className={`flex w-full items-center justify-between gap-2 rounded-lg px-3 py-2 text-left text-sm transition-colors ${
                        active
                          ? 'border border-cyan-500/30 bg-cyan-500/10'
                          : 'border border-transparent hover:bg-slate-800/40'
                      }`}
                    >
                      <span className="flex min-w-0 items-center gap-1.5">
                        <span className="truncate font-mono text-xs text-slate-300">
                          {r.from} {r.oneWay ? '\u2192' : '\u2194'} {r.to}
                        </span>
                        {r.closed && <Ban className="h-3 w-3 flex-shrink-0 text-red-400" />}
                      </span>
                      <span className="flex flex-shrink-0 items-center gap-2">
                        <span className="hidden text-[10px] text-slate-600 sm:inline">
                          {r.oneWay ? 'One-way' : 'Two-way'}
                        </span>
                        <span
                          className={`text-xs ${r.closed ? 'font-medium text-red-400' : 'text-slate-500'}`}
                        >
                          {r.closed ? 'Closed' : formatTravelTime(r.travelTime)}
                        </span>
                      </span>
                    </button>
                  );
                })}
              </div>
            )}
          </div>
        </div>
      </div>
      )}

      <Modal
        open={showAddJunction}
        onClose={() => setShowAddJunction(false)}
        title="Add Junction"
        subtitle="Create a junction — its position on the map is chosen automatically"
        footer={
          <>
            <button type="button" onClick={() => setShowAddJunction(false)} className="btn-secondary">
              Cancel
            </button>
            <button type="button" onClick={handleAddJunction} className="btn-primary" disabled={busy}>
              {busy ? 'Adding\u2026' : 'Add Junction'}
            </button>
          </>
        }
      >
        <div className="space-y-4">
          {junctionError && <ErrorBanner message={junctionError} />}
          <div>
            <label className="mb-1 block text-sm text-slate-400" htmlFor="junction-id">
              Junction ID
            </label>
            <input
              id="junction-id"
              className="input-field"
              placeholder="e.g. J1"
              value={jId}
              onChange={(e) => setJId(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === 'Enter') handleAddJunction();
              }}
              autoFocus
            />
          </div>
          <p className="text-xs text-slate-500">
            Use 1–8 letters, numbers, dashes or underscores. After adding, drag the junction on the map
            to position it — connected roads follow automatically.
          </p>
        </div>
      </Modal>

      <Modal
        open={showAddRoad}
        onClose={() => setShowAddRoad(false)}
        title="Create Road"
        subtitle="Connect two junctions with a one-way or two-way road"
        footer={
          <>
            <button type="button" onClick={() => setShowAddRoad(false)} className="btn-secondary">
              Cancel
            </button>
            <button type="button" onClick={handleAddRoad} className="btn-primary" disabled={busy}>
              <Plus className="w-4 h-4" /> {busy ? 'Creating\u2026' : 'Create Road'}
            </button>
          </>
        }
      >
        <div className="space-y-4">
          {roadError && <ErrorBanner message={roadError} />}
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="mb-1 block text-sm text-slate-400" htmlFor="road-from">
                Source junction
              </label>
              <select
                id="road-from"
                className="select-field"
                value={rFrom}
                onChange={(e) => setRFrom(e.target.value)}
              >
                <option value="">Select source...</option>
                {junctions.map((j) => (
                  <option key={j.id} value={j.id}>
                    {j.id}
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label className="mb-1 block text-sm text-slate-400" htmlFor="road-to">
                Destination junction
              </label>
              <select
                id="road-to"
                className="select-field"
                value={rTo}
                onChange={(e) => setRTo(e.target.value)}
              >
                <option value="">Select destination...</option>
                {junctions.map((j) => (
                  <option key={j.id} value={j.id}>
                    {j.id}
                  </option>
                ))}
              </select>
            </div>
          </div>
          <div>
            <label className="mb-1 block text-sm text-slate-400" htmlFor="road-time">
              Travel time (minutes)
            </label>
            <input
              id="road-time"
              className="input-field"
              type="number"
              min={0.1}
              step={0.1}
              placeholder="e.g. 5"
              value={rTime}
              onChange={(e) => setRTime(e.target.value)}
            />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="mb-1 block text-sm text-slate-400" htmlFor="road-type">
                Road type
              </label>
              <select
                id="road-type"
                className="select-field"
                value={rType}
                onChange={(e) => setRType(e.target.value === 'ONE_WAY' ? 'ONE_WAY' : 'TWO_WAY')}
              >
                <option value="ONE_WAY">One-way</option>
                <option value="TWO_WAY">Two-way</option>
              </select>
            </div>
            <div>
              <label className="mb-1 block text-sm text-slate-400" htmlFor="road-status">
                Road status
              </label>
              <select
                id="road-status"
                className="select-field"
                value={rStatus}
                onChange={(e) => setRStatus(e.target.value === 'CLOSED' ? 'CLOSED' : 'OPEN')}
              >
                <option value="OPEN">Open</option>
                <option value="CLOSED">Closed</option>
              </select>
            </div>
          </div>
          <div className="flex items-start gap-2 rounded-lg border border-slate-800 bg-slate-800/30 px-3 py-2">
            <ArrowRight className="mt-0.5 h-3.5 w-3.5 flex-shrink-0 text-sky-400" />
            <p className="text-xs text-slate-500">
              {rType === 'ONE_WAY'
                ? rFrom && rTo
                  ? `Traffic flows only from ${rFrom} to ${rTo}.`
                  : 'Traffic flows only from the source junction to the destination junction.'
                : 'Traffic flows in both directions.'}
              {rStatus === 'CLOSED' ? ' The road will be created as closed.' : ''}
            </p>
          </div>
        </div>
      </Modal>
    </div>
  );
}
