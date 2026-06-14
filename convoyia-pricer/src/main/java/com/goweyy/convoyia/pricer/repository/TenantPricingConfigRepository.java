package com.goweyy.convoyia.pricer.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.goweyy.convoyia.common.domain.enums.TransportPricingMode;
import com.goweyy.convoyia.common.domain.enums.VehicleCoverageMode;
import com.goweyy.convoyia.pricer.domain.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
public class TenantPricingConfigRepository {

    private final DatabaseClient databaseClient;
    private final ObjectMapper objectMapper;

    public Mono<PricingFormulaConfig> findActiveByTenantId(String tenantId) {
        return databaseClient.sql("""
                        SELECT * FROM tenant_pricing_configs
                        WHERE tenant_id = :tenantId AND active = true
                        LIMIT 1
                        """)
                .bind("tenantId", tenantId)
                .fetch()
                .one()
                .map(row -> mapRowToConfig(row))
                .doOnError(e -> log.error("Error loading pricing config for tenantId={}: {}", tenantId, e.getMessage()));
    }

    public Mono<Void> upsertConfig(PricingFormulaConfig config) {
        try {
            String tiersJson = objectMapper.writeValueAsString(config.getInsuranceConfig().getVehicleCoverageTiers());
            return databaseClient.sql("""
                            INSERT INTO tenant_pricing_configs (
                                tenant_id, transport_mode, rate_per_km, flat_base_fare,
                                minimum_fare, platform_fee_ratio, vat_rate,
                                stripe_pre_auth_multiplier, vehicle_coverage_mode,
                                vehicle_coverage_tiers, rc_pro_annual_cost,
                                rc_pro_estimated_annual_missions,
                                surcharge_standard, surcharge_courant, surcharge_premium,
                                surcharge_haut_de_gamme, night_bonus_ratio, weekend_bonus_ratio,
                                multiplier_express, multiplier_urgent,
                                night_hour_start, night_hour_end, active, updated_at
                            ) VALUES (
                                :tenantId, :transportMode, :ratePerKm, :flatBaseFare,
                                :minimumFare, :platformFeeRatio, :vatRate,
                                :stripePreAuthMultiplier, :vehicleCoverageMode,
                                :vehicleCoverageTiers::jsonb, :rcProAnnualCost,
                                :rcProEstimatedAnnualMissions,
                                :surchargeStandard, :surchargeCourant, :surchargePremium,
                                :surchargeHautDeGamme, :nightBonusRatio, :weekendBonusRatio,
                                :multiplierExpress, :multiplierUrgent,
                                :nightHourStart, :nightHourEnd, :active, NOW()
                            )
                            ON CONFLICT (tenant_id) DO UPDATE SET
                                transport_mode = EXCLUDED.transport_mode,
                                rate_per_km = EXCLUDED.rate_per_km,
                                flat_base_fare = EXCLUDED.flat_base_fare,
                                minimum_fare = EXCLUDED.minimum_fare,
                                platform_fee_ratio = EXCLUDED.platform_fee_ratio,
                                vat_rate = EXCLUDED.vat_rate,
                                stripe_pre_auth_multiplier = EXCLUDED.stripe_pre_auth_multiplier,
                                vehicle_coverage_mode = EXCLUDED.vehicle_coverage_mode,
                                vehicle_coverage_tiers = EXCLUDED.vehicle_coverage_tiers,
                                rc_pro_annual_cost = EXCLUDED.rc_pro_annual_cost,
                                rc_pro_estimated_annual_missions = EXCLUDED.rc_pro_estimated_annual_missions,
                                surcharge_standard = EXCLUDED.surcharge_standard,
                                surcharge_courant = EXCLUDED.surcharge_courant,
                                surcharge_premium = EXCLUDED.surcharge_premium,
                                surcharge_haut_de_gamme = EXCLUDED.surcharge_haut_de_gamme,
                                night_bonus_ratio = EXCLUDED.night_bonus_ratio,
                                weekend_bonus_ratio = EXCLUDED.weekend_bonus_ratio,
                                multiplier_express = EXCLUDED.multiplier_express,
                                multiplier_urgent = EXCLUDED.multiplier_urgent,
                                night_hour_start = EXCLUDED.night_hour_start,
                                night_hour_end = EXCLUDED.night_hour_end,
                                active = EXCLUDED.active,
                                updated_at = NOW()
                            """)
                    .bind("tenantId", config.getTenantId())
                    .bind("transportMode", config.getTransportMode().name())
                    .bind("ratePerKm", config.getRatePerKm() != null ? config.getRatePerKm() : BigDecimal.ZERO)
                    .bind("flatBaseFare", config.getFlatBaseFare() != null ? config.getFlatBaseFare() : BigDecimal.ZERO)
                    .bind("minimumFare", config.getMinimumFare())
                    .bind("platformFeeRatio", config.getPlatformFeeRatio())
                    .bind("vatRate", config.getVatRate())
                    .bind("stripePreAuthMultiplier", config.getStripePreAuthMultiplier())
                    .bind("vehicleCoverageMode", config.getInsuranceConfig().getVehicleCoverageMode().name())
                    .bind("vehicleCoverageTiers", tiersJson)
                    .bind("rcProAnnualCost", config.getInsuranceConfig().getRcProPlatformAnnualCost() != null
                            ? config.getInsuranceConfig().getRcProPlatformAnnualCost() : BigDecimal.ZERO)
                    .bind("rcProEstimatedAnnualMissions", config.getInsuranceConfig().getRcProEstimatedAnnualMissions())
                    .bind("surchargeStandard", config.getSegmentSurcharges().getStandard())
                    .bind("surchargeCourant", config.getSegmentSurcharges().getCourant())
                    .bind("surchargePremium", config.getSegmentSurcharges().getPremium())
                    .bind("surchargeHautDeGamme", config.getSegmentSurcharges().getHautDeGamme())
                    .bind("nightBonusRatio", config.getContextMultipliers().getNightBonusRatio())
                    .bind("weekendBonusRatio", config.getContextMultipliers().getWeekendBonusRatio() != null
                            ? config.getContextMultipliers().getWeekendBonusRatio() : null)
                    .bind("multiplierExpress", config.getContextMultipliers().getExpressMultiplier())
                    .bind("multiplierUrgent", config.getContextMultipliers().getUrgentMultiplier())
                    .bind("nightHourStart", config.getContextMultipliers().getNightHourStart())
                    .bind("nightHourEnd", config.getContextMultipliers().getNightHourEnd())
                    .bind("active", config.isActive())
                    .fetch()
                    .rowsUpdated()
                    .then();
        } catch (Exception e) {
            return Mono.error(new RuntimeException("Failed to serialize pricing config", e));
        }
    }

