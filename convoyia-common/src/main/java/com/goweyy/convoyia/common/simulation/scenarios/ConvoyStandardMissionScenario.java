package com.goweyy.convoyia.common.simulation.scenarios;

import com.goweyy.convoyia.common.domain.enums.ConvoyVehicleSegment;
import com.goweyy.convoyia.common.simulation.ConvoyScenario;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Validates the standard mission pricing formula for a 50 km STANDARD segment weekday mission.
 * Transport = FLAT(15) + 50km * 0.80/km = 55.00 HT
 * Segment surcharge = 0.00 (STANDARD)
 * Total HT = 55.00 (above minimum fare 30.00)
 * VAT 20% = 11.00
 * Total TTC = 66.00
 * Conveyor (75%) = 49.50, Platform (25%) = 16.50
 */
@Slf4j
@Component
public class ConvoyStandardMissionScenario implements ConvoyScenario {

    private static final BigDecimal FLAT_BASE      = new BigDecimal("15.00");
    private static final BigDecimal RATE_PER_KM    = new BigDecimal("0.80");
    private static final BigDecimal VAT_RATE        = new BigDecimal("0.20");
    private static final BigDecimal CONVEYOR_RATIO  = new BigDecimal("0.75");
    private static final BigDecimal PLATFORM_RATIO  = new BigDecimal("0.25");
    private static final BigDecimal MINIMUM_FARE    = new BigDecimal("30.00");

    @Override
    public String name() { return "ConvoyStandardMissionScenario"; }

    @Override
    public void run() {
        log.info("[Scenario] {} — starting", name());

        double distanceKm = 50.0;

        // 1. Vehicle segment classification
        double vehicleValue = 18_000;
        ConvoyVehicleSegment segment = ConvoyVehicleSegment.fromValue(vehicleValue);
        assertThat(segment == ConvoyVehicleSegment.STANDARD,
                "Expected STANDARD segment for value " + vehicleValue + ", got " + segment);

        // 2. Transport cost
        BigDecimal transport = FLAT_BASE
                .add(BigDecimal.valueOf(distanceKm).multiply(RATE_PER_KM))
                .setScale(2, RoundingMode.HALF_UP);
        assertThat(transport.compareTo(new BigDecimal("55.00")) == 0,
                "Transport should be 55.00, got " + transport);

        // 3. No segment surcharge for STANDARD
        BigDecimal segmentSurcharge = BigDecimal.ZERO;
        BigDecimal totalHt = transport.add(segmentSurcharge).setScale(2, RoundingMode.HALF_UP);
        assertThat(totalHt.compareTo(MINIMUM_FARE) > 0, "Total HT must exceed minimum fare");

        // 4. VAT 20%
        BigDecimal vatAmount = totalHt.multiply(VAT_RATE).setScale(2, RoundingMode.HALF_UP);
        assertThat(vatAmount.compareTo(new BigDecimal("11.00")) == 0,
                "VAT should be 11.00, got " + vatAmount);

        // 5. Total TTC
        BigDecimal totalTtc = totalHt.add(vatAmount).setScale(2, RoundingMode.HALF_UP);
        assertThat(totalTtc.compareTo(new BigDecimal("66.00")) == 0,
                "Total TTC should be 66.00, got " + totalTtc);

        // 6. 75/25 split
        BigDecimal conveyorPayout = totalTtc.multiply(CONVEYOR_RATIO).setScale(2, RoundingMode.HALF_UP);
        BigDecimal platformFee    = totalTtc.multiply(PLATFORM_RATIO).setScale(2, RoundingMode.HALF_UP);
        assertThat(conveyorPayout.compareTo(new BigDecimal("49.50")) == 0,
                "Conveyor payout should be 49.50, got " + conveyorPayout);
        assertThat(platformFee.compareTo(new BigDecimal("16.50")) == 0,
                "Platform fee should be 16.50, got " + platformFee);

        log.info("[Scenario] {} — PASSED (transport={}, ttc={}, conveyor={}, platform={})",
                name(), transport, totalTtc, conveyorPayout, platformFee);
    }

    private void assertThat(boolean condition, String message) {
        if (!condition) throw new AssertionError("[" + name() + "] " + message);
    }
}
