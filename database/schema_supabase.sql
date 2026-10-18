-- ============================================================================
-- VEGAM CITY SMART TRAFFIC MANAGEMENT & EMERGENCY ROUTING
-- Supabase (PostgreSQL) Database & Cloud Storage Schema
-- Paste this script into the Supabase SQL Editor
-- ============================================================================

-- Enable pgcrypto if needed
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- 1. Junctions Table
CREATE TABLE IF NOT EXISTS public.junctions (
    id TEXT PRIMARY KEY,
    name TEXT,
    x_coord INTEGER DEFAULT 0,
    y_coord INTEGER DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 2. Roads Table
CREATE TABLE IF NOT EXISTS public.roads (
    id BIGSERIAL PRIMARY KEY,
    from_junction TEXT NOT NULL REFERENCES public.junctions(id) ON DELETE CASCADE,
    to_junction TEXT NOT NULL REFERENCES public.junctions(id) ON DELETE CASCADE,
    base_travel_time NUMERIC(8,2) NOT NULL,
    current_travel_time NUMERIC(8,2) NOT NULL,
    is_closed BOOLEAN DEFAULT FALSE,
    is_two_way BOOLEAN DEFAULT FALSE,
    last_updated TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT uk_road_segment UNIQUE (from_junction, to_junction)
);

-- 3. Vehicles Table
CREATE TABLE IF NOT EXISTS public.vehicles (
    id TEXT PRIMARY KEY,
    vehicle_type TEXT NOT NULL CHECK (vehicle_type IN ('AMBULANCE', 'FIRE_ENGINE', 'RESCUE')),
    current_junction TEXT NOT NULL REFERENCES public.junctions(id),
    destination_junction TEXT REFERENCES public.junctions(id),
    status TEXT DEFAULT 'IDLE' CHECK (status IN ('IDLE', 'EN_ROUTE', 'ARRIVED', 'UNREACHABLE')),
    current_route TEXT,
    route_travel_time NUMERIC(8,2),
    last_updated TIMESTAMPTZ DEFAULT NOW()
);

-- 4. Routing Requests
CREATE TABLE IF NOT EXISTS public.routing_requests (
    id TEXT PRIMARY KEY,
    vehicle_id TEXT NOT NULL REFERENCES public.vehicles(id) ON DELETE CASCADE,
    destination_junction TEXT NOT NULL REFERENCES public.junctions(id),
    severity TEXT NOT NULL CHECK (severity IN ('CRITICAL', 'HIGH', 'NORMAL')),
    severity_rank INTEGER NOT NULL,
    arrival_sequence INTEGER NOT NULL,
    status TEXT DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'PROCESSED', 'ESCALATED', 'FAILED')),
    computed_route TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 5. Traffic Events
CREATE TABLE IF NOT EXISTS public.traffic_events (
    id BIGSERIAL PRIMARY KEY,
    sequence_num INTEGER NOT NULL,
    event_type TEXT NOT NULL CHECK (event_type IN ('CLOSE', 'REOPEN', 'CONGESTION')),
    from_junction TEXT NOT NULL,
    to_junction TEXT NOT NULL,
    slowdown_factor NUMERIC(5,2) DEFAULT 1.00,
    description TEXT NOT NULL,
    processed BOOLEAN DEFAULT FALSE,
    processed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 6. Traffic Snapshots & Audit Logs (Stored in database & backed up to Supabase Storage)
CREATE TABLE IF NOT EXISTS public.audit_logs (
    id BIGSERIAL PRIMARY KEY,
    action TEXT NOT NULL,
    details JSONB NOT NULL,
    logged_at TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================================================
-- SUPABASE CLOUD STORAGE BUCKET SETUP
-- Creates the 'traffic-backups' bucket in Supabase Cloud Storage
-- ============================================================================

INSERT INTO storage.buckets (id, name, public)
VALUES ('traffic-backups', 'traffic-backups', true)
ON CONFLICT (id) DO NOTHING;

-- Storage bucket access policies
CREATE POLICY "Public Read Access for Traffic Backups"
ON storage.objects FOR SELECT
USING (bucket_id = 'traffic-backups');

CREATE POLICY "Public / Service Insert Access for Traffic Backups"
ON storage.objects FOR INSERT
WITH CHECK (bucket_id = 'traffic-backups');

CREATE POLICY "Public / Service Update Access for Traffic Backups"
ON storage.objects FOR UPDATE
USING (bucket_id = 'traffic-backups');

-- Enable Row Level Security (RLS) with open read/write for demo API access
ALTER TABLE public.junctions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.roads ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.vehicles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.routing_requests ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.traffic_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.audit_logs ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Allow anon read/write junctions" ON public.junctions FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Allow anon read/write roads" ON public.roads FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Allow anon read/write vehicles" ON public.vehicles FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Allow anon read/write requests" ON public.routing_requests FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Allow anon read/write events" ON public.traffic_events FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Allow anon read/write audit_logs" ON public.audit_logs FOR ALL USING (true) WITH CHECK (true);

-- Seed Initial Data
INSERT INTO public.junctions (id, name, x_coord, y_coord) VALUES
('J1', 'Central Hub', 100, 200),
('J2', 'North Ring East', 220, 100),
('J3', 'East Express Junction', 380, 100),
('J4', 'Suburban Terminal', 540, 100),
('J5', 'Metro Plaza', 220, 300),
('J6', 'Tech Park North', 380, 300),
('J7', 'East Boulevard', 540, 300),
('J8', 'South Spine A', 100, 420),
('J9', 'South Spine B', 220, 420),
('J10', 'Airport Interchange', 380, 420),
('J11', 'Medical City', 540, 420),
('J12', 'Disaster Relief Base', 660, 300)
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.vehicles (id, vehicle_type, current_junction, status) VALUES
('AMB1', 'AMBULANCE', 'J1', 'IDLE'),
('AMB2', 'AMBULANCE', 'J11', 'IDLE'),
('FIRE1', 'FIRE_ENGINE', 'J4', 'IDLE'),
('RESCUE1', 'RESCUE', 'J8', 'IDLE')
ON CONFLICT (id) DO NOTHING;

