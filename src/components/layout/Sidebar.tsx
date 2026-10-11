import type { ViewKey } from '@/types';
import {
  LayoutDashboard,
  Network,
  Ambulance,
  Route as RouteIcon,
  CalendarClock,
  ScrollText,
  TrafficCone,
} from 'lucide-react';

interface SidebarProps {
  active: ViewKey;
  onNavigate: (view: ViewKey) => void;
  collapsed: boolean;
}

const NAV_ITEMS: { key: ViewKey; label: string; icon: React.ReactNode; description: string }[] = [
  { key: 'dashboard', label: 'Dashboard', icon: <LayoutDashboard className="w-5 h-5" />, description: 'System overview' },
  { key: 'road-network', label: 'Road Network', icon: <Network className="w-5 h-5" />, description: 'Junctions & roads' },
  { key: 'emergency-vehicles', label: 'Emergency Vehicles', icon: <Ambulance className="w-5 h-5" />, description: 'Vehicle registry' },
  { key: 'route-management', label: 'Route Management', icon: <RouteIcon className="w-5 h-5" />, description: 'Priority routing' },
  { key: 'traffic-events', label: 'Traffic Events', icon: <CalendarClock className="w-5 h-5" />, description: 'Closures & congestion' },
  { key: 'activity-log', label: 'Activity Log', icon: <ScrollText className="w-5 h-5" />, description: 'Decision history' },
];

export function Sidebar({ active, onNavigate, collapsed }: SidebarProps) {
  return (
    <aside
      className={`${
        collapsed ? 'w-20' : 'w-64'
      } flex-shrink-0 bg-[#0d1426] border-r border-slate-800 flex-col transition-all duration-200 hidden md:flex`}
    >
      <div className="h-16 flex items-center gap-3 px-5 border-b border-slate-800 flex-shrink-0">
        <div className="w-9 h-9 rounded-lg bg-cyan-500/10 border border-cyan-500/30 flex items-center justify-center text-cyan-400 flex-shrink-0">
          <TrafficCone className="w-5 h-5" />
        </div>
        {!collapsed && (
          <div className="overflow-hidden">
            <p className="text-sm font-bold text-slate-100 leading-tight">Vegam City</p>
            <p className="text-xs text-slate-500 leading-tight">Traffic Control</p>
          </div>
        )}
      </div>

      <nav className="flex-1 py-3 px-3 space-y-1 overflow-y-auto">
        {NAV_ITEMS.map((item) => {
          const isActive = active === item.key;
          return (
            <button
              key={item.key}
              onClick={() => onNavigate(item.key)}
              className={`w-full flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-all group relative ${
                isActive
                  ? 'bg-cyan-500/10 text-cyan-400 border border-cyan-500/20'
                  : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/50 border border-transparent'
              }`}
            >
              <span className="flex-shrink-0">{item.icon}</span>
              {!collapsed && (
                <div className="overflow-hidden text-left">
                  <p className="leading-tight">{item.label}</p>
                  <p className="text-xs text-slate-600 leading-tight">{item.description}</p>
                </div>
              )}
              {collapsed && (
                <div className="absolute left-full ml-2 px-2 py-1 bg-slate-900 border border-slate-700 rounded text-xs text-slate-200 opacity-0 group-hover:opacity-100 pointer-events-none whitespace-nowrap z-50 transition-opacity">
                  {item.label}
                </div>
              )}
            </button>
          );
        })}
      </nav>

      {!collapsed && (
        <div className="p-4 border-t border-slate-800 flex-shrink-0">
          <div className="rounded-lg bg-slate-800/40 border border-slate-700/50 p-3">
            <p className="text-xs text-slate-500 leading-relaxed">
              Frontend UI for the Smart Traffic Management &amp; Emergency Routing System.
              Data is session-only.
            </p>
          </div>
        </div>
      )}
    </aside>
  );
}
