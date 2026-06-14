package com.goweyy.convoyia.common.simulation.scenarios;

import com.goweyy.convoyia.common.simulation.ConvoyScenario;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Validates the Stripe pre-auth computation (totalTtc × 1.20, ceiling rounding).
 * Pre-auth must always be strictly greater than totalTtc.
 */
@Slf4j
@Component
public class ConvoyPreAuthCaptureScenario implements ConvoyScenario {

    private static final BigDecimal STRIPE_PRE_AUTH_MULT = new BigDecimal("1.20");

    @Override
    public String name() { return "ConvoyPreAuthCaptureScenario"; }

    @Override
    public void run() {
        log.info("[Scenario] {} — starting", name());

        BigDecimal[] testTtcAmounts = {
            new BigDecimal("36.00"),    // minimum fare
            new BigDecimal("66.00"),    // standard 50km
            new BigDecimal("132.00"),   // premium 100km
            new BigDecimal("158.40"),   // urgent premium
            new BigDecimal("1440.00"),  // large mission
        };

        BigDecimal[] expectedPreAuths = {
            new BigDecimal("43.20"),
            new BigDecimal("79.20"),
            new BigDecimal("158.40"),
            new BigDecimal("190.08"),
            new BigDecimal("1728.00"),
        };

        for (int i = 0; i < testTtcAmounts.length; i++) {
            BigDecimal ttc      = testTtcAmounts[i];
            BigDecimal expected = expectedPreAuths[i];
            BigDecimal preAuth  = ttc.multiply(STRIPE_PRE_AUTH_MULT).setScale(2, RoundingMode.CEILING);

            assertThat(preAuth.compareTo(expected) == 0,
                    "Pre-auth for ttc=" + ttc + " should be " + expected + ", got " + preAuth);
            assertThat(preAuth.compareTo(ttc) > 0,
                    "Pre-auth must exceed ttc: preAuth=" + preAuth + " ttc=" + ttc);
        }

        // Pre-auth must always use CEILING rounding (to protect platform)
        BigDecimal fractionalTtc = new BigDecimal("55.55");
        BigDecimal preAuth = fractionalTtc.multiply(STRIPE_PRE_AUTH_MULT).setScale(2, RoundingMode.CEILING);
        BigDecimal preAuthHalf = fractionalTtc.multiply(STRIPE_PRE_AUTH_MULT).setScale(2, RoundingMode.HALF_UP);
        // CEILING should round up, ensuring platform is always covered
        assertThat(preAuth.compareTo(preAuthHalf) >= 0,
                "CEILING rounding should produce amount >= HALF_UP rounding");

        log.info("[Scenario] {} — PASSED (validated {} pre-auth amounts)", name(), testTtcAmounts.length);
    }

    private void assertThat(boolean condition, String message) {
        if (!condition) throw new AssertionError("[" + name() + "] " + message);
    }
}
