-- V2: Verifier Tables
CREATE TABLE IF NOT EXISTS mission_verifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    mission_id UUID NOT NULL,
    tenant_id VARCHAR(100) NOT NULL,
    global_status VARCHAR(20) NOT NULL,
    vehicle_block JSONB,
    conveyor_block JSONB,
    mission_block JSONB,
    blocking_reasons JSONB,
    alerts JSONB,
    hiscox_confirmed BOOLEAN DEFAULT FALSE,
    verified_at TIMESTAMPTZ NOT NULL,
    verified_by VARCHAR(100),
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_verif_mission_id ON mission_verifications(mission_id);
CREATE INDEX IF NOT EXISTS idx_verif_tenant_id ON mission_verifications(tenant_id);
CREATE INDEX IF NOT EXISTS idx_verif_status ON mission_verifications(global_status);
