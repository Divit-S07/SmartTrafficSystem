import { useCallback, useEffect, useRef, useState } from 'react';
import type {
  ActivityEntry,
  EmergencyVehicle,
  Junction,
  RoadSegment,
  RoadUpdate,
  Route,
  RoutingRequest,
  Severity,
  TrafficEvent,
  EventType,
  VehicleType,
} from '@/types';
import { SEVERITY_RANK } from '@/types';
import { apiErrorMessage } from '@/api/client';
import {
  events as eventsApi,
  junctions as junctionsApi,
  roads as roadsApi,
  routes as routesApi,
  vehicles as vehiclesApi,
} from '@/api/routes';
import type { EventPayload, JunctionPayload, RoadPayload, RouteResponse, VehiclePayload } from '@/api/routes';

let idCounter = 0;
const nextId = (prefix: string) => `${prefix}${++idCounter}`;

// ---- payload → view-model mapping ----------------------------------------

/** Backend junction → UI junction (identical shape). */
const toJunction = (payload: JunctionPayload): Junction => ({
  id: payload.id,
  x: payload.x,
  y: payload.y,
});

/** Backend directed segment → UI road segment; `id` is kept for API updates. */
const toRoadSegment = (payload: RoadPayload): RoadSegment => ({
  id: payload.id,
  from: payload.from,
  to: payload.to,
  baseTravelTime: payload.baseTravelTime,
  currentTravelTime: payload.currentTravelTime,
  closed: payload.closed,
  oneWay: payload.oneWay,
});

/**
 * Backend vehicle → UI vehicle. The backend owns identity, type and position;
 * `destination`/`currentRoute` are derived client-side from routing requests,
 * which have no backend resource, so they are carried over when present.
 */
const toEmergencyVehicle = (
  payload: VehiclePayload,
  previous?: EmergencyVehicle,
): EmergencyVehicle => ({
  id: payload.id,
  type: payload.type,
  lastConfirmedJunction: payload.lastConfirmedJunction,
  destination: payload.destination ?? previous?.destination ?? null,
  status: payload.status,
  priority: payload.priority,
  currentRoute: previous?.currentRoute ?? null,
});

const toEvent = (payload: EventPayload): TrafficEvent => ({
  id: payload.id,
  sequence: payload.sequence,
  type: payload.type,
  from: payload.from,
  to: payload.to,
  reopening: payload.reopening,
  factor: payload.factor,
  description: payload.description,
  status: payload.status,
});

const toRoute = (payload: RouteResponse): Route => ({
  path: payload.path,
  totalTravelTime: payload.totalTravelTime ?? Infinity,
  status: payload.status,
  requestedFrom: payload.requestedFrom,
  requestedTo: payload.requestedTo,
});

/** Human-readable description matching the backend's event wording. */
function describeEvent(type: EventType, from: string, to: string, factor: number | null): string {
  const arrow = `${from} \u2192 ${to}`;
  switch (type) {
    case 'CLOSURE':
      return `Close road ${arrow}`;
    case 'REOPEN':
      return `Reopen road ${arrow}`;
    case 'CONGESTION':
      return `Congestion on ${arrow} (\u00d7${factor})`;
  }
}

