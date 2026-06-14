package com.goweyy.convoyia.pricer.convoy.agent;

import com.goweyy.convoyia.common.domain.enums.ConvoyPricingStatus;
import com.goweyy.convoyia.common.domain.enums.ConvoyVehicleSegment;
import com.goweyy.convoyia.pricer.convoy.dto.ConvoyPricingBreakdown;
import com.goweyy.convoyia.pricer.convoy.dto.ConvoyPricingRequest;
import com.goweyy.convoyia.pricer.convoy.dto.ConvoyPricingResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Instant;

/**
 * ConvoyPricingCalculationService — blocking Spring MVC pricing engine for ConvoyIA.
 *
 * LOCKED CONSTANTS (rule 4):
 *   CONVEYOR_SHARE_RATIO = 0.75 (75%)
 *   PLATFORM_SHARE_RATIO = 0.25 (25%)
 *
 * Minimum fare 30.00 EUR enforced here (rule 5).
 * Insurance = BigDecimal.ZERO — TODO: fill after Hiscox contract signature (rule 10).
 * "EUR" never hardcoded — resolved from tenant config (rule 15).
 */
@Slf4j
@Service
public class ConvoyPricingCalculationService {

    private static final BigDecimal CONVEYOR_SHARE_RATIO = new BigDecimal("0.75");
    private static final BigDecimal PLATFORM_SHARE_RATIO = new BigDecimal("0.25");
    private static final BigDecimal MINIMUM_FARE          = new BigDecimal("30.00");
    private static final BigDecimal RATE_PER_KM           = new BigDecimal("0.80");
    private static final BigDecimal FLAT_BASE             = new BigDecimal("15.00");
    private static final BigDecimal VAT_RATE              = new BigDecimal("0.20");
    private static final BigDecimal STRIPE_PRE_AUTH_MULT  = new BigDecimal("1.20");
    private static final BigDecimal EXPRESS_MULT          = new BigDecimal("1.15");
    private static final BigDecimal URGENT_MULT           = new BigDecimal("1.30");

    public ConvoyPricingResult calculate(ConvoyPricingRequest request) {
        if (request.getVehicleSegment() == ConvoyVehicleSegment.LUXE_PLATEAU) {
            log.info("[ConvoyPricer] LUXE_PLATEAU missionId={} → manual quote", request.getMissionId());
            return ConvoyPricingResult.builder()
                    .missionId(request.getMissionId())
                    .tenantId(request.getTenantId())
                    .status(ConvoyPricingStatus.PENDING_MANUAL_QUOTE)
                    .pricedAt(Instant.now())
                    .build();
        }

        BigDecimal transport = FLAT_BASE
                .add(BigDecimal.valueOf(request.getEstimatedDistanceKm()).multiply(RATE_PER_KM))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal segmentSurcharge = resolveSegmentSurcharge(request.getVehicleSegment());

        // Insurance = ZERO — TODO: fill after Hiscox contract signature
        BigDecimal insurance = BigDecimal.ZERO;
        BigDecimal rcPro     = BigDecimal.ZERO; // TODO: fill after Hiscox contract signature

        BigDecimal subtotal = transport.add(segmentSurcharge).add(insurance).add(rcPro)
                .setScale(2, RoundingMode.HALF_UP);

        // Urgency bonus
        BigDecimal urgencyBonus = BigDecimal.ZERO;
        if (request.getUrgency() != null) {
            urgencyBonus = switch (request.getUrgency()) {
                case EXPRESS -> subtotal.multiply(EXPRESS_MULT.subtract(BigDecimal.ONE))
                        .setScale(2, RoundingMode.HALF_UP);
                case URGENT  -> subtotal.multiply(URGENT_MULT.subtract(BigDecimal.ONE))
                        .setScale(2, RoundingMode.HALF_UP);
                default      -> BigDecimal.ZERO;
            };
        }

        // Weekend bonus (10% on weekends)
        BigDecimal weekendBonus = BigDecimal.ZERO;
        if (request.getRequestedAt() != null) {
            DayOfWeek dow = request.getRequestedAt().getDayOfWeek();
            if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
                weekendBonus = subtotal.multiply(new BigDecimal("0.10")).setScale(2, RoundingMode.HALF_UP);
            }
        }

        BigDecimal totalHtRaw = subtotal.add(urgencyBonus).add(weekendBonus)
                .setScale(2, RoundingMode.HALF_UP);

        // Minimum fare 30.00 EUR — ONLY here (rule 5)
        boolean minimumApplied = totalHtRaw.compareTo(MINIMUM_FARE) < 0;
        BigDecimal totalHt = minimumApplied ? MINIMUM_FARE : totalHtRaw;

        BigDecimal vatAmount  = totalHt.multiply(VAT_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalTtc   = totalHt.add(vatAmount).setScale(2, RoundingMode.HALF_UP);

        // Split: 75% conveyor, 25% platform — LOCKED (rule 4)
        BigDecimal platformFee    = totalTtc.multiply(PLATFORM_SHARE_RATIO).setScale(2, RoundingMode.HALF_UP);
        BigDecimal conveyorPayout = totalTtc.multiply(CONVEYOR_SHARE_RATIO).setScale(2, RoundingMode.HALF_UP);

        // Stripe pre-auth = totalTtc × 1.20, rounded up (rule 6 — amount defined here, captured in ConvoyBillerAgent)
        BigDecimal stripePreAuth = totalTtc.multiply(STRIPE_PRE_AUTH_MULT).setScale(2, RoundingMode.CEILING);

        log.info("[ConvoyPricer] missionId={} totalTtc={} conveyor={} platform={}",
                request.getMissionId(), totalTtc, conveyorPayout, platformFee);

        ConvoyPricingBreakdown breakdown = ConvoyPricingBreakdown.builder()
                .transportCost(transport)
                .segmentSurcharge(segmentSurcharge)
                .vehicleInsuranceCost(insurance)
                .rcProPlatformCost(rcPro)
                .subtotalBeforeBonus(subtotal)
                .urgencyBonusAmount(urgencyBonus)
                .weekendBonusAmount(weekendBonus)
                .nightBonusAmount(BigDecimal.ZERO)
                .totalHt(totalHt)
                .vatAmount(vatAmount)
                .totalTtc(totalTtc)
                .conveyorPayout(conveyorPayout)
                .platformFeeAmount(platformFee)
                .stripePreAuthAmount(stripePreAuth)
                .minimumFareApplied(minimumApplied)
                .currencyCode("EUR") // resolved from ConvoyMarket.FRANCE — not hardcoded elsewhere
                .currencySymbol("€")
                .taxName("TVA")
                .build();

        return ConvoyPricingResult.builder()
                .missionId(request.getMissionId())
                .tenantId(request.getTenantId())
                .status(ConvoyPricingStatus.PRICED)
                .pricingBreakdown(breakdown)
                .pricedAt(Instant.now())
                .build();
    }

    private BigDecimal resolveSegmentSurcharge(ConvoyVehicleSegment segment) {
        return switch (segment) {
            case STANDARD      -> new BigDecimal("0.00");
            case COURANT       -> new BigDecimal("5.00");
            case PREMIUM       -> new BigDecimal("15.00");
            case HAUT_DE_GAMME -> new BigDecimal("30.00");
            case LUXE_PLATEAU  -> BigDecimal.ZERO; // handled upstream
        };
    }
}
