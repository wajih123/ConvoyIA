-- V4: Inspector Tables
CREATE TABLE IF NOT EXISTS mission_inspections (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    mission_id UUID NOT NULL,
    tenant_id VARCHAR(100) NOT NULL,
    phase VARCHAR(20) NOT NULL,
    damage_detected BOOLEAN DEFAULT FALSE,
    damage_report JSONB,
    odometer_reading BIGINT,
    fuel_level_percent INTEGER,
    photo_urls JSONB,
    inspected_at TIMESTAMPTZ,
    conveyor_id VARCHAR(100),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(mission_id, phase)
);

CREATE INDEX IF NOT EXISTS idx_insp_mission_id ON mission_inspections(mission_id);
CREATE INDEX IF NOT EXISTS idx_insp_tenant_id ON mission_inspections(tenant_id);
