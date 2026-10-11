import { ROAD_COLORS } from './roadColors';

type LegendKind = 'junction' | 'one-way' | 'two-way' | 'closed';

function LegendSwatch({ kind }: { kind: LegendKind }) {
  if (kind === 'junction') {
    return (
      <svg width="52" height="18" viewBox="0 0 52 18" aria-hidden className="flex-shrink-0">
        <circle cx="26" cy="9" r="6" fill="#ffffff" stroke="#3b82f6" strokeWidth="1.8" />
      </svg>
    );
  }
  if (kind === 'one-way') {
    return (
      <svg width="52" height="18" viewBox="0 0 52 18" aria-hidden className="flex-shrink-0">
        <line x1="4" y1="9" x2="48" y2="9" stroke={ROAD_COLORS.border} strokeWidth="9" strokeLinecap="round" />
        <line x1="4" y1="9" x2="48" y2="9" stroke={ROAD_COLORS.asphalt} strokeWidth="6" strokeLinecap="round" />
        <polygon points="30,5.5 41,9 30,12.5" fill={ROAD_COLORS.oneWayAccent} />
      </svg>
    );
  }
  if (kind === 'two-way') {
    return (
      <svg width="52" height="18" viewBox="0 0 52 18" aria-hidden className="flex-shrink-0">
        <line x1="4" y1="9" x2="48" y2="9" stroke={ROAD_COLORS.border} strokeWidth="11" strokeLinecap="round" />
        <line x1="4" y1="9" x2="48" y2="9" stroke={ROAD_COLORS.asphalt} strokeWidth="8" strokeLinecap="round" />
        <polygon points="33,5.5 43,9 33,12.5" fill={ROAD_COLORS.arrowNeutral} stroke={ROAD_COLORS.arrowOutline} strokeWidth="0.6" />
        <polygon points="19,5.5 9,9 19,12.5" fill={ROAD_COLORS.arrowNeutral} stroke={ROAD_COLORS.arrowOutline} strokeWidth="0.6" />
      </svg>
    );
  }
  return (
    <svg width="52" height="18" viewBox="0 0 52 18" aria-hidden className="flex-shrink-0">
      <line x1="4" y1="9" x2="48" y2="9" stroke={ROAD_COLORS.closedBorder} strokeWidth="9" strokeLinecap="butt" />
      <line
        x1="4"
        y1="9"
        x2="48"
        y2="9"
        stroke={ROAD_COLORS.closedAsphalt}
        strokeWidth="6"
        strokeDasharray="6 4"
      />
      <circle cx="26" cy="9" r="5.5" fill="#ffffff" stroke="#dc2626" strokeWidth="1.8" />
      <line x1="23.6" y1="6.6" x2="28.4" y2="11.4" stroke="#dc2626" strokeWidth="1.6" strokeLinecap="round" />
      <line x1="28.4" y1="6.6" x2="23.6" y2="11.4" stroke="#dc2626" strokeWidth="1.6" strokeLinecap="round" />
    </svg>
  );
}

const LEGEND_ITEMS: { kind: LegendKind; label: string }[] = [
  { kind: 'junction', label: 'Junction' },
  { kind: 'one-way', label: 'One-way (1 blue arrow)' },
  { kind: 'two-way', label: 'Two-way (2 arrows)' },
  { kind: 'closed', label: 'Closed road' },
];

export function MapLegend() {
  return (
    <div className="absolute bottom-3 left-3 z-20 rounded-lg border border-slate-300 bg-white/95 px-3 py-2 shadow-sm">
      <p className="mb-1.5 text-[10px] font-semibold uppercase tracking-wider text-slate-500">Legend</p>
      <ul className="space-y-1">
        {LEGEND_ITEMS.map((item) => (
          <li key={item.kind} className="flex items-center gap-2 text-[11px] font-medium text-slate-700">
            <LegendSwatch kind={item.kind} />
            <span>{item.label}</span>
          </li>
        ))}
      </ul>
    </div>
  );
}
