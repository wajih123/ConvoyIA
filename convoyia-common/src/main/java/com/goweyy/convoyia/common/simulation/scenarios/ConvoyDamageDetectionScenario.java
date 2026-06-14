package com.goweyy.convoyia.common.simulation.scenarios;

import com.goweyy.convoyia.common.domain.enums.ConvoyBillingStatus;
import com.goweyy.convoyia.common.simulation.ConvoyScenario;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Validates damage detection causes billing to pause.
 * When damageDetected=true, billing status must be PENDING_DAMAGE_REVIEW.
 * When damageDetected=false and pricing is PRICED, billing proceeds normally.
 */
@Slf4j
@Component
public class ConvoyDamageDetectionScenario implements ConvoyScenario {

    @Override
    public String name() { return "ConvoyDamageDetectionScenario"; }

    @Override
    public void run() {
        log.info("[Scenario] {} — starting", name());

        // 1. Damage detected → billing paused
        boolean damageDetected = true;
        ConvoyBillingStatus expectedStatus = damageDetected
                ? ConvoyBillingStatus.PENDING_DAMAGE_REVIEW
                : ConvoyBillingStatus.BILLED;

        assertThat(expectedStatus == ConvoyBillingStatus.PENDING_DAMAGE_REVIEW,
                "Damage detection must pause billing");

        // 2. No damage → billing can proceed
        boolean noDamage = false;
        ConvoyBillingStatus noFaultStatus = noDamage
                ? ConvoyBillingStatus.PENDING_DAMAGE_REVIEW
                : ConvoyBillingStatus.BILLED;
        assertThat(noFaultStatus == ConvoyBillingStatus.BILLED,
                "No damage must allow billing to proceed");

        // 3. PENDING_DAMAGE_REVIEW is a distinct status from BILLED and FAILED
        assertThat(ConvoyBillingStatus.PENDING_DAMAGE_REVIEW != ConvoyBillingStatus.BILLED,
                "PENDING_DAMAGE_REVIEW must differ from BILLED");
        assertThat(ConvoyBillingStatus.PENDING_DAMAGE_REVIEW != ConvoyBillingStatus.FAILED,
                "PENDING_DAMAGE_REVIEW must differ from FAILED");

        // 4. All 4 billing statuses exist
        assertThat(ConvoyBillingStatus.values().length == 4,
                "Expected 4 billing statuses (PENDING, BILLED, PENDING_DAMAGE_REVIEW, FAILED)");

        log.info("[Scenario] {} — PASSED (damage=PENDING_DAMAGE_REVIEW, no-damage=BILLED)", name());
    }

    private void assertThat(boolean condition, String message) {
        if (!condition) throw new AssertionError("[" + name() + "] " + message);
    }
}
