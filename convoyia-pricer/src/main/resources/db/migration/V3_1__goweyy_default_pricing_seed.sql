-- V3.1: Goweyy default pricing seed
-- GOWEYY DEFAULT PRICING CONFIGURATION
--
-- ⚠️ LEGAL VALIDATION REQUIRED BEFORE GO-LIVE:
-- ┌─────────────────────────────────────────────────────────────────┐
-- │  rate_per_km: 0.00 → TODO: replace with real validated rate    │
-- │  rc_pro_annual_cost: 0.00 → TODO: Hiscox annual contract amount│
-- │  vehicle_coverage_tiers.costPerMission: 0.00 → TODO: real      │
-- │    broker amounts per value tier                               │
-- │  Questions to validate with Maître Gaspoz:                     │
-- │    - Who covers the vehicle during mission?                    │
-- │      (owner, Goweyy, or specific convoyage contract?)          │
-- └─────────────────────────────────────────────────────────────────┘
--
-- platform_fee_ratio = 0.25 (25%) — LOCKED for Goweyy.
-- conveyor always receives 75% (1 - platform_fee_ratio). NO EXCEPTIONS.
-- minimum_fare = 30.00 EUR TTC — LOCKED for Goweyy.

INSERT INTO tenant_pricing_configs (
    tenant_id,
    transport_mode,
    rate_per_km,
    minimum_fare,
    platform_fee_ratio,
    vat_rate,
    stripe_pre_auth_multiplier,
    vehicle_coverage_mode,
    vehicle_coverage_tiers,
    rc_pro_annual_cost,
    rc_pro_estimated_annual_missions,
    surcharge_standard,
    surcharge_courant,
    surcharge_premium,
    surcharge_haut_de_gamme,
    night_bonus_ratio,
    weekend_bonus_ratio,
    multiplier_express,
    multiplier_urgent,
    night_hour_start,
    night_hour_end,
    active
) VALUES (
    'goweyy',
    'DISTANCE_BASED',
    0.0000,      -- TODO: replace with real rate/km after commercial validation
    30.00,       -- minimum fare TTC — LOCKED
    0.2500,      -- 25% platform fee — LOCKED for Goweyy (conveyor gets 75%)
    0.2000,      -- TVA 20%
    1.2000,      -- Stripe pre-auth multiplier
    'PER_MISSION',
    -- vehicle_coverage_tiers: PLACEHOLDER amounts — replace with real broker values
    '[
      {"maxVehicleValue": 20000,  "costPerMission": 0.00},
      {"maxVehicleValue": 40000,  "costPerMission": 0.00},
      {"maxVehicleValue": 70000,  "costPerMission": 0.00},
      {"maxVehicleValue": 120000, "costPerMission": 0.00}
    ]'::jsonb,
    0.00,        -- TODO: replace with real Hiscox RC Pro annual contract cost
    500,         -- TODO: adjust based on actual annual mission volume
    0.00,        -- STANDARD surcharge
    4.00,        -- COURANT surcharge
    10.00,       -- PREMIUM surcharge
    25.00,       -- HAUT_DE_GAMME surcharge
    0.2000,      -- night bonus: +20%
    0.1000,      -- weekend bonus: +10%
    1.1500,      -- EXPRESS multiplier: +15%
    1.3000,      -- URGENT multiplier: +30%
    22,          -- night starts at 22h
    6,           -- night ends at 6h
    true
) ON CONFLICT (tenant_id) DO NOTHING;
