-- V17: ConvoyIA Mission Context additions and missing convoy tables
ALTER TABLE convoy_mission_contexts
    ADD COLUMN IF NOT EXISTS mission_type VARCHAR(20) DEFAULT 'SCHEDULED',
    ADD COLUMN IF NOT EXISTS surge_multiplier NUMERIC(5,4) DEFAULT 1.0000;

CREATE TABLE IF NOT EXISTS convoy_broadcasts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(100) NOT NULL,
    mission_id UUID UNIQUE,
    current_circle VARCHAR(20),
    surge_multiplier NUMERIC(5,4) DEFAULT 1.0000,
    drivers_notified INTEGER DEFAULT 0,
    drivers_declined INTEGER DEFAULT 0,
    accepted_by_driver_id VARCHAR(100),
    started_at TIMESTAMPTZ,
    accepted_at TIMESTAMPTZ,
    outcome VARCHAR(30),
    created_at TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_cb_mission ON convoy_broadcasts(mission_id);
CREATE INDEX IF NOT EXISTS idx_cb_tenant ON convoy_broadcasts(tenant_id);

CREATE TABLE IF NOT EXISTS convoy_disputes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(100) NOT NULL,
    mission_id UUID,
    dispute_type VARCHAR(50),
    status VARCHAR(30),
    description TEXT,
    evidence_urls JSONB,
    resolution VARCHAR(200),
    refund_amount NUMERIC(10,2),
    penalty_amount NUMERIC(10,2),
    escalated BOOLEAN DEFAULT FALSE,
    resolved_at TIMESTAMPTZ,
    opened_by VARCHAR(100),
    created_at TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_cd_mission ON convoy_disputes(mission_id);

CREATE TABLE IF NOT EXISTS convoy_conveyor_penalties (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(100) NOT NULL,
    mission_id UUID,
    conveyor_id VARCHAR(100),
    penalty_type VARCHAR(50),
    amount NUMERIC(10,2),
    currency_code CHAR(3),
    reason TEXT,
    applied BOOLEAN DEFAULT FALSE,
    applied_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_cpen_conveyor ON convoy_conveyor_penalties(conveyor_id);

CREATE TABLE IF NOT EXISTS convoy_return_bookings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(100) NOT NULL,
    mission_id UUID UNIQUE,
    conveyor_id VARCHAR(100),
    return_mode VARCHAR(30),
    origin_address TEXT,
    destination_address TEXT,
    distance_km DOUBLE PRECISION,
    estimated_cost NUMERIC(10,2),
    actual_cost NUMERIC(10,2),
    currency_code CHAR(3),
    provider_reference VARCHAR(100),
    status VARCHAR(30),
    booked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_crb_mission ON convoy_return_bookings(mission_id);

CREATE TABLE IF NOT EXISTS convoy_driver_availability (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(100) NOT NULL,
    driver_id VARCHAR(100) NOT NULL,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    available BOOLEAN DEFAULT TRUE,
    segments TEXT,
    last_heartbeat TIMESTAMPTZ NOT NULL,
    UNIQUE(driver_id, tenant_id),
    created_at TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_cda_available ON convoy_driver_availability(tenant_id, available);

CREATE TABLE IF NOT EXISTS convoy_verifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(100) NOT NULL,
    mission_id UUID NOT NULL,
    status VARCHAR(30) NOT NULL,
    payload JSONB,
    verified_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_cv_mission ON convoy_verifications(mission_id);

CREATE TABLE IF NOT EXISTS convoy_pricing (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(100) NOT NULL,
    mission_id UUID NOT NULL,
    status VARCHAR(30) NOT NULL,
    total_ttc NUMERIC(10,2),
    currency_code CHAR(3),
    payload JSONB,
    priced_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_cp_mission ON convoy_pricing(mission_id);

CREATE TABLE IF NOT EXISTS convoy_inspections (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(100) NOT NULL,
    mission_id UUID NOT NULL,
    phase VARCHAR(30) NOT NULL,
    passed BOOLEAN,
    damage_detected BOOLEAN,
    payload JSONB,
    inspected_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_ci_mission ON convoy_inspections(mission_id);

CREATE TABLE IF NOT EXISTS convoy_tracking_positions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(100) NOT NULL,
    mission_id UUID NOT NULL,
    driver_id VARCHAR(100),
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    speed_kmh DOUBLE PRECISION,
    recorded_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_ctp_mission ON convoy_tracking_positions(mission_id);

CREATE TABLE IF NOT EXISTS convoy_tracking_anomalies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(100) NOT NULL,
    mission_id UUID NOT NULL,
    anomaly_type VARCHAR(50),
    severity VARCHAR(30),
    description TEXT,
    payload JSONB,
    detected_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_cta_mission ON convoy_tracking_anomalies(mission_id);

CREATE TABLE IF NOT EXISTS convoy_billing (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(100) NOT NULL,
    mission_id UUID NOT NULL,
    status VARCHAR(30) NOT NULL,
    charge_id VARCHAR(100),
    transfer_id VARCHAR(100),
    total_ttc NUMERIC(10,2),
    currency_code CHAR(3),
    payload JSONB,
    billed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_cbill_mission ON convoy_billing(mission_id);
