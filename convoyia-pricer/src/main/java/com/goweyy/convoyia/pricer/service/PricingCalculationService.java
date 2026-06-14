package com.goweyy.convoyia.pricer.service;

import com.goweyy.convoyia.common.domain.enums.PricingStatus;
import com.goweyy.convoyia.common.domain.enums.VehicleCoverageMode;
import com.goweyy.convoyia.common.domain.enums.VehicleSegment;
import com.goweyy.convoyia.pricer.domain.*;
import com.goweyy.convoyia.pricer.exception.PricingConfigNotFoundException;
import com.goweyy.convoyia.pricer.repository.TenantPricingConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class PricingCalculationService {

    private final TenantPricingConfigRepository configRepository;

    public Mono<PricingResult> calculate(PricingRequest request) {

        if (request.getVehicleSegment() == VehicleSegment.LUXE_PLATEAU) {
            log.info("[Pricer] Mission {} → LUXE_PLATEAU → manual quote", request.getMissionId());
            return Mono.just(PricingResult.builder()
                    .missionId(request.getMissionId())
                    .tenantId(request.getTenantId())
                    .status(PricingStatus.PENDING_MANUAL_QUOTE)
                    .pricedAt(Instant.now())
                    .build());
        }

        return configRepository.findActiveByTenantId(request.getTenantId())
                .switchIfEmpty(Mono.error(new PricingConfigNotFoundException(request.getTenantId())))
                .map(config -> computePrice(request, config))
                .doOnSuccess(r -> {
                    PricingBreakdown b = r.getPricingBreakdown();
                    log.info("[Pricer] Mission {} priced → TTC={} | conveyor={} | platform={} | vehicleInsurance={} | rcPro={}",
                            request.getMissionId(), b.getTotalTtc(), b.getConveyorPayout(),
                            b.getPlatformFeeAmount(), b.getVehicleInsuranceCost(), b.getRcProPlatformCost());
                });
    }

    private PricingResult computePrice(PricingRequest req, PricingFormulaConfig config) {

        // STEP 1 — Transport cost
        BigDecimal transportCost = computeTransport(req, config);
        log.debug("[Pricer] {} transport={}", req.getMissionId(), transportCost);

        // STEP 2 — Segment surcharge
        BigDecimal segmentSurcharge = resolveSegmentSurcharge(req.getVehicleSegment(), config.getSegmentSurcharges());
        log.debug("[Pricer] {} segmentSurcharge={}", req.getMissionId(), segmentSurcharge);

        // STEP 3 — Vehicle insurance per mission (separate from Hiscox RC Pro)
        // ⚠️ Hiscox = RC Pro for the PLATFORM (Goweyy liability). NOT vehicle coverage.
        BigDecimal vehicleInsuranceCost = computeVehicleInsurance(req.getVehicleDeclaredValue(), config.getInsuranceConfig());
        log.debug("[Pricer] {} vehicleInsurance={}", req.getMissionId(), vehicleInsuranceCost);

        // STEP 4 — RC Pro platform (Hiscox) per-mission share
        BigDecimal rcProCost = computeRcProPerMission(config.getInsuranceConfig());
        log.debug("[Pricer] {} rcPro(Hiscox)={}", req.getMissionId(), rcProCost);

        // STEP 5 — Subtotal before bonuses
        BigDecimal subtotal = transportCost
                .add(segmentSurcharge)
                .add(vehicleInsuranceCost)
                .add(rcProCost)
                .setScale(2, RoundingMode.HALF_UP);

        // STEP 6 — Context bonuses
        ContextMultipliersConfig ctx = config.getContextMultipliers();

        BigDecimal nightBonus = isNight(req.getRequestedAt(), ctx)
                ? subtotal.multiply(ctx.getNightBonusRatio()).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal weekendBonus = (isWeekend(req.getRequestedAt()) && ctx.getWeekendBonusRatio() != null)
                ? subtotal.multiply(ctx.getWeekendBonusRatio()).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal urgencyBonus = switch (req.getUrgency()) {
            case EXPRESS -> subtotal.multiply(ctx.getExpressMultiplier().subtract(BigDecimal.ONE))
                    .setScale(2, RoundingMode.HALF_UP);
            case URGENT -> subtotal.multiply(ctx.getUrgentMultiplier().subtract(BigDecimal.ONE))
                    .setScale(2, RoundingMode.HALF_UP);
            default -> BigDecimal.ZERO;
        };

        // STEP 7 — Total HT + enforce minimum fare
        BigDecimal totalHtRaw = subtotal.add(nightBonus).add(weekendBonus).add(urgencyBonus)
                .setScale(2, RoundingMode.HALF_UP);
        boolean minimumApplied = totalHtRaw.compareTo(config.getMinimumFare()) < 0;
        BigDecimal totalHt = minimumApplied ? config.getMinimumFare() : totalHtRaw;
        log.debug("[Pricer] {} totalHt={} (minimumEnforced={})", req.getMissionId(), totalHt, minimumApplied);

        // STEP 8 — TVA
        BigDecimal vatAmount = totalHt.multiply(config.getVatRate()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalTtc = totalHt.add(vatAmount).setScale(2, RoundingMode.HALF_UP);

        // STEP 9 — Platform fee & conveyor payout (Goweyy: 25%/75% — LOCKED)
        BigDecimal platformFee = totalTtc.multiply(config.getPlatformFeeRatio()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal conveyorPayout = totalTtc.subtract(platformFee).setScale(2, RoundingMode.HALF_UP);
        log.debug("[Pricer] {} totalTtc={} platform={} conveyor={}", req.getMissionId(), totalTtc, platformFee, conveyorPayout);

        // STEP 10 — Stripe pre-auth (totalTtc × 1.20, rounded up)
        BigDecimal stripePreAuth = totalTtc.multiply(config.getStripePreAuthMultiplier()).setScale(2, RoundingMode.CEILING);

        PricingBreakdown breakdown = PricingBreakdown.builder()
                .transportCost(transportCost)
                .segmentSurcharge(segmentSurcharge)
                .vehicleInsuranceCost(vehicleInsuranceCost)
                .rcProPlatformCost(rcProCost)
                .subtotalBeforeBonus(subtotal)
                .nightBonusAmount(nightBonus)
                .weekendBonusAmount(weekendBonus)
                .urgencyBonusAmount(urgencyBonus)
                .totalHt(totalHt)
                .vatAmount(vatAmount)
                .totalTtc(totalTtc)
                .platformFeeAmount(platformFee)
                .conveyorPayout(conveyorPayout)
                .stripePreAuthAmount(stripePreAuth)
                .minimumFareApplied(minimumApplied)
                .appliedFormulaSummary(buildSummary(config))
                .build();

        return PricingResult.builder()
                .missionId(req.getMissionId())
                .tenantId(req.getTenantId())
                .status(PricingStatus.PRICED)
                .pricingBreakdown(breakdown)
                .pricedAt(Instant.now())
                .build();
    }

    /**
     * Vehicle insurance cost per mission — tiered by declared vehicle value.
     * NOTE: This is NOT Hiscox. Hiscox covers platform RC Pro only.
     * The vehicle coverage type must be validated with an insurance broker.
     */
    private BigDecimal computeVehicleInsurance(double vehicleValue, InsuranceConfig insurance) {
        if (insurance.getVehicleCoverageMode() != VehicleCoverageMode.PER_MISSION) {
            return BigDecimal.ZERO;
        }
        return insurance.getVehicleCoverageTiers().stream()
                .filter(tier -> vehicleValue <= tier.getMaxVehicleValue())
                .findFirst()
                .map(InsuranceTier::getCostPerMission)
                .orElse(BigDecimal.ZERO); // LUXE_PLATEAU already handled upstream
    }

    /**
     * RC Pro platform (Hiscox) per-mission share.
     * annualCost ÷ estimatedAnnualMissions = cost per mission.
     * Returns ZERO while placeholder values (0.00) are in place.
     */
    private BigDecimal computeRcProPerMission(InsuranceConfig insurance) {
        if (insurance.getRcProPlatformAnnualCost() == null
                || insurance.getRcProPlatformAnnualCost().compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO; // TODO: placeholder — fill after Hiscox contract signature
        }
        return insurance.getRcProPlatformAnnualCost()
                .divide(BigDecimal.valueOf(insurance.getRcProEstimatedAnnualMissions()), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal computeTransport(PricingRequest req, PricingFormulaConfig config) {
        BigDecimal distance = BigDecimal.valueOf(req.getEstimatedDistanceKm());
        return switch (config.getTransportMode()) {
            case DISTANCE_BASED -> distance.multiply(config.getRatePerKm()).setScale(2, RoundingMode.HALF_UP);
            case FLAT_RATE -> config.getFlatBaseFare();
            case HYBRID -> config.getFlatBaseFare()
                    .add(distance.multiply(config.getRatePerKm()).setScale(2, RoundingMode.HALF_UP));
        };
    }

    private BigDecimal resolveSegmentSurcharge(VehicleSegment segment, SegmentSurchargeConfig s) {
        return switch (segment) {
            case STANDARD -> s.getStandard();
            case COURANT -> s.getCourant();
            case PREMIUM -> s.getPremium();
            case HAUT_DE_GAMME -> s.getHautDeGamme();
            case LUXE_PLATEAU -> BigDecimal.ZERO; // handled upstream
        };
    }

    private boolean isNight(LocalDateTime dt, ContextMultipliersConfig ctx) {
        if (dt == null) return false;
        int h = dt.getHour();
        return h >= ctx.getNightHourStart() || h < ctx.getNightHourEnd();
    }

    private boolean isWeekend(LocalDateTime dt) {
        if (dt == null) return false;
        return dt.getDayOfWeek() == DayOfWeek.SATURDAY || dt.getDayOfWeek() == DayOfWeek.SUNDAY;
    }

    private String buildSummary(PricingFormulaConfig config) {
        return String.format(
                "transport=%s | vehicleInsurance=%s | rcPro=HISCOX_PLATFORM | fee=%.0f%%",
                config.getTransportMode(),
                config.getInsuranceConfig().getVehicleCoverageMode(),
                config.getPlatformFeeRatio().multiply(BigDecimal.valueOf(100)));
    }
}

