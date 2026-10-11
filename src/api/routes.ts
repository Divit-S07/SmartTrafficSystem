import { api, apiErrorMessage } from './client';

export { apiErrorMessage };

// ---- junctions -----------------------------------------------------------

export interface JunctionPayload {
  id: string;
  x: number;
  y: number;
}

export type JunctionCreatePayload = JunctionPayload;

export type JunctionPositionPayload = {
  x: number;
  y: number;
};

export const junctions = {
  list: (): Promise<JunctionPayload[]> => api.get<JunctionPayload[]>('/api/junctions'),

  create: (payload: JunctionCreatePayload): Promise<JunctionPayload> =>
    api.post<JunctionPayload>('/api/junctions', payload),

  get: (id: string): Promise<JunctionPayload> =>
    api.get<JunctionPayload>(`/api/junctions/${encodeURIComponent(id)}`),

  /** Persist a dragged junction position on the map. */
  updatePosition: (id: string, payload: JunctionPositionPayload): Promise<JunctionPayload> =>
    api.put<JunctionPayload>(`/api/junctions/${encodeURIComponent(id)}`, payload),

  delete: (id: string): Promise<void> => api.delete(`/api/junctions/${encodeURIComponent(id)}`),
};

// ---- roads ---------------------------------------------------------------

export interface RoadPayload {
  id: string;
  from: string;
  to: string;
  baseTravelTime: number;
  currentTravelTime: number;
  oneWay: boolean;
  closed: boolean;
}

export type RoadCreatePayload = {
  from: string;
  to: string;
  travelTime: number;
  oneWay: boolean;
};

export type RoadUpdatePayload = {
  travelTime: number;
  oneWay: boolean;
  closed: boolean;
};

export type RoadStatusPayload = {
  closed: boolean;
};

export const roads = {
  list: (): Promise<RoadPayload[]> => api.get<RoadPayload[]>('/api/roads'),

  create: (payload: RoadCreatePayload): Promise<RoadPayload> =>
    api.post<RoadPayload>('/api/roads', payload),

  update: (id: string, payload: RoadUpdatePayload): Promise<RoadPayload> =>
    api.put<RoadPayload>(`/api/roads/${encodeURIComponent(id)}`, payload),

  /** Close or reopen a road; reopening restores the base travel time. */
  patchStatus: (id: string, payload: RoadStatusPayload): Promise<RoadPayload> =>
    api.patch<RoadPayload>(`/api/roads/${encodeURIComponent(id)}/status`, payload),

  delete: (id: string): Promise<void> => api.delete(`/api/roads/${encodeURIComponent(id)}`),
};

// ---- vehicles ------------------------------------------------------------

export type VehicleType = 'AMBULANCE' | 'FIRE_ENGINE' | 'RESCUE';
export type Severity = 'CRITICAL' | 'HIGH' | 'NORMAL';
export type VehicleStatus = 'IDLE' | 'EN_ROUTE' | 'ARRIVED' | 'UNREACHABLE';

export interface VehiclePayload {
  id: string;
  type: VehicleType;
  lastConfirmedJunction: string;
  destination: string | null;
  status: VehicleStatus;
  priority: Severity;
}

export interface VehicleCreatePayload {
  id: string;
  type: VehicleType;
  junction: string;
  priority?: Severity;
}

export const vehicles = {
  list: (): Promise<VehiclePayload[]> => api.get<VehiclePayload[]>('/api/vehicles'),

  register: (payload: VehicleCreatePayload): Promise<VehiclePayload> =>
    api.post<VehiclePayload>('/api/vehicles', payload),

  get: (id: string): Promise<VehiclePayload> =>
    api.get<VehiclePayload>(`/api/vehicles/${encodeURIComponent(id)}`),

  /** Move a vehicle to an adjacent junction (validated server-side). */
  updateJunction: (id: string, junction: string): Promise<VehiclePayload> =>
    api.put<VehiclePayload>(`/api/vehicles/${encodeURIComponent(id)}/junction`, { junction }),

  updatePriority: (id: string, priority: Severity): Promise<VehiclePayload> =>
    api.put<VehiclePayload>(`/api/vehicles/${encodeURIComponent(id)}/priority`, { priority }),
};

// ---- events --------------------------------------------------------------

export type EventType = 'CLOSURE' | 'REOPEN' | 'CONGESTION';
export type EventStatus = 'PENDING' | 'PROCESSED';

export interface EventPayload {
  id: string;
  sequence: number;
  type: EventType;
  from: string;
  to: string;
  reopening: boolean;
  factor: number | null;
  description: string;
  status: EventStatus;
}

export interface EventCreatePayload {
  type: EventType;
  from: string;
  to: string;
  factor?: number;
}

export const events = {
  list: (): Promise<EventPayload[]> => api.get<EventPayload[]>('/api/events'),

  create: (payload: EventCreatePayload): Promise<EventPayload> =>
    api.post<EventPayload>('/api/events', payload),

  get: (id: string): Promise<EventPayload> =>
    api.get<EventPayload>(`/api/events/${encodeURIComponent(id)}`),

  /** Setting PROCESSED applies the event's effect to the road server-side. */
  patchStatus: (id: string, status: EventStatus): Promise<EventPayload> =>
    api.patch<EventPayload>(`/api/events/${encodeURIComponent(id)}/status`, { status }),
};

// ---- routes --------------------------------------------------------------

export interface RouteRequestPayload {
  source: string;
  destination: string;
}

export interface RouteResponse {
  path: string[];
  totalTravelTime: number | null;
  status: 'VALID' | 'UNREACHABLE';
  requestedFrom: string;
  requestedTo: string;
  message: string;
}

export const routes = {
  /** Server-side Dijkstra over the persisted road network. */
  calculate: (payload: RouteRequestPayload): Promise<RouteResponse> =>
    api.post<RouteResponse>('/api/routes/calculate', payload),
};
