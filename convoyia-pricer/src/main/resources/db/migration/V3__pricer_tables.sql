-- V3: Pricer Tables
CREATE TABLE IF NOT EXISTS mission_pricing (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    mission_id UUID NOT NULL UNIQUE,
    tenant_id VARCHAR(100) NOT NULL,
    status VARCHAR(30) NOT NULL,
    base_fare NUMERIC(10,2),
    segment_surcharge NUMERIC(10,2),
    night_bonus NUMERIC(10,2),
    total_ht NUMERIC(10,2),
    total_ttc NUMERIC(10,2),
    stripe_pre_auth_amount NUMERIC(10,2),
    conveyor_share NUMERIC(10,2),
    platform_share NUMERIC(10,2),
    estimated_return_cost NUMERIC(10,2),
    currency VARCHAR(3) DEFAULT 'EUR',
    priced_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_pricing_mission_id ON mission_pricing(mission_id);
CREATE INDEX IF NOT EXISTS idx_pricing_tenant_id ON mission_pricing(tenant_id);
