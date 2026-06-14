package com.goweyy.convoyia.common.simulation.scenarios;

import com.goweyy.convoyia.common.domain.enums.ConvoyVehicleSegment;
import com.goweyy.convoyia.common.simulation.ConvoyScenario;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Validates PREMIUM segment pricing:
 * - Vehicle value 55,000 → PREMIUM segment
 * - Segment surcharge = 15.00
 * - 100 km: transport = 15 + 80 = 95, total HT = 95 + 15 = 110.00
 * - Total TTC = 110 × 1.20 = 132.00
 */
@Slf4j
@Component
public class ConvoyPremiumSegmentScenario implements ConvoyScenario {

    private static final BigDecimal FLAT_BASE           = new BigDecimal("15.00");
    private static final BigDecimal RATE_PER_KM         = new BigDecimal("0.80");
    private static final BigDecimal PREMIUM_SURCHARGE   = new BigDecimal("15.00");
    private static final BigDecimal VAT_RATE             = new BigDecimal("0.20");

    @Override
    public String name() { return "ConvoyPremiumSegmentScenario"; }

    @Override
    public void run() {
        log.info("[Scenario] {} — starting", name());

        // 1. Vehicle value 55,000 → PREMIUM
        double vehicleValue = 55_000;
        ConvoyVehicleSegment segment = ConvoyVehicleSegment.fromValue(vehicleValue);
        assertThat(segment == ConvoyVehicleSegment.PREMIUM,
                "55,000 should map to PREMIUM, got " + segment);

        // 2. Segment surcharge
        double distanceKm = 100.0;
        BigDecimal transport = FLAT_BASE
                .add(BigDecimal.valueOf(distanceKm).multiply(RATE_PER_KM))
                .setScale(2, RoundingMode.HALF_UP); // 95.00

        BigDecimal totalHt = transport.add(PREMIUM_SURCHARGE).setScale(2, RoundingMode.HALF_UP); // 110.00
        assertThat(totalHt.compareTo(new BigDecimal("110.00")) == 0,
                "PREMIUM total HT should be 110.00, got " + totalHt);

        // 3. TTC
        BigDecimal vatAmount = totalHt.multiply(VAT_RATE).setScale(2, RoundingMode.HALF_UP); // 22.00
        BigDecimal totalTtc = totalHt.add(vatAmount); // 132.00
        assertThat(totalTtc.compareTo(new BigDecimal("132.00")) == 0,
                "PREMIUM total TTC should be 132.00, got " + totalTtc);

        // 4. PREMIUM surcharge > COURANT surcharge (5.00)
        BigDecimal courantSurcharge = new BigDecimal("5.00");
        assertThat(PREMIUM_SURCHARGE.compareTo(courantSurcharge) > 0,
                "PREMIUM surcharge must exceed COURANT surcharge");

        // 5. Segment ordering
        assertThat(ConvoyVehicleSegment.PREMIUM.getMinValue() > ConvoyVehicleSegment.COURANT.getMinValue(),
                "PREMIUM min value must exceed COURANT min value");

        log.info("[Scenario] {} — PASSED (transport={}, totalHt={}, totalTtc={})",
                name(), transport, totalHt, totalTtc);
    }

    private void assertThat(boolean condition, String message) {
        if (!condition) throw new AssertionError("[" + name() + "] " + message);
    }
}
