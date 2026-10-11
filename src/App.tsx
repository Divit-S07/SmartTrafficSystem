import { useState } from 'react';
import type { ViewKey } from '@/types';
import { useTrafficStore } from '@/store/useTrafficStore';
import { Sidebar } from '@/components/layout/Sidebar';
import { MobileNav } from '@/components/layout/MobileNav';
import { TopBar } from '@/components/layout/TopBar';
import { Dashboard } from '@/pages/Dashboard';
import { RoadNetwork } from '@/pages/RoadNetwork';
import { EmergencyVehicles } from '@/pages/EmergencyVehicles';
import { RouteManagement } from '@/pages/RouteManagement';
import { TrafficEvents } from '@/pages/TrafficEvents';
import { ActivityLog } from '@/pages/ActivityLog';

function App() {
  const [view, setView] = useState<ViewKey>('dashboard');
  const [collapsed, setCollapsed] = useState(false);
  const store = useTrafficStore();

  return (
    <div className="flex h-screen overflow-hidden bg-[#0a0f1c]">
      <Sidebar
        active={view}
        onNavigate={setView}
        collapsed={collapsed}
      />

      <div className="flex-1 flex flex-col overflow-hidden">
        <MobileNav active={view} onNavigate={setView} />
        <TopBar
          view={view}
          onToggleSidebar={() => setCollapsed(!collapsed)}
          collapsed={collapsed}
        />

        <main className="flex-1 overflow-y-auto p-4 sm:p-6">
          <div className="max-w-7xl mx-auto">
            {view === 'dashboard' && <Dashboard store={store} onNavigate={setView} />}
            {view === 'road-network' && <RoadNetwork store={store} />}
            {view === 'emergency-vehicles' && <EmergencyVehicles store={store} />}
            {view === 'route-management' && <RouteManagement store={store} />}
            {view === 'traffic-events' && <TrafficEvents store={store} />}
            {view === 'activity-log' && <ActivityLog store={store} />}
          </div>
        </main>
      </div>
    </div>
  );
}

export default App;
