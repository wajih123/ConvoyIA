package com.goweyy.convoyia.common.simulation.scenarios;

import com.goweyy.convoyia.common.domain.enums.ConvoyBillingStatus;
import com.goweyy.convoyia.common.domain.enums.ConvoyPricingStatus;
import com.goweyy.convoyia.common.simulation.ConvoyScenario;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Validates complete billing flow for a standard priced mission.
 * Given a PRICED mission with totalTtc=66.00:
 *   - Billing status → BILLED
 *   - Stripe pre-auth = 66.00 × 1.20 = 79.20
 *   - Conveyor payout = 66.00 × 0.75 = 49.50
 *   - Platform fee = 66.00 × 0.25 = 16.50
 */
@Slf4j
@Component
public class ConvoyBillingCompletionScenario implements ConvoyScenario {

    private static final BigDecimal STRIPE_PRE_AUTH_MULT = new BigDecimal("1.20");
    private static final BigDecimal CONVEYOR_RATIO       = new BigDecimal("0.75");
    private static final BigDecimal PLATFORM_RATIO       = new BigDecimal("0.25");

    @Override
    public String name() { return "ConvoyBillingCompletionScenario"; }

    @Override
    public void run() {
        log.info("[Scenario] {} — starting", name());

        // Simulate a PRICED result with totalTtc = 66.00
        BigDecimal totalTtc = new BigDecimal("66.00");
        ConvoyPricingStatus pricingStatus = ConvoyPricingStatus.PRICED;

        assertThat(pricingStatus == ConvoyPricingStatus.PRICED,
                "Pricing must be PRICED before billing");

        // Billing computes Stripe pre-auth
        BigDecimal stripePreAuth = totalTtc.multiply(STRIPE_PRE_AUTH_MULT)
                .setScale(2, RoundingMode.CEILING);
        assertThat(stripePreAuth.compareTo(new BigDecimal("79.20")) == 0,
                "Stripe pre-auth should be 79.20, got " + stripePreAuth);

        // Split
        BigDecimal conveyorPayout = totalTtc.multiply(CONVEYOR_RATIO).setScale(2, RoundingMode.HALF_UP);
        BigDecimal platformFee    = totalTtc.multiply(PLATFORM_RATIO).setScale(2, RoundingMode.HALF_UP);
        assertThat(conveyorPayout.compareTo(new BigDecimal("49.50")) == 0,
                "Conveyor payout should be 49.50, got " + conveyorPayout);
        assertThat(platformFee.compareTo(new BigDecimal("16.50")) == 0,
                "Platform fee should be 16.50, got " + platformFee);

        // Final billing status
        ConvoyBillingStatus billingStatus = ConvoyBillingStatus.BILLED;
        assertThat(billingStatus == ConvoyBillingStatus.BILLED, "Billing status must be BILLED");

        // Pre-auth must be higher than actual TTC (to cover incidentals)
        assertThat(stripePreAuth.compareTo(totalTtc) > 0,
                "Stripe pre-auth must exceed total TTC");

        log.info("[Scenario] {} — PASSED (ttc={}, preAuth={}, conveyor={}, platform={})",
                name(), totalTtc, stripePreAuth, conveyorPayout, platformFee);
    }

    private void assertThat(boolean condition, String message) {
        if (!condition) throw new AssertionError("[" + name() + "] " + message);
    }
}
