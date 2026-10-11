import type { Junction, RoadSegment } from '@/types';

export type Selection =
  | { kind: 'junction'; id: string }
  | { kind: 'road'; from: string; to: string }
  | null;

export interface VisualRoad {
  edgeId: string;
  from: string;
  to: string;
  sx: number;
  sy: number;
  tx: number;
  ty: number;
  borderExtend: number;
  length: number;
  angle: number;
  width: number;
  travelTime: number;
  baseTime: number;
  oneWay: boolean;
  closed: boolean;
}

export const JUNCTION_RADIUS = 15;
export const MIN_JUNCTION_GAP = 140;

export function pairKey(a: string, b: string): string {
  return a < b ? `${a}|${b}` : `${b}|${a}`;
}

export function formatTravelTime(time: number): string {
  return Number.isInteger(time) ? `${time} min` : `${time.toFixed(1)} min`;
}

export function buildVisualRoads(junctions: Junction[], roads: RoadSegment[]): VisualRoad[] {
  const positions = new Map<string, Junction>();
  junctions.forEach((j) => positions.set(j.id, j));

  const groups = new Map<string, RoadSegment[]>();
  for (const road of roads) {
    const key = pairKey(road.from, road.to);
    const group = groups.get(key);
    if (group) group.push(road);
    else groups.set(key, [road]);
  }

  const result: VisualRoad[] = [];
  groups.forEach((segments) => {
    const primary = segments[0];
    const a = positions.get(primary.from);
    const b = positions.get(primary.to);
    if (!a || !b) return;

    const dx = b.x - a.x;
    const dy = b.y - a.y;
    const dist = Math.hypot(dx, dy);
    if (dist < 2) return;

    const ux = dx / dist;
    const uy = dy / dist;
    const oneWay = segments.length === 1;

    const width = oneWay ? 11 : 15;
    const r = JUNCTION_RADIUS;
    const maxTrim = Math.max(0, dist / 2 - 2);
    const asphaltTrim = Math.min(
      Math.sqrt(Math.max(1, r * r - (width / 2) * (width / 2))),
      maxTrim,
    );
    let borderTrim = Math.min(
      Math.sqrt(Math.max(1, r * r - ((width + 4) / 2) * ((width + 4) / 2))),
      maxTrim,
    );
    if (asphaltTrim < borderTrim) borderTrim = asphaltTrim;

    const sx = a.x + ux * asphaltTrim;
    const sy = a.y + uy * asphaltTrim;
    const tx = b.x - ux * asphaltTrim;
    const ty = b.y - uy * asphaltTrim;

    result.push({
      edgeId: `road-${primary.from}-${primary.to}`,
      from: primary.from,
      to: primary.to,
      sx,
      sy,
      tx,
      ty,
      borderExtend: asphaltTrim - borderTrim,
      length: Math.hypot(tx - sx, ty - sy),
      angle: Math.atan2(ty - sy, tx - sx),
      width,
      travelTime: primary.currentTravelTime,
      baseTime: primary.baseTravelTime,
      oneWay,
      closed: segments.some((s) => s.closed),
    });
  });
  return result;
}

export function findVisualRoad(
  visualRoads: VisualRoad[],
  a: string,
  b: string,
): VisualRoad | undefined {
  const key = pairKey(a, b);
  return visualRoads.find((r) => pairKey(r.from, r.to) === key);
}

export function roadsAtJunction(visualRoads: VisualRoad[], junctionId: string): VisualRoad[] {
  return visualRoads.filter((r) => r.from === junctionId || r.to === junctionId);
}

export function findFreeJunctionPosition(junctions: Junction[]): { x: number; y: number } {
  const columns = 5;
  const gapX = 240;
  const gapY = 200;
  const originX = 200;
  const originY = 170;

  for (let i = 0; i < 500; i++) {
    const x = originX + (i % columns) * gapX;
    const y = originY + Math.floor(i / columns) * gapY;
    const free = junctions.every((j) => Math.hypot(j.x - x, j.y - y) >= MIN_JUNCTION_GAP);
    if (free) return { x, y };
  }
  return { x: originX + junctions.length * 40, y: originY + junctions.length * 40 };
}