export function useTrafficStore() {
  const [junctions, setJunctions] = useState<Junction[]>([]);
  const [roads, setRoads] = useState<RoadSegment[]>([]);
  const [vehicles, setVehicles] = useState<EmergencyVehicle[]>([]);
  const [requests, setRequests] = useState<RoutingRequest[]>([]);
  const [events, setEvents] = useState<TrafficEvent[]>([]);
  const [activity, setActivity] = useState<ActivityEntry[]>([]);

  /** Initial load of everything persisted in MongoDB. */
  const [loading, setLoading] = useState(true);
  const [loaded, setLoaded] = useState(false);
  const [loadError, setLoadError] = useState<string | null>(null);

  const [requestSeq, setRequestSeq] = useState(0);
  const [arrivalSeq, setArrivalSeq] = useState(0);

  const loadedRef = useRef(false);

  const logActivity = useCallback(
    (category: ActivityEntry['category'], message: string, details: string | null = null) => {
      setActivity((prev) => [
        { id: nextId('ACT'), timestamp: Date.now(), category, message, details },
        ...prev,
      ]);
    },
    [],
  );

  /**
   * Loads junctions, roads, vehicles and events from the backend in one go.
   * The road segments returned here are exactly what the React Flow map
   * renders, so a successful load (or reload) always rebuilds the network.
   */
  const reload = useCallback(
    async (options: { silent?: boolean } = {}) => {
      const { silent = false } = options;
      if (!silent) setLoading(true);
      try {
        const [junctionPayloads, roadPayloads, vehiclePayloads, eventPayloads] = await Promise.all([
          junctionsApi.list(),
          roadsApi.list(),
          vehiclesApi.list(),
          eventsApi.list(),
        ]);

        const nextJunctions = junctionPayloads.map(toJunction);
        const nextRoads = roadPayloads.map(toRoadSegment);

        setJunctions(nextJunctions);
        setRoads(nextRoads);
        setEvents(eventPayloads.map(toEvent));
        setVehicles((prev) => {
          const byId = new Map(prev.map((v) => [v.id, v]));
          return vehiclePayloads.map((p) => toEmergencyVehicle(p, byId.get(p.id)));
        });

        setLoadError(null);
        if (!loadedRef.current) {
          loadedRef.current = true;
          logActivity(
            'SYSTEM',
            `Loaded ${nextJunctions.length} junction${nextJunctions.length === 1 ? '' : 's'}, ` +
              `${nextRoads.length} road${nextRoads.length === 1 ? '' : 's'}, ` +
              `${vehiclePayloads.length} vehicle${vehiclePayloads.length === 1 ? '' : 's'} and ` +
              `${eventPayloads.length} traffic event${eventPayloads.length === 1 ? '' : 's'} from the backend.`,
          );
        }
        setLoaded(true);
        return true;
      } catch (err) {
        const message = apiErrorMessage(err);
        setLoadError(message);
        logActivity('SYSTEM', `Failed to load data from the backend: ${message}`);
        return false;
      } finally {
        setLoading(false);
      }
    },
    [logActivity],
  );

  useEffect(() => {
    void reload();
  }, [reload]);

  /** True once the backend answered and nothing at all has been created yet. */
  const isEmptyDataset =
    loaded && junctions.length === 0 && roads.length === 0 && vehicles.length === 0 && events.length === 0;

  const findRoad = useCallback(
    (from: string, to: string) => roads.find((r) => r.from === from && r.to === to),
    [roads],
  );

  // ---- route calculation (backend Dijkstra) -------------------------------

  /**
   * Delegates to POST /api/routes/calculate, which runs Dijkstra over the
   * persisted network. Unreachable results come back as a normal UNREACHABLE
   * route; unknown junctions (4xx) are folded into the same shape so callers
   * never have to catch.
   */
  const findRoute = useCallback(
    async (from: string, to: string): Promise<Route> => {
      const unreachable: Route = {
        path: [],
        totalTravelTime: Infinity,
        status: 'UNREACHABLE',
        requestedFrom: from,
        requestedTo: to,
      };
      if (from === to) {
        return { path: [from], totalTravelTime: 0, status: 'VALID', requestedFrom: from, requestedTo: to };
      }
      if (!junctions.some((j) => j.id === from) || !junctions.some((j) => j.id === to)) {
        return unreachable;
      }
      try {
        return toRoute(await routesApi.calculate({ source: from, destination: to }));
      } catch {
        return unreachable;
      }
    },
    [junctions],
  );

  /**
   * Recomputes the displayed route of every en-route/unreachable vehicle after
   * the network changed. Uses the routing API so the map and the preview agree.
   */
  const recomputeEnRouteRoutes = useCallback(
    async (allVehicles: EmergencyVehicle[]): Promise<EmergencyVehicle[]> => {
      const active = allVehicles.filter(
        (v) => (v.status === 'EN_ROUTE' || v.status === 'UNREACHABLE') && v.destination,
      );
      if (active.length === 0) return allVehicles;

      const recomputed = new Map<string, Route>();
      await Promise.all(
        active.map(async (v) => {
          recomputed.set(v.id, await findRoute(v.lastConfirmedJunction, v.destination as string));
        }),
      );

      return allVehicles.map((v) => {
        const route = recomputed.get(v.id);
        if (!route) return v;
        const status: EmergencyVehicle['status'] =
          route.status === 'UNREACHABLE'
            ? 'UNREACHABLE'
            : v.lastConfirmedJunction === v.destination
              ? 'ARRIVED'
              : 'EN_ROUTE';
        return { ...v, currentRoute: route, status };
      });
    },
    [findRoute],
  );

  // ---- junctions ----------------------------------------------------------

  /**
   * Adds a junction at an automatic free slot. Returns `ok`, plus the
   * backend's message when it failed so the modal can show it.
   */
  const addJunction = useCallback(
    async (id: string, x: number, y: number): Promise<{ ok: boolean; error?: string }> => {
      if (junctions.some((j) => j.id === id)) {
        return { ok: false, error: `Junction ${id} already exists.` };
      }
      try {
        const created = await junctionsApi.create({ id, x, y });
        setJunctions((prev) => [...prev.filter((j) => j.id !== id), toJunction(created)]);
        logActivity('SYSTEM', `Junction ${id} added to the network.`);
        return { ok: true };
      } catch (err) {
        const message = apiErrorMessage(err);
        logActivity('SYSTEM', `Failed to add junction ${id}: ${message}`);
        return { ok: false, error: message };
      }
    },
    [junctions, logActivity],
  );

  /** Moves a junction on the canvas without hitting the network (drag frames). */
  const moveJunction = useCallback((id: string, x: number, y: number) => {
    setJunctions((prev) => prev.map((j) => (j.id === id ? { ...j, x, y } : j)));
  }, []);

  /** Persists a dragged junction position once the drag has finished. */
  const updateJunctionPosition = useCallback(
    async (id: string, x: number, y: number): Promise<boolean> => {
      try {
        const updated = await junctionsApi.updatePosition(id, { x, y });
        setJunctions((prev) => prev.map((j) => (j.id === id ? toJunction(updated) : j)));
        return true;
      } catch (err) {
        const message = apiErrorMessage(err);
        logActivity('SYSTEM', `Failed to save position of junction ${id}: ${message}`);
        // Roll the canvas back to the persisted coordinates.
        void reload({ silent: true });
        return false;
      }
    },
    [logActivity, reload],
  );

  // ---- roads ---------------------------------------------------------------

  /**
   * Creates a road. A two-way road is stored as two directed segments by a
   * single backend call, so the UI state is refreshed from the server rather
   * than assembled locally. `closed` is applied through the status endpoint
   * because creation always starts open.
   */
  const addRoad = useCallback(
    async (
      from: string,
      to: string,
      travelTime: number,
      oneWay: boolean,
      closed = false,
    ): Promise<{ ok: boolean; error?: string }> => {
      if (from === to) {
        return { ok: false, error: 'Source and destination must be different.' };
      }
      if (!junctions.some((j) => j.id === from) || !junctions.some((j) => j.id === to)) {
        return { ok: false, error: 'One or both junctions do not exist.' };
      }
      try {
        const created = await roadsApi.create({ from, to, travelTime, oneWay });
        if (closed) {
          await roadsApi.patchStatus(created.id, { closed: true });
        }
        const updated = await roadsApi.list();
        setRoads(updated.map(toRoadSegment));
        logActivity(
          'SYSTEM',
          `Road ${from}\u2192${to} (${oneWay ? 'one-way' : 'two-way'}) added with travel time ${travelTime} min` +
            `${closed ? ' \u2014 closed' : ''}.`,
        );
        return { ok: true };
      } catch (err) {
        const message = apiErrorMessage(err);
        logActivity('SYSTEM', `Failed to add road ${from}\u2192${to}: ${message}`);
        return { ok: false, error: message };
      }
    },
    [junctions, logActivity],
  );

  /**
   * Updates travel time / direction / closed state of a road pair.
   *
   * A status-only change uses PATCH /status; anything else goes through PUT,
   * which updates the two-way pair atomically server-side.
   */
  const updateRoad = useCallback(
    async (from: string, to: string, changes: RoadUpdate): Promise<{ ok: boolean; error?: string }> => {
      const primary = findRoad(from, to);
      if (!primary?.id) {
        return { ok: false, error: `Road ${from}\u2192${to} was not found on the backend.` };
      }

      const statusOnly =
        changes.closed !== undefined && changes.travelTime === undefined && changes.oneWay === undefined;

      // PUT always wants the full triple, so unchanged fields are echoed back.
      const travelTime = changes.travelTime ?? primary.baseTravelTime;
      const oneWay = changes.oneWay ?? primary.oneWay;
      const closed = changes.closed ?? primary.closed;

      const optimistic: RoadSegment[] = roads.map((r) => {
        const inGroup = (r.from === from && r.to === to) || (r.from === to && r.to === from);
        if (!inGroup) return r;
        if (r.from === from && r.to === to) {
          return { ...r, baseTravelTime: travelTime, currentTravelTime: travelTime, oneWay, closed };
        }
        return { ...r, baseTravelTime: travelTime, currentTravelTime: travelTime, closed };
      });
      setRoads(optimistic);

      try {
        if (statusOnly) {
          await roadsApi.patchStatus(primary.id, { closed });
        } else {
          await roadsApi.update(primary.id, { travelTime, oneWay, closed });
        }
        // Pull the authoritative pair back so the map and the lists agree.
        const updated = await roadsApi.list();
        setRoads(updated.map(toRoadSegment));

        const notes: string[] = [];
        if (changes.travelTime !== undefined) notes.push(`travel time set to ${travelTime} min`);
        if (changes.oneWay !== undefined) notes.push(`changed to ${oneWay ? 'one-way' : 'two-way'}`);
        if (changes.closed !== undefined) notes.push(closed ? 'closed' : 'reopened');
        logActivity(
          statusOnly ? 'TRAFFIC_UPDATE' : 'SYSTEM',
          `Road ${from}\u2192${to} ${notes.join(', ')}.`,
        );
        return { ok: true };
      } catch (err) {
        const message = apiErrorMessage(err);
        logActivity('SYSTEM', `Failed to update road ${from}\u2192${to}: ${message}`);
        await reload({ silent: true });
        return { ok: false, error: message };
      }
    },
    [findRoad, roads, logActivity, reload],
  );

  // ---- vehicles ------------------------------------------------------------

  const registerVehicle = useCallback(
    async (
      id: string,
      type: VehicleType,
      position: string,
    ): Promise<{ ok: boolean; error?: string }> => {
      if (vehicles.some((v) => v.id === id)) {
        return { ok: false, error: `Vehicle ${id} already exists.` };
      }
      if (!junctions.some((j) => j.id === position)) {
        return { ok: false, error: `Junction ${position} does not exist.` };
      }
      try {
        const created = await vehiclesApi.register({ id, type, junction: position });
        setVehicles((prev) => [...prev.filter((v) => v.id !== id), toEmergencyVehicle(created)]);
        logActivity('SYSTEM', `Vehicle ${id} (${type}) registered at junction ${position}.`);
        return { ok: true };
      } catch (err) {
        const message = apiErrorMessage(err);
        logActivity('SYSTEM', `Failed to register vehicle ${id}: ${message}`);
        return { ok: false, error: message };
      }
    },
    [junctions, vehicles, logActivity],
  );

  /**
   * Moves a vehicle to an adjacent junction. The backend validates adjacency
   * and that the road is open, then the remaining route to the destination is
   * recalculated through the routing API.
   */
  const advanceVehicle = useCallback(
    async (vehicleId: string, toJunction: string): Promise<boolean> => {
      const vehicle = vehicles.find((v) => v.id === vehicleId);
      if (!vehicle) return false;

      try {
        const updated = await vehiclesApi.updateJunction(vehicleId, toJunction);
        const arrived = vehicle.destination === toJunction;

        let currentRoute = vehicle.currentRoute;
        let status = updated.status as EmergencyVehicle['status'];
        if (vehicle.destination && !arrived) {
          currentRoute = await findRoute(toJunction, vehicle.destination);
          status = currentRoute.status === 'UNREACHABLE' ? 'UNREACHABLE' : 'EN_ROUTE';
        } else if (arrived) {
          status = 'ARRIVED';
        }

        setVehicles((prev) =>
          prev.map((v) => (v.id === vehicleId ? { ...toEmergencyVehicle(updated, v), status, currentRoute } : v)),
        );

        logActivity(
          'VEHICLE_MOVEMENT',
          `Vehicle ${vehicleId} advanced ${vehicle.lastConfirmedJunction} \u2192 ${toJunction}.` +
            (arrived ? ' Arrived at destination.' : ''),
        );
        return true;
      } catch (err) {
        const message = apiErrorMessage(err);
        logActivity('VEHICLE_MOVEMENT', `Failed to advance vehicle ${vehicleId}: ${message}`);
        return false;
      }
    },
    [vehicles, findRoute, logActivity],
  );

  // ---- routing requests (client-side queue) -------------------------------

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
        prev.map((r) => (r.id === requestId && r.status === 'PENDING' ? { ...r, severity: newSeverity } : r)),
      );
      logActivity('ROUTING_DECISION', `Request ${requestId} escalated to ${newSeverity}.`);
    },
    [logActivity],
  );

  /** Picks the highest-severity pending request and routes it via the API. */
  const processNextRequest = useCallback(async (): Promise<string | null> => {
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

    try {
      const route = await findRoute(vehicle.lastConfirmedJunction, req.destination);
      const newStatus: EmergencyVehicle['status'] =
        route.status === 'UNREACHABLE'
          ? 'UNREACHABLE'
          : vehicle.lastConfirmedJunction === req.destination
            ? 'ARRIVED'
            : 'EN_ROUTE';

      setVehicles((prev) =>
        prev.map((v) =>
          v.id === vehicle.id ? { ...v, destination: req.destination, status: newStatus, currentRoute: route } : v,
        ),
      );
      setRequests((prev) => prev.map((r) => (r.id === req.id ? { ...r, status: 'PROCESSED' } : r)));

      logActivity(
        'ROUTING_DECISION',
        `Request ${req.id} processed: ${vehicle.id} routed to ${req.destination}. ` +
          (route.status === 'VALID'
            ? `Route found (${route.path.join(' \u2192 ')}, ${route.totalTravelTime} min).`
            : 'Route UNREACHABLE.'),
        route.status === 'VALID'
          ? `Path: ${route.path.join(' \u2192 ')} | Time: ${route.totalTravelTime} min`
          : 'No valid path found',
      );
      return req.id;
    } catch (err) {
      const message = apiErrorMessage(err);
      logActivity('ROUTING_DECISION', `Failed to process request ${req.id}: ${message}`);
      return null;
    }
  }, [requests, vehicles, findRoute, logActivity]);

  // ---- traffic events (API-backed) ----------------------------------------

  const queueTrafficEvent = useCallback(
    async (
      type: EventType,
      from: string,
      to: string,
      factor: number | null,
    ): Promise<{ ok: boolean; error?: string }> => {
      if (from === to) {
        return { ok: false, error: 'Source and destination must be different.' };
      }
      if (type === 'CONGESTION' && (factor === null || factor < 1.0)) {
        return { ok: false, error: 'Congestion factor must be a number \u2265 1.0.' };
      }
      if (!roads.some((r) => r.from === from && r.to === to)) {
        return { ok: false, error: `Road ${from}\u2192${to} does not exist in the network.` };
      }
      try {
        const created = await eventsApi.create({
          type,
          from,
          to,
          ...(type === 'CONGESTION' && factor !== null ? { factor } : {}),
        });
        setEvents((prev) => [...prev, toEvent(created)]);
        logActivity('TRAFFIC_UPDATE', `Traffic event queued: ${describeEvent(type, from, to, factor)}.`);
        return { ok: true };
      } catch (err) {
        const message = apiErrorMessage(err);
        logActivity('TRAFFIC_UPDATE', `Failed to queue traffic event: ${message}`);
        return { ok: false, error: message };
      }
    },
    [roads, logActivity],
  );

  /**
   * Applies the given events server-side (PATCH → PROCESSED), then reloads the
   * roads so the React Flow network shows the authoritative post-event state.
   */
  const processEvents = useCallback(
    async (eventIds: string[]): Promise<{ processed: number; error?: string }> => {
      let processed = 0;
      let firstError: string | undefined;

      for (const id of eventIds) {
        try {
          const processedEvent = await eventsApi.patchStatus(id, 'PROCESSED');
          setEvents((prev) => prev.map((e) => (e.id === id ? toEvent(processedEvent) : e)));
          processed++;
        } catch (err) {
          const message = apiErrorMessage(err);
          firstError = firstError ?? message;
          logActivity('TRAFFIC_UPDATE', `Failed to process traffic event ${id}: ${message}`);
        }
      }

      if (processed > 0) {
        // The backend already mutated the roads — pull them back so closures,
        // reopens and congestion travel times on the map are the real values.
        try {
          const refreshedRoads = await roadsApi.list();
          setRoads(refreshedRoads.map(toRoadSegment));
        } catch (err) {
          logActivity('TRAFFIC_UPDATE', `Failed to refresh roads: ${apiErrorMessage(err)}`);
        }
        setVehicles(await recomputeEnRouteRoutes(vehicles));
      }

      return { processed, error: firstError };
    },
    [vehicles, recomputeEnRouteRoutes, logActivity],
  );

  const processNextEvent = useCallback(async (): Promise<string | null> => {
    const pending = events.filter((e) => e.status === 'PENDING').sort((a, b) => a.sequence - b.sequence);
    if (pending.length === 0) return null;
    const evt = pending[0];

    const { processed } = await processEvents([evt.id]);
    if (processed === 0) return null;

    logActivity(
      'TRAFFIC_UPDATE',
      `Traffic event processed: ${evt.description}. Roads refreshed and en-route vehicles re-evaluated.`,
    );
    return evt.id;
  }, [events, processEvents, logActivity]);

  const processAllEvents = useCallback(async (): Promise<number> => {
    const pending = events.filter((e) => e.status === 'PENDING').sort((a, b) => a.sequence - b.sequence);
    if (pending.length === 0) return 0;

    const { processed } = await processEvents(pending.map((e) => e.id));
    logActivity(
      'TRAFFIC_UPDATE',
      `All ${processed} pending traffic event${processed === 1 ? '' : 's'} processed. ` +
        'Road network state refreshed from the backend.',
    );
    return processed;
  }, [events, processEvents, logActivity]);

  return {
    junctions,
    roads,
    vehicles,
    requests,
    events,
    activity,
    loading,
    loaded,
    loadError,
    isEmptyDataset,
    reload,
    addJunction,
    addRoad,
    moveJunction,
    updateJunctionPosition,
    updateRoad,
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
