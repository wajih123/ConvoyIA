-- V15: ConvoyIA Mission Contexts — persistent state for all AI agent workflows
-- New blocking (MVC) Convoy* classes use this table via JPA/Spring Data.
-- Separate from mission_contexts (V1, reactive R2DBC) to preserve existing code.

CREATE TABLE IF NOT EXISTS convoy_mission_contexts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    mission_id UUID NOT NULL UNIQUE,
    tenant_id VARCHAR(100) NOT NULL,
    current_state VARCHAR(50) NOT NULL,
    vehicle_segment VARCHAR(50),
    urgency VARCHAR(30),
    confidence_score DOUBLE PRECISION,
    assigned_driver_id VARCHAR(100),
    estimated_duration_min INTEGER,
    origin_address TEXT,
    destination_address TEXT,
    client_aboard BOOLEAN DEFAULT FALSE,
    agent_trace TEXT,
    enriched_data TEXT,
    last_updated TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_cmc_mission_id  ON convoy_mission_contexts(mission_id);
CREATE INDEX IF NOT EXISTS idx_cmc_tenant_id   ON convoy_mission_contexts(tenant_id);
CREATE INDEX IF NOT EXISTS idx_cmc_state       ON convoy_mission_contexts(current_state);
