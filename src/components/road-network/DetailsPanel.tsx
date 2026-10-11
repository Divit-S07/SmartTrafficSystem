import { useEffect, useState } from 'react';
import type { ReactNode } from 'react';
import {
  ArrowLeft,
  ArrowRight,
  Ban,
  CircleDot,
  Clock,
  Info,
  Plus,
  Unlock,
  Waypoints,
  X,
} from 'lucide-react';
import type { EmergencyVehicle, Junction, RoadUpdate } from '@/types';
import type { Selection, VisualRoad } from './networkGeometry';
import { findVisualRoad, formatTravelTime, roadsAtJunction } from './networkGeometry';

interface DetailsPanelProps {
  selection: Selection;
  junctions: Junction[];
  vehicles: EmergencyVehicle[];
  visualRoads: VisualRoad[];
  onSelectJunction: (id: string) => void;
  onSelectRoad: (from: string, to: string) => void;
  onClearSelection: () => void;
  onUpdateRoad: (from: string, to: string, changes: RoadUpdate) => void;
  onAddRoadFrom: (junctionId: string) => void;
}

function StatusBadge({ closed }: { closed: boolean }) {
  return closed ? (
    <span className="badge border border-red-500/30 bg-red-500/15 text-red-400">
      <Ban className="h-3 w-3" /> Closed
    </span>
  ) : (
    <span className="badge border border-emerald-500/30 bg-emerald-500/15 text-emerald-400">
      <Unlock className="h-3 w-3" /> Open
    </span>
  );
}

function TypeBadge({ oneWay }: { oneWay: boolean }) {
  return oneWay ? (
    <span className="badge border border-sky-500/30 bg-sky-500/10 text-sky-400">One-way</span>
  ) : (
    <span className="badge border border-slate-500/30 bg-slate-500/10 text-slate-400">Two-way</span>
  );
}

function Row({ label, children }: { label: string; children: ReactNode }) {
  return (
    <div className="flex items-center justify-between gap-3 py-1.5">
      <span className="text-xs text-slate-500">{label}</span>
      <div className="flex items-center gap-2 text-right">{children}</div>
    </div>
  );
}

function PanelShell({
  icon,
  title,
  subtitle,
  onClear,
  children,
}: {
  icon: React.ReactNode;
  title: string;
  subtitle?: string;
  onClear: () => void;
  children: ReactNode;
}) {
  return (
    <div className="card p-4">
      <div className="mb-3 flex items-start justify-between gap-2">
        <div className="flex items-center gap-2.5">
          <div className="flex h-8 w-8 items-center justify-center rounded-lg border border-cyan-500/20 bg-cyan-500/10 text-cyan-400">
            {icon}
          </div>
          <div>
            <h3 className="text-sm font-semibold text-slate-200">{title}</h3>
            {subtitle && <p className="text-[11px] text-slate-500">{subtitle}</p>}
          </div>
        </div>
        <button
          type="button"
          onClick={onClear}
          title="Clear selection"
          aria-label="Clear selection"
          className="rounded-md p-1 text-slate-500 transition-colors hover:bg-slate-800 hover:text-slate-300"
        >
          <X className="h-4 w-4" />
        </button>
      </div>
      {children}
    </div>
  );
}

function EmptyDetails({ junctionCount, roadCount, closedCount }: { junctionCount: number; roadCount: number; closedCount: number }) {
  const stats = [
    { label: 'Junctions', value: junctionCount },
    { label: 'Roads', value: roadCount },
    { label: 'Closed', value: closedCount },
  ];
  return (
    <div className="card p-4">
      <div className="mb-3 flex items-center gap-2.5">
        <div className="flex h-8 w-8 items-center justify-center rounded-lg border border-cyan-500/20 bg-cyan-500/10 text-cyan-400">
          <Info className="h-4 w-4" />
        </div>
        <h3 className="text-sm font-semibold text-slate-200">Details</h3>
      </div>
      <p className="mb-3 text-xs leading-relaxed text-slate-500">
        Click a junction or a road on the map to view its details and edit it.
      </p>
      <div className="grid grid-cols-3 gap-2">
        {stats.map((s) => (
          <div key={s.label} className="rounded-lg border border-slate-800 bg-slate-800/40 py-2 text-center">
            <div className="text-base font-semibold text-slate-200">{s.value}</div>
            <div className="text-[10px] uppercase tracking-wide text-slate-500">{s.label}</div>
          </div>
        ))}
      </div>
    </div>
  );
}

