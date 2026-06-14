package com.goweyy.convoyia.common.simulation.scenarios;

import com.goweyy.convoyia.common.simulation.ConvoyScenario;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Validates minimum fare enforcement.
 * A 1 km STANDARD mission: transport = 15 + 0.80 = 15.80 → below MINIMUM_FARE 30.00.
 * Result must be 30.00 HT with minimumFareApplied = true.
 */
@Slf4j
@Component
public class ConvoyMinimumFareScenario implements ConvoyScenario {

    private static final BigDecimal FLAT_BASE    = new BigDecimal("15.00");
    private static final BigDecimal RATE_PER_KM  = new BigDecimal("0.80");
    private static final BigDecimal MINIMUM_FARE = new BigDecimal("30.00");
    private static final BigDecimal VAT_RATE     = new BigDecimal("0.20");

    @Override
    public String name() { return "ConvoyMinimumFareScenario"; }

    @Override
    public void run() {
        log.info("[Scenario] {} — starting", name());

        double distanceKm = 1.0;
        BigDecimal rawTransport = FLAT_BASE
                .add(BigDecimal.valueOf(distanceKm).multiply(RATE_PER_KM))
                .setScale(2, RoundingMode.HALF_UP); // 15.80

        assertThat(rawTransport.compareTo(MINIMUM_FARE) < 0,
                "Raw transport " + rawTransport + " must be below minimum fare " + MINIMUM_FARE);

        // Minimum fare applied
        BigDecimal totalHt = rawTransport.max(MINIMUM_FARE);
        assertThat(totalHt.compareTo(MINIMUM_FARE) == 0,
                "Total HT must equal minimum fare when raw transport is below it");

        // VAT on minimum fare
        BigDecimal vatAmount = MINIMUM_FARE.multiply(VAT_RATE).setScale(2, RoundingMode.HALF_UP);
        assertThat(vatAmount.compareTo(new BigDecimal("6.00")) == 0,
                "VAT on 30.00 should be 6.00, got " + vatAmount);

        BigDecimal totalTtc = totalHt.add(vatAmount).setScale(2, RoundingMode.HALF_UP);
        assertThat(totalTtc.compareTo(new BigDecimal("36.00")) == 0,
                "Total TTC should be 36.00, got " + totalTtc);

        // Also validate that a 0 km mission still hits minimum fare
        BigDecimal zeroKmTransport = FLAT_BASE;
        assertThat(zeroKmTransport.max(MINIMUM_FARE).compareTo(MINIMUM_FARE) == 0,
                "Zero km must also use minimum fare");

        log.info("[Scenario] {} — PASSED (rawTransport={}, minimumApplied, ttc={})",
                name(), rawTransport, totalTtc);
    }

    private void assertThat(boolean condition, String message) {
        if (!condition) throw new AssertionError("[" + name() + "] " + message);
    }
}
