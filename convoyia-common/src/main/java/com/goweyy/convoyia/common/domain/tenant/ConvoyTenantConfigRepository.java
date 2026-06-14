package com.goweyy.convoyia.common.domain.tenant;

import com.goweyy.convoyia.common.domain.enums.ConvoyMarket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Repository for ConvoyTenantConfig.
 * Uses R2DBC DatabaseClient — consistent with TenantPricingConfigRepository pattern.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ConvoyTenantConfigRepository {

    private final DatabaseClient databaseClient;

    /**
     * Load the active tenant configuration for the given tenantId.
     */
    public Mono<ConvoyTenantConfig> findActiveByTenantId(String tenantId) {
        return databaseClient.sql("""
                        SELECT * FROM convoy_tenant_configs
                        WHERE tenant_id = :tenantId AND active = true
                        LIMIT 1
                        """)
                .bind("tenantId", tenantId)
                .fetch()
                .one()
                .map(this::mapRow)
                .doOnError(e -> log.error(
                        "Error loading tenant config for tenantId={}: {}", tenantId, e.getMessage()));
    }

    private ConvoyTenantConfig mapRow(java.util.Map<String, Object> row) {
        return ConvoyTenantConfig.builder()
                .tenantId((String) row.get("tenant_id"))
                .tenantName((String) row.get("tenant_name"))
                .tenantLogoUrl((String) row.get("tenant_logo_url"))
                .market(row.get("market") != null ? ConvoyMarket.valueOf((String) row.get("market")) : null)
                .currencyCode((String) row.get("currency_code"))
                .currencySymbol((String) row.get("currency_symbol"))
                .timezone((String) row.get("timezone"))
                .locale((String) row.get("locale"))
                .countryCode((String) row.get("country_code"))
                .taxName((String) row.get("tax_name"))
                .taxRate(toBigDecimal(row.get("tax_rate")))
                .taxNumber((String) row.get("tax_number"))
                .backgroundCheckDocName((String) row.get("background_check_doc_name"))
                .backgroundCheckMaxAgeDays(toInt(row.get("background_check_max_age_days")))
                .acceptedLicenseCategories((String) row.get("accepted_license_categories"))
                .insuranceProviderName((String) row.get("insurance_provider_name"))
                .insuranceCeilingAmount(toBigDecimal(row.get("insurance_ceiling_amount")))
                .insuranceCurrency((String) row.get("insurance_currency"))
                .returnTripPartnerName((String) row.get("return_trip_partner_name"))
                .returnTripPartnerApiUrl((String) row.get("return_trip_partner_api_url"))
                .platformFeeRatio(toBigDecimal(row.get("platform_fee_ratio")))
                .minimumFareTtc(toBigDecimal(row.get("minimum_fare_ttc")))
                .llmPromptLanguage((String) row.get("llm_prompt_language"))
                .stripeAccountId((String) row.get("stripe_account_id"))
                .stripeCurrency((String) row.get("stripe_currency"))
                .active(Boolean.TRUE.equals(row.get("active")))
                .createdAt(row.get("created_at") instanceof Instant i ? i : null)
                .updatedAt(row.get("updated_at") instanceof Instant i ? i : null)
                .build();
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) return null;
        if (value instanceof BigDecimal bd) return bd;
        return new BigDecimal(value.toString());
    }

    private Integer toInt(Object value) {
        if (value == null) return null;
        return ((Number) value).intValue();
    }
}
