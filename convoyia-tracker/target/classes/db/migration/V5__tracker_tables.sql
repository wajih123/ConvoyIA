-- V5: Tracker Tables
CREATE TABLE IF NOT EXISTS tracking_positions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    mission_id UUID NOT NULL,
    tenant_id VARCHAR(100) NOT NULL,
    conveyor_id VARCHAR(100),
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    speed_kmh DOUBLE PRECISION,
    accuracy DOUBLE PRECISION,
    recorded_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS tracking_anomalies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    mission_id UUID NOT NULL,
    tenant_id VARCHAR(100) NOT NULL,
    anomaly_type VARCHAR(50) NOT NULL,
    severity VARCHAR(20),
    description TEXT,
    position JSONB,
    resolved BOOLEAN DEFAULT FALSE,
    detected_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_track_mission_id ON tracking_positions(mission_id);
CREATE INDEX IF NOT EXISTS idx_track_timestamp ON tracking_positions(recorded_at);
CREATE INDEX IF NOT EXISTS idx_anom_mission_id ON tracking_anomalies(mission_id);
