package com.goweyy.convoyia.pricer.service;

import com.goweyy.convoyia.common.domain.enums.VehicleSegment;
import com.goweyy.convoyia.pricer.domain.PricingRequest;
import com.goweyy.convoyia.pricer.domain.PricingResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;

@Slf4j
@Service
public class PricingCalculationService {

    private static final BigDecimal MINIMUM_FARE = new BigDecimal("30.00");
    private static final BigDecimal BASE_RATE_PER_KM = new BigDecimal("1.20");
    private static final BigDecimal VAT_RATE = new BigDecimal("1.20");
    private static final BigDecimal NIGHT_BONUS_RATE = new BigDecimal("0.20");
    private static final BigDecimal STRIPE_PRE_AUTH_MULTIPLIER = new BigDecimal("1.20");
    private static final BigDecimal CONVEYOR_SHARE_RATIO = new BigDecimal("0.75");
    private static final BigDecimal PLATFORM_SHARE_RATIO = new BigDecimal("0.25");
    private static final BigDecimal ESTIMATED_RETURN_COST = new BigDecimal("15.00");

    public Mono<PricingResult> calculate(PricingRequest request) {
        return Mono.fromCallable(() -> {
            double distanceKm = request.getEstimatedDistanceKm() != null
                    ? request.getEstimatedDistanceKm()
                    : 50.0;

            BigDecimal baseFare = BASE_RATE_PER_KM
                    .multiply(BigDecimal.valueOf(distanceKm))
                    .setScale(2, RoundingMode.HALF_UP);
            log.debug("missionId={} baseFare={}", request.getMissionId(), baseFare);

            BigDecimal segmentSurcharge = getSegmentSurcharge(request.getVehicleSegment());
            log.debug("missionId={} segmentSurcharge={}", request.getMissionId(), segmentSurcharge);

            BigDecimal subtotal = baseFare.add(segmentSurcharge).setScale(2, RoundingMode.HALF_UP);

            boolean isNight = isNightTime(request.getRequestedAt());
            BigDecimal nightBonus = BigDecimal.ZERO;
            if (isNight) {
                nightBonus = subtotal.multiply(NIGHT_BONUS_RATE).setScale(2, RoundingMode.HALF_UP);
                subtotal = subtotal.add(nightBonus).setScale(2, RoundingMode.HALF_UP);
                log.debug("missionId={} nightBonus={}", request.getMissionId(), nightBonus);
            }

            BigDecimal totalHt = subtotal.max(MINIMUM_FARE).setScale(2, RoundingMode.HALF_UP);
            BigDecimal totalTtc = totalHt.multiply(VAT_RATE).setScale(2, RoundingMode.HALF_UP);

            // Enforce minimum fare TTC
            BigDecimal minFareTtc = MINIMUM_FARE.multiply(VAT_RATE).setScale(2, RoundingMode.HALF_UP);
            if (totalTtc.compareTo(MINIMUM_FARE) < 0) {
                totalTtc = MINIMUM_FARE;
            }
            log.debug("missionId={} totalHt={} totalTtc={}", request.getMissionId(), totalHt, totalTtc);

            BigDecimal stripePreAuth = totalTtc.multiply(STRIPE_PRE_AUTH_MULTIPLIER)
                    .setScale(2, RoundingMode.CEILING);
            log.debug("missionId={} stripePreAuth={}", request.getMissionId(), stripePreAuth);

            // ALWAYS 75/25 split — NO EXCEPTIONS
            BigDecimal conveyorShare = totalTtc.multiply(CONVEYOR_SHARE_RATIO).setScale(2, RoundingMode.HALF_UP);
            BigDecimal platformShare = totalTtc.multiply(PLATFORM_SHARE_RATIO).setScale(2, RoundingMode.HALF_UP);
            log.debug("missionId={} conveyorShare={} platformShare={}", request.getMissionId(), conveyorShare, platformShare);

            return PricingResult.builder()
                    .missionId(request.getMissionId())
                    .tenantId(request.getTenantId())
                    .status("PRICED")
                    .baseFare(baseFare)
                    .segmentSurcharge(segmentSurcharge)
                    .nightBonus(nightBonus)
                    .totalHt(totalHt)
                    .totalTtc(totalTtc)
                    .stripePreAuthAmount(stripePreAuth)
                    .conveyorShare(conveyorShare)
                    .platformShare(platformShare)
                    .estimatedReturnCost(ESTIMATED_RETURN_COST)
                    .currency("EUR")
                    .pricedAt(Instant.now())
                    .build();
        });
    }

    private BigDecimal getSegmentSurcharge(VehicleSegment segment) {
        if (segment == null) return BigDecimal.ZERO;
        return switch (segment) {
            case STANDARD -> BigDecimal.ZERO;
            case COURANT -> new BigDecimal("4.00");
            case PREMIUM -> new BigDecimal("10.00");
            case HAUT_DE_GAMME -> new BigDecimal("25.00");
            case LUXE_PLATEAU -> BigDecimal.ZERO; // handled separately
        };
    }

    private boolean isNightTime(LocalDateTime dateTime) {
        if (dateTime == null) return false;
        int hour = dateTime.getHour();
        return hour >= 22 || hour < 6;
    }
}
