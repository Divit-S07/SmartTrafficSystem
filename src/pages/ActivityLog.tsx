import { useState } from 'react';
import type { TrafficStore } from '@/store/useTrafficStore';
import type { ActivityEntry } from '@/types';
import { EmptyState, SectionHeader } from '@/components/ui/Common';
import {
  ScrollText,
  Search,
  Route as RouteIcon,
  Ambulance,
  AlertTriangle,
  Settings,
} from 'lucide-react';

interface ActivityLogProps {
  store: TrafficStore;
}

const CATEGORY_CONFIG: Record<ActivityEntry['category'], { label: string; icon: React.ReactNode; color: string }> = {
  ROUTING_DECISION: { label: 'Routing Decision', icon: <RouteIcon className="w-4 h-4" />, color: 'text-sky-400 bg-sky-500/10 border-sky-500/20' },
  VEHICLE_MOVEMENT: { label: 'Vehicle Movement', icon: <Ambulance className="w-4 h-4" />, color: 'text-cyan-400 bg-cyan-500/10 border-cyan-500/20' },
  TRAFFIC_UPDATE: { label: 'Traffic Update', icon: <AlertTriangle className="w-4 h-4" />, color: 'text-amber-400 bg-amber-500/10 border-amber-500/20' },
  SYSTEM: { label: 'System', icon: <Settings className="w-4 h-4" />, color: 'text-slate-400 bg-slate-600/10 border-slate-600/20' },
};

export function ActivityLog({ store }: ActivityLogProps) {
  const { activity } = store;
  const [search, setSearch] = useState('');
  const [categoryFilter, setCategoryFilter] = useState('ALL');

  const filtered = activity.filter((entry) => {
    const matchSearch = !search ||
      entry.message.toLowerCase().includes(search.toLowerCase()) ||
      (entry.details?.toLowerCase().includes(search.toLowerCase()) ?? false);
    const matchCategory = categoryFilter === 'ALL' || entry.category === categoryFilter;
    return matchSearch && matchCategory;
  });

  const formatTime = (ts: number) => {
    const d = new Date(ts);
    return d.toLocaleTimeString('en-US', { hour12: false, hour: '2-digit', minute: '2-digit', second: '2-digit' });
  };

  return (
    <div className="space-y-4 fade-in-up">
      <SectionHeader
        title="Activity Log"
        subtitle="Routing decisions, vehicle movements and traffic updates"
        icon={<ScrollText className="w-5 h-5" />}
      />

      {activity.length > 0 && (
        <div className="flex flex-col sm:flex-row gap-3">
          <div className="relative flex-1">
            <Search className="w-4 h-4 text-slate-500 absolute left-3 top-1/2 -translate-y-1/2" />
            <input className="input-field pl-9" placeholder="Search activity log..." value={search} onChange={(e) => setSearch(e.target.value)} />
          </div>
          <select className="select-field sm:w-48" value={categoryFilter} onChange={(e) => setCategoryFilter(e.target.value)}>
            <option value="ALL">All Categories</option>
            <option value="ROUTING_DECISION">Routing Decisions</option>
            <option value="VEHICLE_MOVEMENT">Vehicle Movements</option>
            <option value="TRAFFIC_UPDATE">Traffic Updates</option>
            <option value="SYSTEM">System</option>
          </select>
        </div>
      )}

      {activity.length === 0 ? (
        <div className="card">
          <EmptyState
            icon={<ScrollText className="w-8 h-8" />}
            title="No activity recorded"
            message="Actions performed across the system \u2014 adding junctions, registering vehicles, submitting requests, processing events and advancing vehicles \u2014 will appear here in chronological order."
          />
        </div>
      ) : filtered.length === 0 ? (
        <div className="card p-8 text-center text-slate-500 text-sm">No entries match the current filters.</div>
      ) : (
        <div className="card overflow-hidden">
          <div className="divide-y divide-slate-800/50">
            {filtered.map((entry) => {
              const cat = CATEGORY_CONFIG[entry.category];
              return (
                <div key={entry.id} className="flex items-start gap-4 px-5 py-3 hover:bg-slate-800/20 transition-colors">
                  <div className={`flex-shrink-0 w-9 h-9 rounded-lg border flex items-center justify-center ${cat.color}`}>
                    {cat.icon}
                  </div>
                  <div className="min-w-0 flex-1">
                    <div className="flex items-center gap-2 mb-0.5">
                      <span className="text-xs font-medium text-slate-500">{cat.label}</span>
                      <span className="text-xs text-slate-600">\u00b7</span>
                      <span className="text-xs font-mono text-slate-600">{formatTime(entry.timestamp)}</span>
                    </div>
                    <p className="text-sm text-slate-300">{entry.message}</p>
                    {entry.details && <p className="text-xs text-slate-500 mt-1 font-mono">{entry.details}</p>}
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
