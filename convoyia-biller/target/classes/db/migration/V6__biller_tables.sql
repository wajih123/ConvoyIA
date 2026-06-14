-- V6: Biller Tables
CREATE TABLE IF NOT EXISTS mission_billing (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    mission_id UUID NOT NULL UNIQUE,
    tenant_id VARCHAR(100) NOT NULL,
    status VARCHAR(30) NOT NULL,
    payment_intent_id VARCHAR(100),
    charge_id VARCHAR(100),
    transfer_id VARCHAR(100),
    conveyor_share NUMERIC(10,2),
    platform_share NUMERIC(10,2),
    total_ttc NUMERIC(10,2),
    client_invoice_url TEXT,
    conveyor_receipt_url TEXT,
    damage_detected BOOLEAN DEFAULT FALSE,
    billed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_billing_mission_id ON mission_billing(mission_id);
CREATE INDEX IF NOT EXISTS idx_billing_tenant_id ON mission_billing(tenant_id);
CREATE INDEX IF NOT EXISTS idx_billing_status ON mission_billing(status);
