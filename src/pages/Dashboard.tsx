import type { TrafficStore } from '@/store/useTrafficStore';
import { EmptyState } from '@/components/ui/Common';
import {
  Network,
  Ambulance,
  Route as RouteIcon,
  CalendarClock,
  TrendingUp,
  Activity,
  CircleDot,
  ArrowRight,
  Ban,
  Gauge,
} from 'lucide-react';
import type { ViewKey } from '@/types';
import { SeverityBadge, EventStatusBadge } from '@/components/ui/Badges';

interface DashboardProps {
  store: TrafficStore;
  onNavigate: (view: ViewKey) => void;
}

export function Dashboard({ store, onNavigate }: DashboardProps) {
  const { junctions, roads, vehicles, requests, events } = store;

  const closedRoads = roads.filter((r) => r.closed);
  const congestedRoads = roads.filter((r) => !r.closed && r.currentTravelTime > r.baseTravelTime);
  const pendingEvents = events.filter((e) => e.status === 'PENDING');
  const pendingRequests = requests.filter((r) => r.status === 'PENDING');
  const enRouteVehicles = vehicles.filter((v) => v.status === 'EN_ROUTE');
  const idleVehicles = vehicles.filter((v) => v.status === 'IDLE');
  const unreachableVehicles = vehicles.filter((v) => v.status === 'UNREACHABLE');

  const stats = [
    { label: 'Junctions', value: junctions.length, icon: <CircleDot className="w-5 h-5" />, view: 'road-network' as ViewKey, color: 'text-cyan-400' },
    { label: 'Road Segments', value: roads.length, icon: <Network className="w-5 h-5" />, view: 'road-network' as ViewKey, color: 'text-sky-400' },
    { label: 'Vehicles', value: vehicles.length, icon: <Ambulance className="w-5 h-5" />, view: 'emergency-vehicles' as ViewKey, color: 'text-rose-400' },
    { label: 'Pending Requests', value: pendingRequests.length, icon: <RouteIcon className="w-5 h-5" />, view: 'route-management' as ViewKey, color: 'text-amber-400' },
  ];

  return (
    <div className="space-y-6 fade-in-up">
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        {stats.map((stat) => (
          <button
            key={stat.label}
            onClick={() => onNavigate(stat.view)}
            className="card card-hover p-4 text-left group"
          >
            <div className="flex items-center justify-between mb-3">
              <div className={`w-10 h-10 rounded-lg bg-slate-800/50 border border-slate-700/50 flex items-center justify-center ${stat.color}`}>
                {stat.icon}
              </div>
              <ArrowRight className="w-4 h-4 text-slate-600 group-hover:text-cyan-400 transition-colors" />
            </div>
            <p className="text-2xl font-bold text-slate-100">{stat.value}</p>
            <p className="stat-label mt-1">{stat.label}</p>
          </button>
        ))}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
        <div className="card p-5">
          <div className="flex items-center gap-2 mb-4">
            <Network className="w-5 h-5 text-cyan-400" />
            <h3 className="text-sm font-semibold text-slate-200">Road Network Status</h3>
          </div>
          {roads.length === 0 ? (
            <p className="text-sm text-slate-500 py-4 text-center">No roads added yet.</p>
          ) : (
            <div className="space-y-3">
              <div className="flex items-center justify-between">
                <span className="text-sm text-slate-400 flex items-center gap-2">
                  <span className="w-2 h-2 rounded-full bg-emerald-400" /> Open
                </span>
                <span className="text-sm font-semibold text-slate-200">
                  {roads.length - closedRoads.length - congestedRoads.length}
                </span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-sm text-slate-400 flex items-center gap-2">
                  <Gauge className="w-3 h-3 text-amber-400" /> Congested
                </span>
                <span className="text-sm font-semibold text-amber-400">{congestedRoads.length}</span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-sm text-slate-400 flex items-center gap-2">
                  <Ban className="w-3 h-3 text-red-400" /> Closed
                </span>
                <span className="text-sm font-semibold text-red-400">{closedRoads.length}</span>
              </div>
              <div className="pt-3 border-t border-slate-800">
                <div className="flex items-center justify-between text-xs text-slate-500">
                  <span>Total segments</span>
                  <span>{roads.length}</span>
                </div>
              </div>
            </div>
          )}
        </div>

        <div className="card p-5">
          <div className="flex items-center gap-2 mb-4">
            <Ambulance className="w-5 h-5 text-rose-400" />
            <h3 className="text-sm font-semibold text-slate-200">Vehicle Fleet Status</h3>
          </div>
          {vehicles.length === 0 ? (
            <p className="text-sm text-slate-500 py-4 text-center">No vehicles registered.</p>
          ) : (
            <div className="space-y-3">
              <div className="flex items-center justify-between">
                <span className="text-sm text-slate-400">Idle</span>
                <span className="text-sm font-semibold text-slate-300">{idleVehicles.length}</span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-sm text-slate-400">En Route</span>
                <span className="text-sm font-semibold text-cyan-400">{enRouteVehicles.length}</span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-sm text-slate-400">Unreachable</span>
                <span className="text-sm font-semibold text-red-400">{unreachableVehicles.length}</span>
              </div>
              <div className="pt-3 border-t border-slate-800">
                <div className="flex items-center justify-between text-xs text-slate-500">
                  <span>Total vehicles</span>
                  <span>{vehicles.length}</span>
                </div>
              </div>
            </div>
          )}
        </div>

        <div className="card p-5">
          <div className="flex items-center gap-2 mb-4">
            <Activity className="w-5 h-5 text-amber-400" />
            <h3 className="text-sm font-semibold text-slate-200">Queue Status</h3>
          </div>
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <span className="text-sm text-slate-400">Pending traffic events</span>
              <span className="text-sm font-semibold text-amber-400">{pendingEvents.length}</span>
            </div>
            <div className="flex items-center justify-between">
              <span className="text-sm text-slate-400">Pending routing requests</span>
              <span className="text-sm font-semibold text-sky-400">{pendingRequests.length}</span>
            </div>
            <div className="flex items-center justify-between">
              <span className="text-sm text-slate-400">Processed events</span>
              <span className="text-sm font-semibold text-emerald-400">
                {events.filter((e) => e.status === 'PROCESSED').length}
              </span>
            </div>
            <div className="pt-3 border-t border-slate-800">
              <div className="flex items-center justify-between text-xs text-slate-500">
                <span>Processed requests</span>
                <span>{requests.filter((r) => r.status === 'PROCESSED').length}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
        <div className="card p-5">
          <div className="flex items-center justify-between mb-4">
            <div className="flex items-center gap-2">
              <RouteIcon className="w-5 h-5 text-sky-400" />
              <h3 className="text-sm font-semibold text-slate-200">Pending Routing Requests</h3>
            </div>
            {pendingRequests.length > 0 && (
              <button onClick={() => onNavigate('route-management')} className="text-xs text-cyan-400 hover:text-cyan-300 flex items-center gap-1">
                View all <ArrowRight className="w-3 h-3" />
              </button>
            )}
          </div>
          {pendingRequests.length === 0 ? (
            <p className="text-sm text-slate-500 py-4 text-center">No pending routing requests.</p>
          ) : (
            <div className="space-y-2">
              {pendingRequests.slice(0, 5).map((req) => (
                <div key={req.id} className="flex items-center justify-between px-3 py-2 rounded-lg bg-slate-800/30 border border-slate-700/30">
                  <div className="flex items-center gap-3">
                    <span className="text-sm font-mono text-slate-300">{req.id}</span>
                    <SeverityBadge severity={req.severity} />
                  </div>
                  <span className="text-xs text-slate-500">{req.vehicleId} \u2192 {req.destination}</span>
                </div>
              ))}
            </div>
          )}
        </div>

        <div className="card p-5">
          <div className="flex items-center justify-between mb-4">
            <div className="flex items-center gap-2">
              <CalendarClock className="w-5 h-5 text-amber-400" />
              <h3 className="text-sm font-semibold text-slate-200">Recent Traffic Events</h3>
            </div>
            {events.length > 0 && (
              <button onClick={() => onNavigate('traffic-events')} className="text-xs text-cyan-400 hover:text-cyan-300 flex items-center gap-1">
                View all <ArrowRight className="w-3 h-3" />
              </button>
            )}
          </div>
          {events.length === 0 ? (
            <p className="text-sm text-slate-500 py-4 text-center">No traffic events queued.</p>
          ) : (
            <div className="space-y-2">
              {events.slice(-5).reverse().map((evt) => (
                <div key={evt.id} className="flex items-center justify-between px-3 py-2 rounded-lg bg-slate-800/30 border border-slate-700/30">
                  <div className="flex items-center gap-3 min-w-0">
                    <span className="text-xs font-mono text-slate-500">#{evt.sequence}</span>
                    <span className="text-sm text-slate-300 truncate">{evt.description}</span>
                  </div>
                  <EventStatusBadge status={evt.status} />
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {junctions.length === 0 && vehicles.length === 0 && events.length === 0 && (
        <div className="card p-6">
          <EmptyState
            icon={<TrendingUp className="w-8 h-8" />}
            title="Welcome to Vegam City Traffic Control"
            message="Start by adding junctions and roads to build your road network, then register emergency vehicles and submit routing requests."
            actionLabel="Build Road Network"
            onAction={() => onNavigate('road-network')}
          />
        </div>
      )}
    </div>
  );
}
