-- V3: Pricer Tables
-- tenant_pricing_configs: one row per tenant, holds the full pricing formula
CREATE TABLE IF NOT EXISTS tenant_pricing_configs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(100) NOT NULL UNIQUE,
    transport_mode VARCHAR(30) NOT NULL DEFAULT 'DISTANCE_BASED',
    rate_per_km NUMERIC(10,4),
    flat_base_fare NUMERIC(10,2),
    minimum_fare NUMERIC(10,2) NOT NULL DEFAULT 30.00,
    platform_fee_ratio NUMERIC(5,4) NOT NULL,
    vat_rate NUMERIC(5,4) NOT NULL DEFAULT 0.2000,
    stripe_pre_auth_multiplier NUMERIC(5,4) NOT NULL DEFAULT 1.2000,
    -- INSURANCE: vehicle_coverage_mode = PER_MISSION | INCLUDED | EXTERNAL
    -- NOTE: Hiscox = RC Pro for the PLATFORM (Goweyy liability). NOT vehicle coverage.
    vehicle_coverage_mode VARCHAR(30) DEFAULT 'PER_MISSION',
    -- vehicle_coverage_tiers: JSON array of {maxVehicleValue, costPerMission}
    -- ⚠️ costPerMission values are PLACEHOLDERS — replace with real broker amounts
    vehicle_coverage_tiers JSONB,
    -- rc_pro_annual_cost: Hiscox annual RC Pro contract cost for the platform
    -- ⚠️ PLACEHOLDER — replace with real value after Hiscox contract signature
    rc_pro_annual_cost NUMERIC(10,2) DEFAULT 0.00,
    rc_pro_estimated_annual_missions INTEGER DEFAULT 500,
    surcharge_standard NUMERIC(10,2) DEFAULT 0.00,
    surcharge_courant NUMERIC(10,2) DEFAULT 4.00,
    surcharge_premium NUMERIC(10,2) DEFAULT 10.00,
    surcharge_haut_de_gamme NUMERIC(10,2) DEFAULT 25.00,
    night_bonus_ratio NUMERIC(5,4) DEFAULT 0.2000,
    weekend_bonus_ratio NUMERIC(5,4),
    multiplier_express NUMERIC(5,4) DEFAULT 1.1500,
    multiplier_urgent NUMERIC(5,4) DEFAULT 1.3000,
    night_hour_start INTEGER DEFAULT 22,
    night_hour_end INTEGER DEFAULT 6,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- mission_pricing: one row per priced mission, full breakdown for audit
CREATE TABLE IF NOT EXISTS mission_pricing (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    mission_id UUID NOT NULL UNIQUE,
    tenant_id VARCHAR(100) NOT NULL,
    status VARCHAR(30) NOT NULL,
    transport_cost NUMERIC(10,2),
    segment_surcharge NUMERIC(10,2),
    vehicle_insurance_cost NUMERIC(10,2),
    rc_pro_platform_cost NUMERIC(10,2),
    night_bonus NUMERIC(10,2),
    weekend_bonus NUMERIC(10,2),
    urgency_bonus NUMERIC(10,2),
    total_ht NUMERIC(10,2),
    vat_amount NUMERIC(10,2),
    total_ttc NUMERIC(10,2),
    platform_fee_amount NUMERIC(10,2),
    conveyor_payout NUMERIC(10,2),
    stripe_pre_auth_amount NUMERIC(10,2),
    minimum_fare_applied BOOLEAN DEFAULT FALSE,
    applied_formula_summary TEXT,
    pricing_breakdown JSONB,
    priced_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_pricing_mission_id ON mission_pricing(mission_id);
CREATE INDEX IF NOT EXISTS idx_pricing_tenant_id ON mission_pricing(tenant_id);

