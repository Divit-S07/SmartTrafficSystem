export type Severity = 'CRITICAL' | 'HIGH' | 'NORMAL';

export type VehicleType = 'AMBULANCE' | 'FIRE_ENGINE' | 'RESCUE';

export type VehicleStatus = 'IDLE' | 'EN_ROUTE' | 'ARRIVED' | 'UNREACHABLE';

export type RouteStatus = 'VALID' | 'UNREACHABLE';

export type EventType = 'CLOSURE' | 'REOPEN' | 'CONGESTION';

export type EventStatus = 'PENDING' | 'PROCESSED';

export interface Junction {
  id: string;
  x: number;
  y: number;
}

export interface RoadSegment {
  /** Backend document id — used for update/close/delete API calls. */
  id?: string;
  from: string;
  to: string;
  baseTravelTime: number;
  currentTravelTime: number;
  closed: boolean;
  oneWay: boolean;
}

export interface RoadUpdate {
  travelTime?: number;
  oneWay?: boolean;
  closed?: boolean;
}

export interface Route {
  path: string[];
  totalTravelTime: number;
  status: RouteStatus;
  requestedFrom: string;
  requestedTo: string;
}

export interface EmergencyVehicle {
  id: string;
  type: VehicleType;
  lastConfirmedJunction: string;
  destination: string | null;
  status: VehicleStatus;
  /** Dispatch priority held by the backend (defaults to NORMAL). */
  priority?: Severity;
  currentRoute: Route | null;
}

export interface RoutingRequest {
  id: string;
  vehicleId: string;
  destination: string;
  severity: Severity;
  arrivalSequence: number;
  status: 'PENDING' | 'PROCESSED';
}

export interface TrafficEvent {
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

export interface ActivityEntry {
  id: string;
  timestamp: number;
  category: 'ROUTING_DECISION' | 'VEHICLE_MOVEMENT' | 'TRAFFIC_UPDATE' | 'SYSTEM';
  message: string;
  details: string | null;
}

export type ViewKey =
  | 'dashboard'
  | 'road-network'
  | 'emergency-vehicles'
  | 'route-management'
  | 'traffic-events'
  | 'activity-log';

export const SEVERITY_RANK: Record<Severity, number> = {
  CRITICAL: 3,
  HIGH: 2,
  NORMAL: 1,
};
