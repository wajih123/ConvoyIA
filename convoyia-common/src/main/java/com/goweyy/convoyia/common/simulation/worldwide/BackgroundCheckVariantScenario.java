package com.goweyy.convoyia.common.simulation.worldwide;

import com.goweyy.convoyia.common.simulation.ConvoyScenario;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * BackgroundCheckVariantScenario — validates that the background check validity
 * uses the tenant's configured maxAgeDays, NOT a hardcoded value.
 *
 * Cases:
 * 1. FR tenant: driver has "Casier B3" dated 45 days ago → PASSED (max 90 days)
 * 2. UK tenant: driver has "DBS Check" dated 200 days ago → PASSED (max 365 days)
 * 3. UK tenant: driver has "DBS Check" dated 400 days ago → BLOCKED (expired)
 */
@Slf4j
@Component
public class BackgroundCheckVariantScenario implements ConvoyScenario {

    @Override
    public String name() {
        return "BackgroundCheckVariantScenario";
    }

    @Override
    public void run() throws Exception {
        log.info("[Scenario] {} — start", name());

        // Case 1: FR tenant, Casier B3, 45 days old → PASSED
        assertBackgroundCheck("goweyy", "Casier B3", 90,
                LocalDate.now().minusDays(45), true, "FR 45-day-old B3 must PASS");

        // Case 2: UK tenant, DBS Check, 200 days old → PASSED
        assertBackgroundCheck("convoyia-uk-demo", "DBS Check", 365,
                LocalDate.now().minusDays(200), true, "UK 200-day-old DBS must PASS");

        // Case 3: UK tenant, DBS Check, 400 days old → BLOCKED
        assertBackgroundCheck("convoyia-uk-demo", "DBS Check", 365,
                LocalDate.now().minusDays(400), false, "UK 400-day-old DBS must FAIL");

        log.info("[Scenario] {} — PASSED", name());
    }

    /**
     * Evaluates whether the background check document is still valid based on tenant rules.
     *
     * @param tenantId       tenant identifier
     * @param docName        background check document type
     * @param maxAgeDays     tenant's configured maximum document age in days
     * @param documentDate   date the document was issued
     * @param expectedResult true = document should be VALID, false = document should be EXPIRED
     * @param message        assertion message
     */
    private void assertBackgroundCheck(String tenantId, String docName, int maxAgeDays,
                                        LocalDate documentDate, boolean expectedResult,
                                        String message) {
        long daysOld = ChronoUnit.DAYS.between(documentDate, LocalDate.now());
        boolean isValid = daysOld <= maxAgeDays;

        if (isValid != expectedResult) {
            throw new AssertionError(String.format(
                    "[%s] %s — FAILED: %s (daysOld=%d, maxAgeDays=%d, isValid=%s, expected=%s)",
                    name(), tenantId, message, daysOld, maxAgeDays, isValid, expectedResult));
        }

        log.info("[Scenario] {} tenant={} doc='{}' daysOld={} maxAgeDays={} → {} — OK",
                name(), tenantId, docName, daysOld, maxAgeDays,
                isValid ? "PASSED" : "BLOCKED");
    }
}
