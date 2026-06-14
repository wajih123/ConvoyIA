-- V16: ConvoyIA Worldwide — Tenant Master Config
-- Shared config table for all worldwide tenants.
-- Goweyy (France) is tenant #1 and the live proof of concept.

CREATE TABLE IF NOT EXISTS convoy_tenant_configs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(100) NOT NULL UNIQUE,
    tenant_name VARCHAR(200),
    tenant_logo_url TEXT,
    market VARCHAR(50),
    currency_code CHAR(3) NOT NULL DEFAULT 'EUR',
    currency_symbol VARCHAR(5) DEFAULT '€',
    timezone VARCHAR(100) DEFAULT 'Europe/Paris',
    locale VARCHAR(10) DEFAULT 'fr-FR',
    country_code CHAR(2) DEFAULT 'fr',
    tax_name VARCHAR(20) DEFAULT 'TVA',
    tax_rate NUMERIC(5,4) NOT NULL DEFAULT 0.2000,
    tax_number VARCHAR(50),
    background_check_doc_name VARCHAR(100) DEFAULT 'Casier B3',
    background_check_max_age_days INTEGER DEFAULT 90,
    accepted_license_categories TEXT DEFAULT '["B"]',
    insurance_provider_name VARCHAR(100),
    insurance_ceiling_amount NUMERIC(12,2) DEFAULT 120000.00,
    insurance_currency CHAR(3) DEFAULT 'EUR',
    return_trip_partner_name VARCHAR(100) DEFAULT 'Bolt Business',
    return_trip_partner_api_url TEXT,
    platform_fee_ratio NUMERIC(5,4) NOT NULL DEFAULT 0.2500,
    minimum_fare_ttc NUMERIC(10,2) NOT NULL DEFAULT 30.00,
    llm_prompt_language VARCHAR(20) DEFAULT 'french',
    stripe_account_id VARCHAR(100),
    stripe_currency VARCHAR(3) DEFAULT 'eur',
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Goweyy FR — Tenant #1, the live proof of concept
INSERT INTO convoy_tenant_configs (
    tenant_id, tenant_name, market,
    currency_code, currency_symbol, timezone, locale, country_code,
    tax_name, tax_rate,
    background_check_doc_name, background_check_max_age_days,
    accepted_license_categories,
    insurance_provider_name, insurance_ceiling_amount,
    return_trip_partner_name,
    platform_fee_ratio, minimum_fare_ttc,
    llm_prompt_language, stripe_currency,
    active
) VALUES (
    'goweyy', 'Goweyy', 'FRANCE',
    'EUR', '€', 'Europe/Paris', 'fr-FR', 'fr',
    'TVA', 0.2000,
    'Casier B3', 90,
    '["B","BE"]',      -- B: car, BE: car + trailer (common for vehicle transport platforms)
    'Hiscox', 120000.00,
    'Bolt Business',
    0.2500, 30.00,
    'french', 'eur',
    true
);

-- ConvoyIA UK Demo — placeholder, not active
INSERT INTO convoy_tenant_configs (
    tenant_id, tenant_name, market,
    currency_code, currency_symbol, timezone, locale, country_code,
    tax_name, tax_rate,
    background_check_doc_name, background_check_max_age_days,
    accepted_license_categories,
    insurance_ceiling_amount, insurance_currency,
    return_trip_partner_name,
    platform_fee_ratio, minimum_fare_ttc,
    llm_prompt_language, stripe_currency,
    active
) VALUES (
    'convoyia-uk-demo', 'ConvoyIA UK Demo', 'UNITED_KINGDOM',
    'GBP', '£', 'Europe/London', 'en-GB', 'gb',
    'VAT', 0.2000,
    'DBS Check', 365,
    '["B","B1","BE"]',
    120000.00, 'GBP',
    'Uber Business',
    0.2500, 25.00,
    'english', 'gbp',
    false
);

-- ConvoyIA UAE Demo — placeholder, not active
INSERT INTO convoy_tenant_configs (
    tenant_id, tenant_name, market,
    currency_code, currency_symbol, timezone, locale, country_code,
    tax_name, tax_rate,
    background_check_doc_name, background_check_max_age_days,
    accepted_license_categories,
    insurance_ceiling_amount, insurance_currency,
    return_trip_partner_name,
    platform_fee_ratio, minimum_fare_ttc,
    llm_prompt_language, stripe_currency,
    active
) VALUES (
    'convoyia-uae-demo', 'ConvoyIA UAE Demo', 'UAE',
    'AED', 'د.إ', 'Asia/Dubai', 'en-AE', 'ae',
    'VAT', 0.0500,
    'Police Clearance', 180,
    '["B","E"]',
    500000.00, 'AED',
    'Careem Business',
    0.2500, 120.00,
    'english', 'aed',
    false
);

CREATE INDEX IF NOT EXISTS idx_ctc_tenant_id ON convoy_tenant_configs(tenant_id);
CREATE INDEX IF NOT EXISTS idx_ctc_market    ON convoy_tenant_configs(market);
CREATE INDEX IF NOT EXISTS idx_ctc_active    ON convoy_tenant_configs(active);