export function DetailsPanel({
  selection,
  junctions,
  vehicles,
  visualRoads,
  onSelectJunction,
  onSelectRoad,
  onClearSelection,
  onUpdateRoad,
  onAddRoadFrom,
}: DetailsPanelProps) {
  const [timeDraft, setTimeDraft] = useState('');

  const selectedRoad =
    selection?.kind === 'road' ? findVisualRoad(visualRoads, selection.from, selection.to) : undefined;
  const selectedJunction =
    selection?.kind === 'junction' ? junctions.find((j) => j.id === selection.id) : undefined;

  const road = selectedRoad;
  useEffect(() => {
    if (road) {
      setTimeDraft((prev) => {
        const prevNum = parseFloat(prev);
        if (prev !== '' && !isNaN(prevNum) && prevNum === road.travelTime) return prev;
        return String(road.travelTime);
      });
    }
  }, [road]);

  if (selection?.kind === 'road' && road) {
    const timeInvalid = timeDraft !== '' && (isNaN(parseFloat(timeDraft)) || parseFloat(timeDraft) <= 0);

    const handleTimeChange = (value: string) => {
      setTimeDraft(value);
      const parsed = parseFloat(value);
      if (!isNaN(parsed) && parsed > 0) {
        onUpdateRoad(road.from, road.to, { travelTime: parsed });
      }
    };

    return (
      <PanelShell
        icon={<Waypoints className="h-4 w-4" />}
        title={`${road.from} ${road.oneWay ? '\u2192' : '\u2194'} ${road.to}`}
        subtitle="Road details"
        onClear={onClearSelection}
      >
        <div className="mb-2 flex items-center justify-between gap-2">
          <StatusBadge closed={road.closed} />
          <TypeBadge oneWay={road.oneWay} />
        </div>

        <div className="divide-y divide-slate-800/70">
          <Row label="Source junction">
            <button
              type="button"
              onClick={() => onSelectJunction(road.from)}
              className="font-mono text-xs font-semibold text-cyan-400 hover:text-cyan-300"
            >
              {road.from}
            </button>
          </Row>
          <Row label="Destination junction">
            <button
              type="button"
              onClick={() => onSelectJunction(road.to)}
              className="font-mono text-xs font-semibold text-cyan-400 hover:text-cyan-300"
            >
              {road.to}
            </button>
          </Row>
          <Row label="Travel time">
            <div className="flex items-center gap-1.5">
              <Clock className="h-3.5 w-3.5 text-slate-500" />
              <input
                type="number"
                min={0.1}
                step={0.1}
                className="input-field !w-20 !px-2 !py-1 text-right"
                value={timeDraft}
                onChange={(e) => handleTimeChange(e.target.value)}
                onBlur={() => {
                  if (timeInvalid) setTimeDraft(String(road.travelTime));
                }}
                aria-label="Travel time in minutes"
              />
              <span className="text-xs text-slate-500">min</span>
            </div>
          </Row>
          <Row label="Direction">
            <select
              className="select-field !w-28 !px-2 !py-1"
              value={road.oneWay ? 'ONE_WAY' : 'TWO_WAY'}
              onChange={(e) =>
                onUpdateRoad(road.from, road.to, { oneWay: e.target.value === 'ONE_WAY' })
              }
              aria-label="Road direction type"
            >
              <option value="ONE_WAY">One-way</option>
              <option value="TWO_WAY">Two-way</option>
            </select>
          </Row>
          <Row label="Status">
            <span className={`text-xs font-medium ${road.closed ? 'text-red-400' : 'text-emerald-400'}`}>
              {road.closed ? 'Closed' : 'Open'}
            </span>
          </Row>
        </div>

        {timeInvalid && <p className="mt-1 text-[11px] text-red-400">Travel time must be a positive number.</p>}

        <div className="mt-3">
          {road.closed ? (
            <button
              type="button"
              className="btn-primary w-full justify-center"
              onClick={() => onUpdateRoad(road.from, road.to, { closed: false })}
            >
              <Unlock className="h-4 w-4" /> Reopen Road
            </button>
          ) : (
            <button
              type="button"
              className="btn-danger w-full justify-center"
              onClick={() => onUpdateRoad(road.from, road.to, { closed: true })}
            >
              <Ban className="h-4 w-4" /> Close Road
            </button>
          )}
          <p className="mt-2 text-[11px] leading-relaxed text-slate-600">
            {road.closed
              ? 'Closed roads are not used as routes. The change is saved on the backend and reloaded on refresh.'
              : 'Travel time and direction changes are saved on the backend and reloaded on refresh.'}
          </p>
        </div>
      </PanelShell>
    );
  }

  if (selection?.kind === 'junction' && selectedJunction) {
    const connected = roadsAtJunction(visualRoads, selectedJunction.id);
    const here = vehicles.filter((v) => v.lastConfirmedJunction === selectedJunction.id);

    return (
      <PanelShell
        icon={<CircleDot className="h-4 w-4" />}
        title={`Junction ${selectedJunction.id}`}
        subtitle={`${connected.length} connected road${connected.length === 1 ? '' : 's'}`}
        onClear={onClearSelection}
      >
        <div className="mb-2 flex flex-wrap items-center gap-2">
          <span className="badge border border-slate-600/40 bg-slate-700/30 font-mono text-slate-300">
            {selectedJunction.id}
          </span>
          {here.length > 0 && (
            <span className="badge border border-cyan-500/30 bg-cyan-500/10 text-cyan-400">
              {here.length} vehicle{here.length === 1 ? '' : 's'} present
            </span>
          )}
        </div>

        <p className="mb-2 text-[11px] text-slate-500">
          Position: ({Math.round(selectedJunction.x)}, {Math.round(selectedJunction.y)}) — drag on the map
          to move it.
        </p>

        <div className="space-y-1">
          {connected.length === 0 ? (
            <div className="rounded-lg border border-dashed border-slate-700 px-3 py-4 text-center">
              <p className="mb-2 text-xs text-slate-500">No roads connected yet.</p>
              {junctions.length >= 2 ? (
                <button
                  type="button"
                  className="btn-secondary mx-auto !px-3 !py-1.5 text-xs"
                  onClick={() => onAddRoadFrom(selectedJunction.id)}
                >
                  <Plus className="h-3.5 w-3.5" /> Add Road from {selectedJunction.id}
                </button>
              ) : (
                <p className="text-[11px] text-slate-600">
                  Add another junction first to connect this one.
                </p>
              )}
            </div>
          ) : (
            connected.map((r) => (
              <button
                type="button"
                key={r.edgeId}
                onClick={() => onSelectRoad(r.from, r.to)}
                className="flex w-full items-center justify-between gap-2 rounded-lg border border-transparent bg-slate-800/30 px-3 py-2 text-left transition-colors hover:border-cyan-500/30 hover:bg-slate-800/60"
              >
                <span className="flex min-w-0 items-center gap-1.5 font-mono text-xs text-slate-300">
                  {r.from === selectedJunction.id ? (
                    <ArrowRight className="h-3.5 w-3.5 flex-shrink-0 text-sky-400" />
                  ) : (
                    <ArrowLeft className="h-3.5 w-3.5 flex-shrink-0 text-sky-400" />
                  )}
                  <span className="truncate">
                    {r.from} {r.oneWay ? '\u2192' : '\u2194'} {r.to}
                  </span>
                </span>
                <span className="flex flex-shrink-0 items-center gap-1.5">
                  {r.closed ? (
                    <span className="text-[11px] font-medium text-red-400">Closed</span>
                  ) : (
                    <span className="text-[11px] text-slate-400">{formatTravelTime(r.travelTime)}</span>
                  )}
                </span>
              </button>
            ))
          )}
        </div>
      </PanelShell>
    );
  }

  const closedCount = visualRoads.filter((r) => r.closed).length;
  return (
    <EmptyDetails junctionCount={junctions.length} roadCount={visualRoads.length} closedCount={closedCount} />
  );
}
