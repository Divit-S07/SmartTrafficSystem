import type { EdgeProps } from 'reactflow';
import type { VisualRoad } from './networkGeometry';
import { ROAD_COLORS } from './roadColors';

function pointOn(road: VisualRoad, t: number, offset = 0): { x: number; y: number } {
  const ux = Math.cos(road.angle);
  const uy = Math.sin(road.angle);
  return {
    x: road.sx + ux * road.length * t - uy * offset,
    y: road.sy + uy * road.length * t + ux * offset,
  };
}

function ArrowMarker({
  x,
  y,
  angleDeg,
  color,
}: {
  x: number;
  y: number;
  angleDeg: number;
  color: string;
}) {
  return (
    <g transform={`translate(${x} ${y}) rotate(${angleDeg})`}>
      <polygon
        points="0,-5 11.5,0 0,5"
        fill={color}
        stroke={ROAD_COLORS.arrowOutline}
        strokeWidth={0.9}
        strokeLinejoin="round"
      />
    </g>
  );
}

function TravelTimeLabel({
  x,
  y,
  text,
  congested,
}: {
  x: number;
  y: number;
  text: string;
  congested: boolean;
}) {
  const w = text.length * 5.9 + 16;
  const h = 17;
  return (
    <g
      transform={`translate(${x} ${y})`}
      style={{
        pointerEvents: 'all',
        filter: 'drop-shadow(0 1px 2px rgba(15, 23, 42, 0.35))',
      }}
    >
      <rect
        x={-w / 2}
        y={-h / 2}
        width={w}
        height={h}
        rx={5.5}
        fill={congested ? '#fffbeb' : '#ffffff'}
        stroke={congested ? '#fcd34d' : '#cbd5e1'}
        strokeWidth={1}
      />
      <text
        x={0}
        y={3.5}
        textAnchor="middle"
        fontSize={10}
        fontWeight={700}
        fontFamily="'Inter', system-ui, sans-serif"
        fill={congested ? '#b45309' : '#0f172a'}
      >
        {text}
      </text>
    </g>
  );
}

function ClosedLabel({ x, y }: { x: number; y: number }) {
  const w = 78;
  const h = 18;
  return (
    <g
      transform={`translate(${x} ${y})`}
      style={{
        pointerEvents: 'all',
        filter: 'drop-shadow(0 1px 3px rgba(127, 29, 29, 0.4))',
      }}
    >
      <rect
        x={-w / 2}
        y={-h / 2}
        width={w}
        height={h}
        rx={6}
        fill="#ffffff"
        stroke="#dc2626"
        strokeWidth={1.4}
      />
      <g transform={`translate(${-w / 2 + 12} 0)`}>
        <circle r={5.4} fill="#dc2626" />
        <line x1={-2.2} y1={-2.2} x2={2.2} y2={2.2} stroke="#ffffff" strokeWidth={1.7} strokeLinecap="round" />
        <line x1={2.2} y1={-2.2} x2={-2.2} y2={2.2} stroke="#ffffff" strokeWidth={1.7} strokeLinecap="round" />
      </g>
      <text
        x={-w / 2 + 22}
        y={3.6}
        textAnchor="start"
        fontSize={10}
        fontWeight={800}
        letterSpacing={0.6}
        fontFamily="'Inter', system-ui, sans-serif"
        fill="#b91c1c"
      >
        CLOSED
      </text>
    </g>
  );
}

function BarrierIcon({ x, y }: { x: number; y: number }) {
  return (
    <g transform={`translate(${x} ${y})`}>
      <circle r={9.5} fill="#ffffff" stroke="#dc2626" strokeWidth={2.2} />
      <line x1={-4} y1={-4} x2={4} y2={4} stroke="#dc2626" strokeWidth={2.4} strokeLinecap="round" />
      <line x1={4} y1={-4} x2={-4} y2={4} stroke="#dc2626" strokeWidth={2.4} strokeLinecap="round" />
    </g>
  );
}

export function RoadEdge({ data, selected }: EdgeProps<VisualRoad>) {
  if (!data) return null;
  const road = data;
  const { width, oneWay, closed, travelTime, baseTime } = road;

  const ux = Math.cos(road.angle);
  const uy = Math.sin(road.angle);
  const e = Math.min(road.borderExtend, road.length / 2);
  const borderD =
    e > 0.05
      ? `M ${road.sx - ux * e} ${road.sy - uy * e} L ${road.tx + ux * e} ${road.ty + uy * e}`
      : `M ${road.sx} ${road.sy} L ${road.tx} ${road.ty}`;
  const d = `M ${road.sx} ${road.sy} L ${road.tx} ${road.ty}`;
  const angleDeg = (road.angle * 180) / Math.PI;
  const congested = !closed && travelTime > baseTime;
  const mid = pointOn(road, 0.5, 0);
  const labelPos = pointOn(road, 0.5, -(width / 2 + 11));
  const minT = road.length > 1 ? Math.min(0.5, 16 / road.length) : 0.5;
  const clampT = (t: number) => Math.min(Math.max(t, minT), 1 - minT);
  const forward = pointOn(road, clampT(oneWay ? 0.6 : 0.7), oneWay ? 0 : 3);
  const reverse = pointOn(road, clampT(0.3), -3);
  const timeLabel = `${Number.isInteger(travelTime) ? travelTime : travelTime.toFixed(1)} min`;
  const showTimeLabel = !closed && road.length >= 80;

  return (
    <g>
      <path
        d={d}
        fill="none"
        stroke="transparent"
        strokeWidth={Math.max(width + 22, 30)}
        style={{ pointerEvents: 'stroke' }}
      />
      {selected && (
        <path
          d={d}
          fill="none"
          stroke={ROAD_COLORS.selected}
          strokeWidth={width + 11}
          strokeLinecap="round"
          opacity={0.3}
        />
      )}
      <path
        d={borderD}
        fill="none"
        stroke={closed ? ROAD_COLORS.closedBorder : ROAD_COLORS.border}
        strokeWidth={width + 4}
        strokeLinecap="butt"
      />
      <path
        d={d}
        fill="none"
        stroke={closed ? ROAD_COLORS.closedAsphalt : ROAD_COLORS.asphalt}
        strokeWidth={width}
        strokeLinecap="butt"
        strokeDasharray={closed ? '15 9' : undefined}
      />
      {!closed && (
        <path
          d={d}
          fill="none"
          stroke={oneWay ? 'rgba(96, 165, 250, 0.85)' : ROAD_COLORS.dashNeutral}
          strokeWidth={1.7}
          strokeDasharray="7 10"
          strokeLinecap="butt"
        />
      )}

      {closed && <BarrierIcon x={mid.x} y={mid.y} />}

      {!closed && oneWay && (
        <ArrowMarker x={forward.x} y={forward.y} angleDeg={angleDeg} color={ROAD_COLORS.oneWayAccent} />
      )}
      {!closed && !oneWay && (
        <>
          <ArrowMarker x={reverse.x} y={reverse.y} angleDeg={angleDeg + 180} color={ROAD_COLORS.arrowNeutral} />
          <ArrowMarker x={forward.x} y={forward.y} angleDeg={angleDeg} color={ROAD_COLORS.arrowNeutral} />
        </>
      )}

      {closed && <ClosedLabel x={labelPos.x} y={labelPos.y} />}
      {showTimeLabel && (
        <TravelTimeLabel x={labelPos.x} y={labelPos.y} text={timeLabel} congested={congested} />
      )}
    </g>
  );
}
