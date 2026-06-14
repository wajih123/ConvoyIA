-- V1: Dispatcher Tables
CREATE TABLE IF NOT EXISTS mission_contexts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    mission_id UUID NOT NULL UNIQUE,
    tenant_id VARCHAR(100) NOT NULL,
    current_state VARCHAR(50) NOT NULL,
    vehicle_segment VARCHAR(50),
    confidence_score DOUBLE PRECISION,
    agent_trace TEXT,
    enriched_data TEXT,
    last_updated TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_mc_mission_id ON mission_contexts(mission_id);
CREATE INDEX IF NOT EXISTS idx_mc_tenant_id ON mission_contexts(tenant_id);
