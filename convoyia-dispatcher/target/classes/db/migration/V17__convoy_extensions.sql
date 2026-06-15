-- V17: ConvoyIA additional tables and context field extensions
-- Adds mission_type + surge_multiplier to convoy_mission_contexts.
-- Creates all remaining ConvoyIA operational tables that were specified
-- in the V15 design but not yet created.

-- Extend convoy_mission_contexts
ALTER TABLE convoy_mission_contexts
    ADD COLUMN IF NOT EXISTS mission_type VARCHAR(20) DEFAULT 'SCHEDULED',
    ADD COLUMN IF NOT EXISTS surge_multiplier NUMERIC(5,4) DEFAULT 1.0000;

CREATE INDEX IF NOT EXISTS idx_cmc_type ON convoy_mission_contexts(mission_type);

-- -------------------------
-- Broadcast state table
-- -------------------------
CREATE TABLE IF NOT EXISTS convoy_broadcasts (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id     VARCHAR(100) NOT NULL,
    mission_id    UUID UNIQUE,
    current_circle     VARCHAR(20),
    surge_multiplier   NUMERIC(5,4) DEFAULT 1.0000,
    drivers_notified   INTEGER DEFAULT 0,
    drivers_declined   INTEGER DEFAULT 0,
    accepted_by_driver_id VARCHAR(100),
    started_at    TIMESTAMPTZ,
    accepted_at   TIMESTAMPTZ,
    outcome       VARCHAR(30),
    created_at    TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_cb_mission ON convoy_broadcasts(mission_id);
CREATE INDEX IF NOT EXISTS idx_cb_tenant  ON convoy_broadcasts(tenant_id);

-- -------------------------
-- Verification results
-- -------------------------
CREATE TABLE IF NOT EXISTS convoy_verifications (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id      VARCHAR(100) NOT NULL,
    mission_id     UUID,
    global_status  VARCHAR(20),
    vehicle_block  JSONB,
    driver_block   JSONB,
    mission_block  JSONB,
    blocking_reasons JSONB,
    alerts         JSONB,
    express_mode   BOOLEAN DEFAULT FALSE,
    hiscox_ceiling_exceeded BOOLEAN DEFAULT FALSE,
    verified_at    TIMESTAMPTZ,
    verified_by    VARCHAR(100),
    created_at     TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_cv_mission ON convoy_verifications(mission_id);
CREATE INDEX IF NOT EXISTS idx_cv_tenant  ON convoy_verifications(tenant_id);

-- -------------------------
-- Pricing records
-- -------------------------
CREATE TABLE IF NOT EXISTS convoy_pricing (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id               VARCHAR(100) NOT NULL,
    mission_id              UUID UNIQUE,
    status                  VARCHAR(30),
    mission_type            VARCHAR(20),
    transport_cost          NUMERIC(10,2),
    segment_surcharge       NUMERIC(10,2),
    vehicle_insurance_cost  NUMERIC(10,2),
    rc_pro_platform_cost    NUMERIC(10,2),
    night_bonus             NUMERIC(10,2),
    weekend_bonus           NUMERIC(10,2),
    urgency_bonus           NUMERIC(10,2),
    surge_amount            NUMERIC(10,2),
    surge_multiplier        NUMERIC(5,4) DEFAULT 1.0000,
    surge_reason            VARCHAR(100),
    total_ht                NUMERIC(10,2),
    vat_amount              NUMERIC(10,2),
    total_ttc               NUMERIC(10,2),
    platform_fee_amount     NUMERIC(10,2),
    conveyor_payout         NUMERIC(10,2),
    stripe_pre_auth_amount  NUMERIC(10,2),
    minimum_fare_applied    BOOLEAN DEFAULT FALSE,
    currency_code           CHAR(3),
    applied_formula_summary TEXT,
    pricing_breakdown       JSONB,
    priced_at               TIMESTAMPTZ,
    created_at              TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_cp_mission ON convoy_pricing(mission_id);

-- -------------------------
-- Inspection records
-- -------------------------
CREATE TABLE IF NOT EXISTS convoy_inspections (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id            VARCHAR(100) NOT NULL,
    mission_id           UUID,
    phase                VARCHAR(20),
    damage_detected      BOOLEAN DEFAULT FALSE,
    damage_report        JSONB,
    odometer_reading     BIGINT,
    fuel_level_percent   INTEGER,
    photo_urls           JSONB,
    overall_condition    VARCHAR(20),
    signature_request_id VARCHAR(100),
    signature_status     VARCHAR(20),
    signed_document_url  TEXT,
    inspected_at         TIMESTAMPTZ,
    conveyor_id          VARCHAR(100),
    created_at           TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(mission_id, phase)
);
CREATE INDEX IF NOT EXISTS idx_ci_mission ON convoy_inspections(mission_id);

-- -------------------------
-- GPS tracking
-- -------------------------
CREATE TABLE IF NOT EXISTS convoy_tracking_positions (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id    VARCHAR(100) NOT NULL,
    mission_id   UUID,
    conveyor_id  VARCHAR(100),
    latitude     DOUBLE PRECISION NOT NULL,
    longitude    DOUBLE PRECISION NOT NULL,
    speed_kmh    DOUBLE PRECISION,
    accuracy     DOUBLE PRECISION,
    recorded_at  TIMESTAMPTZ NOT NULL,
    created_at   TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_ctp_mission ON convoy_tracking_positions(mission_id);
CREATE INDEX IF NOT EXISTS idx_ctp_time    ON convoy_tracking_positions(recorded_at);

CREATE TABLE IF NOT EXISTS convoy_tracking_anomalies (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id     VARCHAR(100) NOT NULL,
    mission_id    UUID,
    anomaly_type  VARCHAR(50),
    severity      VARCHAR(20),
    description   TEXT,
    position      JSONB,
    resolved      BOOLEAN DEFAULT FALSE,
    detected_at   TIMESTAMPTZ,
    created_at    TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_cta_mission ON convoy_tracking_anomalies(mission_id);

-- -------------------------
-- Billing records
-- -------------------------
CREATE TABLE IF NOT EXISTS convoy_billing (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           VARCHAR(100) NOT NULL,
    mission_id          UUID UNIQUE,
    status              VARCHAR(30),
    payment_intent_id   VARCHAR(100),
    charge_id           VARCHAR(100),
    transfer_id         VARCHAR(100),
    conveyor_share      NUMERIC(10,2),
    platform_share      NUMERIC(10,2),
    total_ttc           NUMERIC(10,2),
    currency_code       CHAR(3),
    client_invoice_url  TEXT,
    conveyor_receipt_url TEXT,
    damage_detected     BOOLEAN DEFAULT FALSE,
    billed_at           TIMESTAMPTZ,
    created_at          TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_cbill_mission ON convoy_billing(mission_id);

-- -------------------------
-- Disputes
-- -------------------------
CREATE TABLE IF NOT EXISTS convoy_disputes (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id     VARCHAR(100) NOT NULL,
    mission_id    UUID,
    dispute_type  VARCHAR(50),
    status        VARCHAR(30),
    description   TEXT,
    evidence_urls JSONB,
    resolution    VARCHAR(200),
    refund_amount NUMERIC(10,2),
    penalty_amount NUMERIC(10,2),
    escalated     BOOLEAN DEFAULT FALSE,
    resolved_at   TIMESTAMPTZ,
    opened_by     VARCHAR(100),
    created_at    TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_cd_mission ON convoy_disputes(mission_id);

-- -------------------------
-- Conveyor penalties
-- -------------------------
CREATE TABLE IF NOT EXISTS convoy_conveyor_penalties (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id    VARCHAR(100) NOT NULL,
    mission_id   UUID,
    conveyor_id  VARCHAR(100),
    penalty_type VARCHAR(50),
    amount       NUMERIC(10,2),
    currency_code CHAR(3),
    reason       TEXT,
    applied      BOOLEAN DEFAULT FALSE,
    applied_at   TIMESTAMPTZ,
    created_at   TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_cpen_conveyor ON convoy_conveyor_penalties(conveyor_id);

-- -------------------------
-- Return bookings
-- -------------------------
CREATE TABLE IF NOT EXISTS convoy_return_bookings (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id          VARCHAR(100) NOT NULL,
    mission_id         UUID UNIQUE,
    conveyor_id        VARCHAR(100),
    return_mode        VARCHAR(30),
    origin_address     TEXT,
    destination_address TEXT,
    distance_km        DOUBLE PRECISION,
    estimated_cost     NUMERIC(10,2),
    actual_cost        NUMERIC(10,2),
    currency_code      CHAR(3),
    provider_reference VARCHAR(100),
    status             VARCHAR(30),
    booked_at          TIMESTAMPTZ,
    created_at         TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_crb_mission ON convoy_return_bookings(mission_id);

-- -------------------------
-- Driver availability snapshot (persistent complement to Redis)
-- -------------------------
CREATE TABLE IF NOT EXISTS convoy_driver_availability (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       VARCHAR(100) NOT NULL,
    driver_id       VARCHAR(100) NOT NULL,
    latitude        DOUBLE PRECISION,
    longitude       DOUBLE PRECISION,
    available       BOOLEAN DEFAULT TRUE,
    segments        TEXT,
    last_heartbeat  TIMESTAMPTZ NOT NULL,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(driver_id, tenant_id)
);
CREATE INDEX IF NOT EXISTS idx_cda_available ON convoy_driver_availability(tenant_id, available);
