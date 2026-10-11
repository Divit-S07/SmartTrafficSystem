import type { ReactNode } from 'react';
import { AlertCircle, Loader2, PlusCircle, Inbox, RefreshCw } from 'lucide-react';

interface EmptyStateProps {
  icon?: ReactNode;
  title: string;
  message: string;
  actionLabel?: string;
  onAction?: () => void;
}

export function EmptyState({ icon, title, message, actionLabel, onAction }: EmptyStateProps) {
  return (
    <div className="flex flex-col items-center justify-center py-16 px-4 text-center fade-in-up">
      <div className="w-16 h-16 rounded-full bg-slate-800/80 border border-slate-700 flex items-center justify-center mb-4 text-slate-500">
        {icon ?? <Inbox className="w-8 h-8" />}
      </div>
      <h3 className="text-slate-300 font-semibold text-lg mb-1">{title}</h3>
      <p className="text-slate-500 text-sm max-w-sm mb-4">{message}</p>
      {actionLabel && onAction && (
        <button onClick={onAction} className="btn-primary">
          <PlusCircle className="w-4 h-4" />
          {actionLabel}
        </button>
      )}
    </div>
  );
}

export function ErrorBanner({ message }: { message: string }) {
  if (!message) return null;
  return (
    <div className="flex items-center gap-2 px-3 py-2 bg-red-500/10 border border-red-500/30 rounded-lg text-red-400 text-sm fade-in-up">
      <AlertCircle className="w-4 h-4 flex-shrink-0" />
      <span>{message}</span>
    </div>
  );
}

interface LoadingStateProps {
  label?: string;
  /** Renders inside a card, matching the surrounding panels. */
  inCard?: boolean;
}

/** Full-page/section spinner shown while the backend request is in flight. */
export function LoadingState({ label = 'Loading from the backend\u2026', inCard = false }: LoadingStateProps) {
  const body = (
    <div className="flex flex-col items-center justify-center gap-3 py-14 text-center">
      <Loader2 className="h-7 w-7 animate-spin text-cyan-400" />
      <p className="text-sm text-slate-500">{label}</p>
    </div>
  );
  return inCard ? <div className="card">{body}</div> : body;
}

interface BackendErrorProps {
  message: string;
  onRetry?: () => void;
  retrying?: boolean;
}

/** Shown when the backend could not be reached or answered with an error. */
export function BackendError({ message, onRetry, retrying }: BackendErrorProps) {
  return (
    <div className="card border-red-500/20 bg-red-500/5">
      <div className="flex flex-col items-center gap-3 px-4 py-10 text-center">
        <div className="flex h-12 w-12 items-center justify-center rounded-full border border-red-500/30 bg-red-500/10 text-red-400">
          <AlertCircle className="h-6 w-6" />
        </div>
        <div>
          <h3 className="text-slate-300 font-semibold">Could not reach the backend</h3>
          <p className="mt-1 max-w-md text-sm text-slate-500">{message}</p>
        </div>
        {onRetry && (
          <button type="button" onClick={onRetry} className="btn-secondary" disabled={retrying}>
            <RefreshCw className={`h-4 w-4 ${retrying ? 'animate-spin' : ''}`} />
            {retrying ? 'Retrying\u2026' : 'Retry'}
          </button>
        )}
      </div>
    </div>
  );
}

interface SectionHeaderProps {
  title: string;
  subtitle?: string;
  icon?: ReactNode;
  action?: ReactNode;
}

export function SectionHeader({ title, subtitle, icon, action }: SectionHeaderProps) {
  return (
    <div className="flex items-center justify-between mb-4 flex-wrap gap-3">
      <div className="flex items-center gap-3">
        {icon && (
          <div className="w-10 h-10 rounded-lg bg-cyan-500/10 border border-cyan-500/20 flex items-center justify-center text-cyan-400">
            {icon}
          </div>
        )}
        <div>
          <h2 className="text-lg font-semibold text-slate-100">{title}</h2>
          {subtitle && <p className="text-sm text-slate-500">{subtitle}</p>}
        </div>
      </div>
      {action}
    </div>
  );
}

