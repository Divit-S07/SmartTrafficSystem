import { useState } from 'react';
import type { ViewKey } from '@/types';
import { Menu, X, TrafficCone } from 'lucide-react';

interface MobileNavProps {
  active: ViewKey;
  onNavigate: (view: ViewKey) => void;
}

const NAV_ITEMS: { key: ViewKey; label: string }[] = [
  { key: 'dashboard', label: 'Dashboard' },
  { key: 'road-network', label: 'Road Network' },
  { key: 'emergency-vehicles', label: 'Emergency Vehicles' },
  { key: 'route-management', label: 'Route Management' },
  { key: 'traffic-events', label: 'Traffic Events' },
  { key: 'activity-log', label: 'Activity Log' },
];

export function MobileNav({ active, onNavigate }: MobileNavProps) {
  const [open, setOpen] = useState(false);

  return (
    <div className="md:hidden relative z-40">
      <div className="h-14 bg-[#0d1426] border-b border-slate-800 flex items-center justify-between px-4">
        <div className="flex items-center gap-2">
          <div className="w-8 h-8 rounded-lg bg-cyan-500/10 border border-cyan-500/30 flex items-center justify-center text-cyan-400">
            <TrafficCone className="w-4 h-4" />
          </div>
          <span className="text-sm font-bold text-slate-100">Vegam City Traffic</span>
        </div>
        <button onClick={() => setOpen(!open)} className="text-slate-400 hover:text-slate-200">
          {open ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
        </button>
      </div>

      {open && (
        <div className="absolute top-14 right-0 left-0 bg-[#0d1426] border-b border-slate-800 fade-in-up">
          <nav className="py-2">
            {NAV_ITEMS.map((item) => (
              <button
                key={item.key}
                onClick={() => {
                  onNavigate(item.key);
                  setOpen(false);
                }}
                className={`w-full text-left px-5 py-3 text-sm font-medium transition-colors ${
                  active === item.key
                    ? 'text-cyan-400 bg-cyan-500/5 border-l-2 border-cyan-500'
                    : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/30'
                }`}
              >
                {item.label}
              </button>
            ))}
          </nav>
        </div>
      )}
    </div>
  );
}