    @SuppressWarnings("unchecked")
    private PricingFormulaConfig mapRowToConfig(java.util.Map<String, Object> row) {
        try {
            String tiersJson = (String) row.get("vehicle_coverage_tiers");
            List<InsuranceTier> tiers = tiersJson != null
                    ? objectMapper.readValue(tiersJson, new TypeReference<List<InsuranceTier>>() {})
                    : List.of();

            InsuranceConfig insurance = InsuranceConfig.builder()
                    .vehicleCoverageMode(VehicleCoverageMode.valueOf((String) row.get("vehicle_coverage_mode")))
                    .vehicleCoverageTiers(tiers)
                    .rcProPlatformAnnualCost(toBigDecimal(row.get("rc_pro_annual_cost")))
                    .rcProEstimatedAnnualMissions(toInt(row.get("rc_pro_estimated_annual_missions"), 500))
                    .build();

            SegmentSurchargeConfig surcharges = SegmentSurchargeConfig.builder()
                    .standard(toBigDecimal(row.get("surcharge_standard")))
                    .courant(toBigDecimal(row.get("surcharge_courant")))
                    .premium(toBigDecimal(row.get("surcharge_premium")))
                    .hautDeGamme(toBigDecimal(row.get("surcharge_haut_de_gamme")))
                    .build();

            BigDecimal weekendRatio = row.get("weekend_bonus_ratio") != null
                    ? toBigDecimal(row.get("weekend_bonus_ratio")) : null;

            ContextMultipliersConfig multipliers = ContextMultipliersConfig.builder()
                    .nightBonusRatio(toBigDecimal(row.get("night_bonus_ratio")))
                    .weekendBonusRatio(weekendRatio)
                    .expressMultiplier(toBigDecimal(row.get("multiplier_express")))
                    .urgentMultiplier(toBigDecimal(row.get("multiplier_urgent")))
                    .nightHourStart(toInt(row.get("night_hour_start"), 22))
                    .nightHourEnd(toInt(row.get("night_hour_end"), 6))
                    .build();

            return PricingFormulaConfig.builder()
                    .tenantId((String) row.get("tenant_id"))
                    .transportMode(TransportPricingMode.valueOf((String) row.get("transport_mode")))
                    .ratePerKm(toBigDecimal(row.get("rate_per_km")))
                    .flatBaseFare(toBigDecimal(row.get("flat_base_fare")))
                    .minimumFare(toBigDecimal(row.get("minimum_fare")))
                    .platformFeeRatio(toBigDecimal(row.get("platform_fee_ratio")))
                    .vatRate(toBigDecimal(row.get("vat_rate")))
                    .stripePreAuthMultiplier(toBigDecimal(row.get("stripe_pre_auth_multiplier")))
                    .insuranceConfig(insurance)
                    .segmentSurcharges(surcharges)
                    .contextMultipliers(multipliers)
                    .active(Boolean.TRUE.equals(row.get("active")))
                    .createdAt(row.get("created_at") instanceof Instant i ? i : null)
                    .updatedAt(row.get("updated_at") instanceof Instant i ? i : null)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to map pricing config row", e);
        }
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal bd) return bd;
        return new BigDecimal(value.toString());
    }

    private int toInt(Object value, int defaultValue) {
        if (value == null) return defaultValue;
        return ((Number) value).intValue();
    }
}
