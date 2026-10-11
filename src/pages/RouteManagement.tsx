import { useState } from 'react';
import type { TrafficStore } from '@/store/useTrafficStore';
import type { Severity, Route as RouteType } from '@/types';
import { EmptyState, ErrorBanner, SectionHeader } from '@/components/ui/Common';
import { SeverityBadge, RouteStatusBadge } from '@/components/ui/Badges';
import {
  Route as RouteIcon,
  Plus,
  Send,
  ArrowRight,
  Play,
  TrendingUp,
  Clock,
  MapPin,
  Navigation,
  Zap,
} from 'lucide-react';

interface RouteManagementProps {
  store: TrafficStore;
}

export function RouteManagement({ store }: RouteManagementProps) {
  const { vehicles, junctions, requests, submitRequest, escalateRequest, processNextRequest, findRoute } = store;

  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [fVehicle, setFVehicle] = useState('');
  const [fDestination, setFDestination] = useState('');
  const [fSeverity, setFSeverity] = useState<Severity>('NORMAL');
  const [escalateId, setEscalateId] = useState('');
  const [escalateSeverity, setEscalateSeverity] = useState<Severity>('CRITICAL');
  const [routeResult, setRouteResult] = useState<RouteType | null>(null);

  const pendingRequests = requests
    .filter((r) => r.status === 'PENDING')
    .sort((a, b) => {
      const ra = a.severity === 'CRITICAL' ? 3 : a.severity === 'HIGH' ? 2 : 1;
      const rb = b.severity === 'CRITICAL' ? 3 : b.severity === 'HIGH' ? 2 : 1;
      if (rb !== ra) return rb - ra;
      return a.arrivalSequence - b.arrivalSequence;
    });

  const processedRequests = requests.filter((r) => r.status === 'PROCESSED');

  const handleSubmit = () => {
    setError(''); setSuccess('');
    if (!fVehicle) { setError('Select a vehicle.'); return; }
    if (!fDestination) { setError('Select a destination junction.'); return; }
    const reqId = submitRequest(fVehicle, fDestination, fSeverity);
    if (reqId) {
      setSuccess(`Request ${reqId} submitted successfully.`);
      setFVehicle(''); setFDestination(''); setFSeverity('NORMAL');
    } else {
      setError('Failed to submit request.');
    }
  };

  const handlePreviewRoute = () => {
    setError('');
    if (!fVehicle || !fDestination) { setError('Select a vehicle and destination to preview the route.'); return; }
    const vehicle = vehicles.find((v) => v.id === fVehicle);
    if (!vehicle) return;
    setRouteResult(findRoute(vehicle.lastConfirmedJunction, fDestination));
  };

  const handleProcessNext = () => {
    setError('');
    const result = processNextRequest();
    if (!result) setError('No pending requests to process.');
  };

  const handleEscalate = () => {
    setError('');
    if (!escalateId) { setError('Enter a request ID to escalate.'); return; }
    const req = requests.find((r) => r.id === escalateId && r.status === 'PENDING');
    if (!req) { setError(`Request ${escalateId} not found or already processed.`); return; }
    escalateRequest(escalateId, escalateSeverity);
    setEscalateId('');
  };

  return (
    <div className="space-y-4 fade-in-up">
      <SectionHeader
        title="Route Management"
        subtitle="Submit priority routing requests and process the queue"
        icon={<RouteIcon className="w-5 h-5" />}
        action={
          <button onClick={handleProcessNext} className="btn-primary" disabled={pendingRequests.length === 0}>
            <Play className="w-4 h-4" /> Process Next Request
          </button>
        }
      />

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
        <div className="card p-5">
          <h3 className="text-sm font-semibold text-slate-200 mb-4 flex items-center gap-2">
            <Send className="w-4 h-4 text-cyan-400" /> Submit Routing Request
          </h3>
          {vehicles.length === 0 || junctions.length === 0 ? (
            <p className="text-sm text-slate-500 py-4 text-center">
              {junctions.length === 0 ? 'Add junctions to the road network first.' : 'Register emergency vehicles first.'}
            </p>
          ) : (
            <div className="space-y-4">
              {error && <ErrorBanner message={error} />}
              {success && (
                <div className="px-3 py-2 bg-emerald-500/10 border border-emerald-500/30 rounded-lg text-emerald-400 text-sm">{success}</div>
              )}
              <div>
                <label className="text-sm text-slate-400 mb-1 block">Vehicle</label>
                <select className="select-field" value={fVehicle} onChange={(e) => { setFVehicle(e.target.value); setRouteResult(null); }}>
                  <option value="">Select vehicle...</option>
                  {vehicles.map((v) => (
                    <option key={v.id} value={v.id}>{v.id} \u2014 {v.type.replace('_', ' ')} (at {v.lastConfirmedJunction})</option>
                  ))}
                </select>
              </div>
              <div>
                <label className="text-sm text-slate-400 mb-1 block">Destination Junction</label>
                <select className="select-field" value={fDestination} onChange={(e) => { setFDestination(e.target.value); setRouteResult(null); }}>
                  <option value="">Select destination...</option>
                  {junctions.map((j) => <option key={j.id} value={j.id}>{j.id}</option>)}
                </select>
              </div>
              <div>
                <label className="text-sm text-slate-400 mb-1 block">Priority / Severity</label>
                <div className="grid grid-cols-3 gap-2">
                  {(['NORMAL', 'HIGH', 'CRITICAL'] as Severity[]).map((sev) => (
                    <button
                      key={sev}
                      onClick={() => setFSeverity(sev)}
                      className={`px-3 py-2 rounded-lg text-sm font-medium border transition-all ${
                        fSeverity === sev
                          ? sev === 'CRITICAL' ? 'bg-red-500/15 text-red-400 border-red-500/40'
                          : sev === 'HIGH' ? 'bg-amber-500/15 text-amber-400 border-amber-500/40'
                          : 'bg-sky-500/15 text-sky-400 border-sky-500/40'
                          : 'bg-slate-800/30 text-slate-400 border-slate-700/30 hover:border-slate-600'
                      }`}
                    >
                      {sev}
                    </button>
                  ))}
                </div>
              </div>
              <div className="flex gap-2">
                <button onClick={handleSubmit} className="btn-primary flex-1 justify-center">
                  <Plus className="w-4 h-4" /> Submit Request
                </button>
                <button onClick={handlePreviewRoute} className="btn-secondary" disabled={!fVehicle || !fDestination}>
                  <Navigation className="w-4 h-4" /> Preview Route
                </button>
              </div>
            </div>
          )}
        </div>

        <div className="card p-5">
          <h3 className="text-sm font-semibold text-slate-200 mb-4 flex items-center gap-2">
            <MapPin className="w-4 h-4 text-cyan-400" /> Route Preview
          </h3>
          {!routeResult ? (
            <p className="text-sm text-slate-500 py-4 text-center">
              Select a vehicle and destination, then click "Preview Route" to see the computed path.
            </p>
          ) : (
            <div className="space-y-4 fade-in-up">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2 text-sm text-slate-400">
                  <span className="font-mono">{routeResult.requestedFrom}</span>
                  <ArrowRight className="w-4 h-4 text-cyan-400" />
                  <span className="font-mono">{routeResult.requestedTo}</span>
                </div>
                <RouteStatusBadge status={routeResult.status} />
              </div>
              {routeResult.status === 'VALID' ? (
                <>
                  <div className="bg-slate-800/30 rounded-lg p-4 border border-slate-700/30">
                    <div className="flex flex-wrap items-center gap-1 text-sm font-mono">
                      {routeResult.path.map((node, i) => (
                        <span key={i} className="flex items-center gap-1">
                          <span className={`px-2 py-1 rounded ${
                            i === 0 ? 'bg-cyan-500/15 text-cyan-400'
                            : i === routeResult.path.length - 1 ? 'bg-emerald-500/15 text-emerald-400'
                            : 'bg-slate-700/30 text-slate-300'
                          }`}>{node}</span>
                          {i < routeResult.path.length - 1 && <ArrowRight className="w-4 h-4 text-slate-600" />}
                        </span>
                      ))}
                    </div>
                  </div>
                  <div className="flex items-center gap-4">
                    <div className="flex items-center gap-2 text-sm">
                      <Clock className="w-4 h-4 text-slate-500" />
                      <span className="text-slate-400">Total time:</span>
                      <span className="font-semibold text-cyan-400">{routeResult.totalTravelTime} min</span>
                    </div>
                    <div className="flex items-center gap-2 text-sm">
                      <RouteIcon className="w-4 h-4 text-slate-500" />
                      <span className="text-slate-400">Hops:</span>
                      <span className="font-semibold text-slate-200">{routeResult.path.length - 1}</span>
                    </div>
                  </div>
                </>
              ) : (
                <div className="px-4 py-6 bg-red-500/5 border border-red-500/20 rounded-lg text-center">
                  <p className="text-red-400 text-sm font-medium">No valid route found</p>
                  <p className="text-slate-500 text-xs mt-1">
                    The destination is unreachable from the vehicle's current position. This may be due to road closures or missing connections.
                  </p>
                </div>
              )}
            </div>
          )}
        </div>
      </div>

      <div className="card p-5">
        <h3 className="text-sm font-semibold text-slate-200 mb-4 flex items-center gap-2">
          <TrendingUp className="w-4 h-4 text-amber-400" /> Escalate Request Priority
        </h3>
        {pendingRequests.length === 0 ? (
          <p className="text-sm text-slate-500 py-2">No pending requests to escalate.</p>
        ) : (
          <div className="flex flex-col sm:flex-row gap-3 items-end">
            <div className="flex-1 w-full">
              <label className="text-sm text-slate-400 mb-1 block">Request ID</label>
              <select className="select-field" value={escalateId} onChange={(e) => setEscalateId(e.target.value)}>
                <option value="">Select request...</option>
                {pendingRequests.map((r) => (
                  <option key={r.id} value={r.id}>{r.id} \u2014 {r.vehicleId} \u2192 {r.destination} ({r.severity})</option>
                ))}
              </select>
            </div>
            <div className="w-full sm:w-40">
              <label className="text-sm text-slate-400 mb-1 block">New Severity</label>
              <select className="select-field" value={escalateSeverity} onChange={(e) => setEscalateSeverity(e.target.value as Severity)}>
                <option value="NORMAL">NORMAL</option>
                <option value="HIGH">HIGH</option>
                <option value="CRITICAL">CRITICAL</option>
              </select>
            </div>
            <button onClick={handleEscalate} className="btn-secondary w-full sm:w-auto" disabled={!escalateId}>
              <Zap className="w-4 h-4" /> Escalate
            </button>
          </div>
        )}
      </div>

      <div className="card p-5">
        <h3 className="text-sm font-semibold text-slate-200 mb-4 flex items-center gap-2">
          <Clock className="w-4 h-4 text-amber-400" /> Pending Requests Queue
          <span className="badge bg-amber-500/10 text-amber-400 border border-amber-500/20">{pendingRequests.length}</span>
        </h3>
        {pendingRequests.length === 0 ? (
          <p className="text-sm text-slate-500 py-4 text-center">No pending routing requests.</p>
        ) : (
          <div className="space-y-2">
            {pendingRequests.map((req, idx) => (
              <div key={req.id} className="flex items-center justify-between px-4 py-3 rounded-lg bg-slate-800/30 border border-slate-700/30">
                <div className="flex items-center gap-3 min-w-0">
                  <span className={`text-xs font-bold w-6 h-6 rounded flex items-center justify-center ${
                    idx === 0 ? 'bg-cyan-500/20 text-cyan-400' : 'bg-slate-700/30 text-slate-500'
                  }`}>{idx + 1}</span>
                  <span className="font-mono text-sm text-slate-300">{req.id}</span>
                  <SeverityBadge severity={req.severity} />
                  <span className="text-xs text-slate-500 hidden sm:block">{req.vehicleId} \u2192 {req.destination}</span>
                </div>
                <span className="text-xs text-slate-600">#{req.arrivalSequence}</span>
              </div>
            ))}
          </div>
        )}
      </div>

      {processedRequests.length > 0 && (
        <div className="card p-5">
          <h3 className="text-sm font-semibold text-slate-200 mb-4 flex items-center gap-2">
            <RouteIcon className="w-4 h-4 text-emerald-400" /> Processed Requests
            <span className="badge bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">{processedRequests.length}</span>
          </h3>
          <div className="space-y-2">
            {processedRequests.slice().reverse().map((req) => {
              const vehicle = vehicles.find((v) => v.id === req.vehicleId);
              return (
                <div key={req.id} className="flex items-center justify-between px-4 py-3 rounded-lg bg-slate-800/20 border border-slate-700/20">
                  <div className="flex items-center gap-3 min-w-0">
                    <span className="font-mono text-sm text-slate-400">{req.id}</span>
                    <SeverityBadge severity={req.severity} />
                    <span className="text-xs text-slate-500">{req.vehicleId} \u2192 {req.destination}</span>
                  </div>
                  <div className="flex items-center gap-2">
                    {vehicle?.currentRoute && <RouteStatusBadge status={vehicle.currentRoute.status} />}
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      )}
    </div>
  );
}
