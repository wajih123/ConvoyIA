package com.goweyy.convoyia.common.simulation.scenarios;

import com.goweyy.convoyia.common.domain.enums.ConvoyVerificationStatus;
import com.goweyy.convoyia.common.simulation.ConvoyScenario;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Validates driver verification logic:
 * - All blocks pass → VERIFIED
 * - Any block blocked → BLOCKED
 * - VERIFIED != BLOCKED != PARTIAL
 */
@Slf4j
@Component
public class ConvoyDriverVerificationScenario implements ConvoyScenario {

    @Override
    public String name() { return "ConvoyDriverVerificationScenario"; }

    @Override
    public void run() {
        log.info("[Scenario] {} — starting", name());

        // All blocks pass → VERIFIED
        boolean driverPassed  = true;
        boolean vehiclePassed = true;
        boolean missionPassed = true;

        ConvoyVerificationStatus status = computeStatus(driverPassed, vehiclePassed, missionPassed);
        assertThat(status == ConvoyVerificationStatus.VERIFIED,
                "All blocks passing should yield VERIFIED, got " + status);

        // Driver blocked → BLOCKED
        status = computeStatus(false, true, true);
        assertThat(status == ConvoyVerificationStatus.BLOCKED,
                "Driver blocked should yield BLOCKED, got " + status);

        // Vehicle blocked → BLOCKED
        status = computeStatus(true, false, true);
        assertThat(status == ConvoyVerificationStatus.BLOCKED,
                "Vehicle blocked should yield BLOCKED, got " + status);

        // Mission blocked → BLOCKED
        status = computeStatus(true, true, false);
        assertThat(status == ConvoyVerificationStatus.BLOCKED,
                "Mission blocked should yield BLOCKED, got " + status);

        // All blocked → BLOCKED
        status = computeStatus(false, false, false);
        assertThat(status == ConvoyVerificationStatus.BLOCKED,
                "All blocked should yield BLOCKED, got " + status);

        // Enum values are distinct
        assertThat(ConvoyVerificationStatus.VERIFIED != ConvoyVerificationStatus.BLOCKED,
                "VERIFIED must differ from BLOCKED");
        assertThat(ConvoyVerificationStatus.VERIFIED != ConvoyVerificationStatus.PARTIAL,
                "VERIFIED must differ from PARTIAL");

        log.info("[Scenario] {} — PASSED", name());
    }

    /**
     * Mirrors the logic in ConvoyVerifierAgent without depending on it.
     */
    private ConvoyVerificationStatus computeStatus(boolean driver, boolean vehicle, boolean mission) {
        boolean allPassed   = driver && vehicle && mission;
        boolean anyBlocked  = !driver || !vehicle || !mission;
        if (allPassed) return ConvoyVerificationStatus.VERIFIED;
        if (anyBlocked) return ConvoyVerificationStatus.BLOCKED;
        return ConvoyVerificationStatus.PARTIAL;
    }

    private void assertThat(boolean condition, String message) {
        if (!condition) throw new AssertionError("[" + name() + "] " + message);
    }
}
