import type { Severity, VehicleStatus, VehicleType, EventType, RouteStatus, EventStatus } from '@/types';
import {
  AlertTriangle,
  Flame,
  LifeBuoy,
  Ambulance,
  CheckCircle2,
  Clock,
  XCircle,
  CircleSlash,
  Ban,
  RefreshCw,
  Gauge,
} from 'lucide-react';

const base = 'badge';

export function SeverityBadge({ severity }: { severity: Severity }) {
  const styles: Record<Severity, string> = {
    CRITICAL: 'bg-red-500/15 text-red-400 border border-red-500/30',
    HIGH: 'bg-amber-500/15 text-amber-400 border border-amber-500/30',
    NORMAL: 'bg-sky-500/15 text-sky-400 border border-sky-500/30',
  };
  return (
    <span className={`${base} ${styles[severity]}`}>
      {severity === 'CRITICAL' && <AlertTriangle className="w-3 h-3" />}
      {severity}
    </span>
  );
}

export function VehicleStatusBadge({ status }: { status: VehicleStatus }) {
  const styles: Record<VehicleStatus, { cls: string; icon: React.ReactNode }> = {
    IDLE: { cls: 'bg-slate-600/20 text-slate-400 border border-slate-600/30', icon: <Clock className="w-3 h-3" /> },
    EN_ROUTE: { cls: 'bg-cyan-500/15 text-cyan-400 border border-cyan-500/30', icon: <Gauge className="w-3 h-3" /> },
    ARRIVED: { cls: 'bg-emerald-500/15 text-emerald-400 border border-emerald-500/30', icon: <CheckCircle2 className="w-3 h-3" /> },
    UNREACHABLE: { cls: 'bg-red-500/15 text-red-400 border border-red-500/30', icon: <XCircle className="w-3 h-3" /> },
  };
  const s = styles[status];
  return (
    <span className={`${base} ${s.cls}`}>
      {s.icon}
      {status.replace('_', ' ')}
    </span>
  );
}

export function VehicleTypeBadge({ type }: { type: VehicleType }) {
  const styles: Record<VehicleType, { cls: string; icon: React.ReactNode }> = {
    AMBULANCE: { cls: 'bg-rose-500/10 text-rose-400 border border-rose-500/20', icon: <Ambulance className="w-3 h-3" /> },
    FIRE_ENGINE: { cls: 'bg-orange-500/10 text-orange-400 border border-orange-500/20', icon: <Flame className="w-3 h-3" /> },
    RESCUE: { cls: 'bg-teal-500/10 text-teal-400 border border-teal-500/20', icon: <LifeBuoy className="w-3 h-3" /> },
  };
  const s = styles[type];
  return (
    <span className={`${base} ${s.cls}`}>
      {s.icon}
      {type.replace('_', ' ')}
    </span>
  );
}

export function EventTypeBadge({ type }: { type: EventType }) {
  const styles: Record<EventType, { cls: string; icon: React.ReactNode }> = {
    CLOSURE: { cls: 'bg-red-500/15 text-red-400 border border-red-500/30', icon: <Ban className="w-3 h-3" /> },
    REOPEN: { cls: 'bg-emerald-500/15 text-emerald-400 border border-emerald-500/30', icon: <RefreshCw className="w-3 h-3" /> },
    CONGESTION: { cls: 'bg-amber-500/15 text-amber-400 border border-amber-500/30', icon: <AlertTriangle className="w-3 h-3" /> },
  };
  const s = styles[type];
  return (
    <span className={`${base} ${s.cls}`}>
      {s.icon}
      {type.charAt(0) + type.slice(1).toLowerCase()}
    </span>
  );
}

export function EventStatusBadge({ status }: { status: EventStatus }) {
  const styles: Record<EventStatus, string> = {
    PENDING: 'bg-amber-500/10 text-amber-400 border border-amber-500/20',
    PROCESSED: 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20',
  };
  return <span className={`${base} ${styles[status]}`}>{status}</span>;
}

export function RouteStatusBadge({ status }: { status: RouteStatus }) {
  const styles: Record<RouteStatus, { cls: string; icon: React.ReactNode }> = {
    VALID: { cls: 'bg-emerald-500/15 text-emerald-400 border border-emerald-500/30', icon: <CheckCircle2 className="w-3 h-3" /> },
    UNREACHABLE: { cls: 'bg-red-500/15 text-red-400 border border-red-500/30', icon: <CircleSlash className="w-3 h-3" /> },
  };
  const s = styles[status];
  return (
    <span className={`${base} ${s.cls}`}>
      {s.icon}
      {status}
    </span>
  );
}
