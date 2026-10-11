import { useCallback, useState } from 'react';
import type {
  ActivityEntry,
  EmergencyVehicle,
  Junction,
  RoadSegment,
  Route,
  RoutingRequest,
  Severity,
  TrafficEvent,
  EventType,
  VehicleType,
} from '@/types';
import { SEVERITY_RANK } from '@/types';

let idCounter = 0;
const nextId = (prefix: string) => `${prefix}${++idCounter}`;

function applyEventToRoads(allRoads: RoadSegment[], evt: TrafficEvent): RoadSegment[] {
  return allRoads.map((r) => {
    if (r.from !== evt.from || r.to !== evt.to) return r;
    if (evt.type === 'CLOSURE') return { ...r, closed: true };
    if (evt.type === 'REOPEN')
      return { ...r, closed: false, currentTravelTime: r.baseTravelTime };
    if (evt.type === 'CONGESTION' && evt.factor !== null)
      return { ...r, currentTravelTime: r.baseTravelTime * Math.max(1.0, evt.factor) };
    return r;
  });
}

export function useTrafficStore() {
  const [junctions, setJunctions] = useState<Junction[]>([]);
  const [roads, setRoads] = useState<RoadSegment[]>([]);
  const [vehicles, setVehicles] = useState<EmergencyVehicle[]>([]);
  const [requests, setRequests] = useState<RoutingRequest[]>([]);
  const [events, setEvents] = useState<TrafficEvent[]>([]);
  const [activity, setActivity] = useState<ActivityEntry[]>([]);

  const [eventSeq, setEventSeq] = useState(0);
  const [requestSeq, setRequestSeq] = useState(0);
  const [arrivalSeq, setArrivalSeq] = useState(0);

  const logActivity = useCallback(
    (category: ActivityEntry['category'], message: string, details: string | null = null) => {
      setActivity((prev) => [
        { id: nextId('ACT'), timestamp: Date.now(), category, message, details },
        ...prev,
      ]);
    },
    [],
  );

  const addJunction = useCallback(
    (id: string, x: number, y: number) => {
      if (junctions.some((j) => j.id === id)) return false;
      setJunctions((prev) => [...prev, { id, x, y }]);
      logActivity('SYSTEM', `Junction ${id} added to the network.`);
      return true;
    },
    [junctions, logActivity],
  );

  const addRoad = useCallback(
    (from: string, to: string, travelTime: number, oneWay: boolean) => {
      if (from === to) return false;
      if (!junctions.some((j) => j.id === from) || !junctions.some((j) => j.id === to))
        return false;
      if (roads.some((r) => r.from === from && r.to === to)) return false;

      const newSegments: RoadSegment[] = [
        { from, to, baseTravelTime: travelTime, currentTravelTime: travelTime, closed: false, oneWay },
      ];
      if (!oneWay) {
        newSegments.push({
          from: to,
          to: from,
          baseTravelTime: travelTime,
          currentTravelTime: travelTime,
          closed: false,
          oneWay: false,
        });
      }
      setRoads((prev) => [...prev, ...newSegments]);
      logActivity(
        'SYSTEM',
        `Road ${from}\u2192${to}${oneWay ? ' (one-way)' : ' (two-way)'} added with travel time ${travelTime} min.`,
      );
      return true;
    },
    [junctions, roads, logActivity],
  );

  const registerVehicle = useCallback(
    (id: string, type: VehicleType, position: string) => {
      if (vehicles.some((v) => v.id === id)) return false;
      if (!junctions.some((j) => j.id === position)) return false;
      setVehicles((prev) => [
        ...prev,
        {
          id,
          type,
          lastConfirmedJunction: position,
          destination: null,
          status: 'IDLE',
          currentRoute: null,
        },
      ]);
      logActivity('SYSTEM', `Vehicle ${id} (${type}) registered at junction ${position}.`);
      return true;
    },
    [junctions, vehicles, logActivity],
  );

  const findRoute = useCallback(
    (from: string, to: string): Route => {
      if (!junctions.some((j) => j.id === from) || !junctions.some((j) => j.id === to)) {
        return { path: [], totalTravelTime: Infinity, status: 'UNREACHABLE', requestedFrom: from, requestedTo: to };
      }
      if (from === to) {
        return { path: [from], totalTravelTime: 0, status: 'VALID', requestedFrom: from, requestedTo: to };
      }

      const dist = new Map<string, number>();
      const prev = new Map<string, string | null>();
      const settled = new Set<string>();
      const adj = new Map<string, RoadSegment[]>();
      for (const road of roads) {
        if (!adj.has(road.from)) adj.set(road.from, []);
        adj.get(road.from)!.push(road);
      }

      for (const j of junctions) {
        dist.set(j.id, Infinity);
        prev.set(j.id, null);
      }
      dist.set(from, 0);

      type HeapEntry = { node: string; distance: number };
      const heap: HeapEntry[] = [{ node: from, distance: 0 }];

      while (heap.length > 0) {
        heap.sort((a, b) => a.distance - b.distance);
        const entry = heap.shift()!;
        const u = entry.node;
        if (settled.has(u)) continue;
        if (entry.distance > (dist.get(u) ?? Infinity)) continue;
        settled.add(u);
        if (u === to) break;

        const neighbors = adj.get(u) || [];
        for (const seg of neighbors) {
          if (seg.closed) continue;
          const v = seg.to;
          if (settled.has(v)) continue;
          const alt = (dist.get(u) ?? Infinity) + seg.currentTravelTime;
          if (alt < (dist.get(v) ?? Infinity)) {
            dist.set(v, alt);
            prev.set(v, u);
            heap.push({ node: v, distance: alt });
          }
        }
      }

      const targetDist = dist.get(to) ?? Infinity;
      if (targetDist === Infinity || !prev.get(to)) {
        return { path: [], totalTravelTime: Infinity, status: 'UNREACHABLE', requestedFrom: from, requestedTo: to };
      }

      const path: string[] = [];
      let current: string | null = to;
      while (current) {
        path.unshift(current);
        current = prev.get(current) ?? null;
      }
      if (path[0] !== from) {
        return { path: [], totalTravelTime: Infinity, status: 'UNREACHABLE', requestedFrom: from, requestedTo: to };
      }
      return { path, totalTravelTime: targetDist, status: 'VALID', requestedFrom: from, requestedTo: to };
    },
    [junctions, roads],
  );

  const submitRequest = useCallback(
    (vehicleId: string, destination: string, severity: Severity): string | null => {
      if (!vehicles.some((v) => v.id === vehicleId)) return null;
      if (!junctions.some((j) => j.id === destination)) return null;

      const reqId = `REQ${requestSeq + 1}`;
      const arrival = arrivalSeq + 1;
      setRequestSeq((c) => c + 1);
      setArrivalSeq((c) => c + 1);
      setRequests((prev) => [
        ...prev,
        { id: reqId, vehicleId, destination, severity, arrivalSequence: arrival, status: 'PENDING' },
      ]);
      logActivity(
        'ROUTING_DECISION',
        `Routing request ${reqId} submitted: vehicle ${vehicleId} \u2192 ${destination} (${severity}).`,
      );
      return reqId;
    },
    [vehicles, junctions, requestSeq, arrivalSeq, logActivity],
  );

  const escalateRequest = useCallback(
    (requestId: string, newSeverity: Severity) => {
      setRequests((prev) =>
        prev.map((r) =>
          r.id === requestId && r.status === 'PENDING' ? { ...r, severity: newSeverity } : r,
        ),
      );
      logActivity('ROUTING_DECISION', `Request ${requestId} escalated to ${newSeverity}.`);
    },
    [logActivity],
  );

  const recomputeEnRouteVehicles = useCallback(
    (allVehicles: EmergencyVehicle[], allRoads: RoadSegment[]): EmergencyVehicle[] => {
      const adj = new Map<string, RoadSegment[]>();
      for (const road of allRoads) {
        if (!adj.has(road.from)) adj.set(road.from, []);
        adj.get(road.from)!.push(road);
      }

      return allVehicles.map((v) => {
        if ((v.status === 'EN_ROUTE' || v.status === 'UNREACHABLE') && v.destination) {
          const dist = new Map<string, number>();
          const prev = new Map<string, string | null>();
          const settled = new Set<string>();
          for (const j of junctions) {
            dist.set(j.id, Infinity);
            prev.set(j.id, null);
          }
          dist.set(v.lastConfirmedJunction, 0);
          type HE = { node: string; distance: number };
          const heap: HE[] = [{ node: v.lastConfirmedJunction, distance: 0 }];
          while (heap.length > 0) {
            heap.sort((a, b) => a.distance - b.distance);
            const e = heap.shift()!;
            if (settled.has(e.node)) continue;
            if (e.distance > (dist.get(e.node) ?? Infinity)) continue;
            settled.add(e.node);
            if (e.node === v.destination) break;
            for (const seg of adj.get(e.node) || []) {
              if (seg.closed) continue;
              if (settled.has(seg.to)) continue;
              const alt = (dist.get(e.node) ?? Infinity) + seg.currentTravelTime;
              if (alt < (dist.get(seg.to) ?? Infinity)) {
                dist.set(seg.to, alt);
                prev.set(seg.to, e.node);
                heap.push({ node: seg.to, distance: alt });
              }
            }
          }
          const d = dist.get(v.destination) ?? Infinity;
          if (d === Infinity || !prev.get(v.destination)) {
            return { ...v, currentRoute: { path: [], totalTravelTime: Infinity, status: 'UNREACHABLE', requestedFrom: v.lastConfirmedJunction, requestedTo: v.destination }, status: 'UNREACHABLE' };
          }
          const path: string[] = [];
          let cur: string | null = v.destination;
          while (cur) { path.unshift(cur); cur = prev.get(cur) ?? null; }
          const newStatus = v.lastConfirmedJunction === v.destination ? 'ARRIVED' : 'EN_ROUTE';
          return { ...v, currentRoute: { path, totalTravelTime: d, status: 'VALID', requestedFrom: v.lastConfirmedJunction, requestedTo: v.destination }, status: newStatus as EmergencyVehicle['status'] };
        }
        return v;
      });
    },
    [junctions],
  );

  const processNextRequest = useCallback(() => {
    const pending = requests
      .filter((r) => r.status === 'PENDING')
      .sort((a, b) => {
        const rankDiff = SEVERITY_RANK[b.severity] - SEVERITY_RANK[a.severity];
        if (rankDiff !== 0) return rankDiff;
        return a.arrivalSequence - b.arrivalSequence;
      });
    if (pending.length === 0) return null;

    const req = pending[0];
    const vehicle = vehicles.find((v) => v.id === req.vehicleId);
    if (!vehicle) return null;

    const route = findRoute(vehicle.lastConfirmedJunction, req.destination);
    const newStatus =
      route.status === 'UNREACHABLE'
        ? 'UNREACHABLE'
        : vehicle.lastConfirmedJunction === req.destination
        ? 'ARRIVED'
        : 'EN_ROUTE';

    setVehicles((prev) =>
      prev.map((v) =>
        v.id === vehicle.id
          ? { ...v, destination: req.destination, status: newStatus as EmergencyVehicle['status'], currentRoute: route }
          : v,
      ),
    );
    setRequests((prev) => prev.map((r) => (r.id === req.id ? { ...r, status: 'PROCESSED' } : r)));

    logActivity(
      'ROUTING_DECISION',
      `Request ${req.id} processed: ${vehicle.id} routed to ${req.destination}. Route ${route.status === 'VALID' ? `found (${route.path.join(' \u2192 ')}, ${route.totalTravelTime} min)` : 'UNREACHABLE'}.`,
      route.status === 'VALID' ? `Path: ${route.path.join(' \u2192 ')} | Time: ${route.totalTravelTime} min` : 'No valid path found',
    );
    return req.id;
  }, [requests, vehicles, findRoute, logActivity]);

  const advanceVehicle = useCallback(
    (vehicleId: string, toJunction: string) => {
      const vehicle = vehicles.find((v) => v.id === vehicleId);
      if (!vehicle) return false;
      const road = roads.find((r) => r.from === vehicle.lastConfirmedJunction && r.to === toJunction);
      if (!road || road.closed) return false;

      const arrived = vehicle.destination === toJunction;
      let newRoute = vehicle.currentRoute;
      let newStatus: EmergencyVehicle['status'] = arrived ? 'ARRIVED' : 'EN_ROUTE';

      if (!arrived && vehicle.destination) {
        const route = findRoute(toJunction, vehicle.destination);
        newRoute = route;
        newStatus = route.status === 'UNREACHABLE' ? 'UNREACHABLE' : 'EN_ROUTE';
      }

      setVehicles((prev) =>
        prev.map((v) =>
          v.id === vehicleId
            ? { ...v, lastConfirmedJunction: toJunction, status: newStatus, currentRoute: newRoute }
            : v,
        ),
      );
      logActivity(
        'VEHICLE_MOVEMENT',
        `Vehicle ${vehicleId} advanced ${vehicle.lastConfirmedJunction} \u2192 ${toJunction}.${arrived ? ' Arrived at destination.' : ''}`,
      );
      return true;
    },
    [vehicles, roads, findRoute, logActivity],
  );

  const queueTrafficEvent = useCallback(
    (type: EventType, from: string, to: string, reopening: boolean, factor: number | null) => {
      const seq = eventSeq + 1;
      setEventSeq((c) => c + 1);
      let description = '';
      if (type === 'CLOSURE') description = `Close road ${from}\u2192${to}`;
      else if (type === 'REOPEN') description = `Reopen road ${from}\u2192${to}`;
      else if (type === 'CONGESTION') description = `Congestion on ${from}\u2192${to} (\u00d7${factor})`;

      setEvents((prev) => [
        ...prev,
        { id: nextId('EVT'), sequence: seq, type, from, to, reopening, factor, description, status: 'PENDING' },
      ]);
      logActivity('TRAFFIC_UPDATE', `Traffic event queued: ${description}.`);
    },
    [eventSeq, logActivity],
  );

  const processNextEvent = useCallback(() => {
    const pending = events.filter((e) => e.status === 'PENDING').sort((a, b) => a.sequence - b.sequence);
    if (pending.length === 0) return null;
    const evt = pending[0];

    setRoads((prevRoads) => {
      const updated = applyEventToRoads(prevRoads, evt);
      setVehicles((prevVehicles) => recomputeEnRouteVehicles(prevVehicles, updated));
      return updated;
    });
    setEvents((prev) => prev.map((e) => (e.id === evt.id ? { ...e, status: 'PROCESSED' } : e)));
    logActivity('TRAFFIC_UPDATE', `Traffic event processed: ${evt.description}. All en-route vehicles re-evaluated.`);
    return evt.id;
  }, [events, recomputeEnRouteVehicles, logActivity]);

  const processAllEvents = useCallback(() => {
    const pending = events.filter((e) => e.status === 'PENDING').sort((a, b) => a.sequence - b.sequence);
    if (pending.length === 0) return 0;

    setRoads((prevRoads) => {
      let updated = prevRoads;
      for (const evt of pending) updated = applyEventToRoads(updated, evt);
      setVehicles((prevVehicles) => recomputeEnRouteVehicles(prevVehicles, updated));
      return updated;
    });
    setEvents((prev) => prev.map((e) => (e.status === 'PENDING' ? { ...e, status: 'PROCESSED' } : e)));
    logActivity('TRAFFIC_UPDATE', `All ${pending.length} pending traffic events processed. Network state updated.`);
    return pending.length;
  }, [events, recomputeEnRouteVehicles, logActivity]);

  return {
    junctions,
    roads,
    vehicles,
    requests,
    events,
    activity,
    addJunction,
    addRoad,
    registerVehicle,
    submitRequest,
    escalateRequest,
    processNextRequest,
    advanceVehicle,
    queueTrafficEvent,
    processNextEvent,
    processAllEvents,
    findRoute,
  };
}

export type TrafficStore = ReturnType<typeof useTrafficStore>;
