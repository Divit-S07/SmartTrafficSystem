import type { ViewKey } from '@/types';
import { PanelLeftClose, PanelLeftOpen } from 'lucide-react';

interface TopBarProps {
  view: ViewKey;
  onToggleSidebar: () => void;
  collapsed: boolean;
}

const TITLES: Record<ViewKey, { title: string; subtitle: string }> = {
  dashboard: { title: 'Dashboard', subtitle: 'System overview and traffic-management status' },
  'road-network': { title: 'Road Network', subtitle: 'Interactive visualisation of junctions, roads and routes' },
  'emergency-vehicles': { title: 'Emergency Vehicles', subtitle: 'Register and monitor emergency vehicle fleet' },
  'route-management': { title: 'Route Management', subtitle: 'Priority routing requests and route results' },
  'traffic-events': { title: 'Traffic Events', subtitle: 'Road closures, reopens and congestion management' },
  'activity-log': { title: 'Activity Log', subtitle: 'Routing decisions, vehicle movements and traffic updates' },
};

export function TopBar({ view, onToggleSidebar, collapsed }: TopBarProps) {
  const info = TITLES[view];

  return (
    <header className="h-16 border-b border-slate-800 bg-[#0d1426]/80 backdrop-blur-sm flex items-center justify-between px-5 flex-shrink-0">
      <div className="flex items-center gap-3">
        <button
          onClick={onToggleSidebar}
          className="text-slate-500 hover:text-slate-300 transition-colors hidden md:block"
          title={collapsed ? 'Expand sidebar' : 'Collapse sidebar'}
        >
          {collapsed ? <PanelLeftOpen className="w-5 h-5" /> : <PanelLeftClose className="w-5 h-5" />}
        </button>
        <div>
          <h1 className="text-base font-semibold text-slate-100">{info.title}</h1>
          <p className="text-xs text-slate-500 hidden sm:block">{info.subtitle}</p>
        </div>
      </div>

      <div className="flex items-center gap-2">
        <div className="flex items-center gap-2 px-3 py-1.5 rounded-lg bg-slate-800/50 border border-slate-700/50">
          <div className="w-2 h-2 rounded-full bg-emerald-400 pulse-cyan" />
          <span className="text-xs text-slate-400 font-medium">System Active</span>
        </div>
      </div>
    </header>
  );
}
