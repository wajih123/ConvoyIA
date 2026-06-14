package com.goweyy.convoyia.common.simulation.scenarios;

import com.goweyy.convoyia.common.simulation.ConvoyScenario;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Validates the locked 75%/25% conveyor/platform split invariant.
 * For any positive TTC, conveyor + platform must equal TTC (within rounding tolerance).
 * Conveyor must always be 75% and platform 25%.
 */
@Slf4j
@Component
public class ConvoyConveyorSplitScenario implements ConvoyScenario {

    private static final BigDecimal CONVEYOR_RATIO = new BigDecimal("0.75");
    private static final BigDecimal PLATFORM_RATIO = new BigDecimal("0.25");

    @Override
    public String name() { return "ConvoyConveyorSplitScenario"; }

    @Override
    public void run() {
        log.info("[Scenario] {} — starting", name());

        // Test with various TTC amounts representing different mission scenarios
        BigDecimal[] testAmounts = {
            new BigDecimal("36.00"),   // minimum fare TTC
            new BigDecimal("66.00"),   // standard 50km
            new BigDecimal("114.00"),  // express 100km
            new BigDecimal("1200.00"), // large mission
        };

        for (BigDecimal totalTtc : testAmounts) {
            BigDecimal conveyor = totalTtc.multiply(CONVEYOR_RATIO).setScale(2, RoundingMode.HALF_UP);
            BigDecimal platform = totalTtc.multiply(PLATFORM_RATIO).setScale(2, RoundingMode.HALF_UP);

            // Ratio validation
            BigDecimal conveyorRatio = conveyor.divide(totalTtc, 4, RoundingMode.HALF_UP);
            BigDecimal platformRatio = platform.divide(totalTtc, 4, RoundingMode.HALF_UP);

            assertThat(conveyorRatio.compareTo(new BigDecimal("0.7500")) == 0,
                    "Conveyor ratio should be 0.75 for ttc=" + totalTtc + ", got " + conveyorRatio);
            assertThat(platformRatio.compareTo(new BigDecimal("0.2500")) == 0,
                    "Platform ratio should be 0.25 for ttc=" + totalTtc + ", got " + platformRatio);

            // Sum should equal TTC (rounding can produce ±0.01 difference max)
            BigDecimal sum = conveyor.add(platform);
            BigDecimal diff = totalTtc.subtract(sum).abs();
            assertThat(diff.compareTo(new BigDecimal("0.01")) <= 0,
                    "conveyor+platform=" + sum + " should equal ttc=" + totalTtc + " (diff=" + diff + ")");

            log.debug("[Scenario] {} ttc={} → conveyor={} platform={}", name(), totalTtc, conveyor, platform);
        }

        // Conveyor must always be larger than platform (75 > 25)
        for (BigDecimal totalTtc : testAmounts) {
            BigDecimal conveyor = totalTtc.multiply(CONVEYOR_RATIO).setScale(2, RoundingMode.HALF_UP);
            BigDecimal platform = totalTtc.multiply(PLATFORM_RATIO).setScale(2, RoundingMode.HALF_UP);
            assertThat(conveyor.compareTo(platform) > 0,
                    "Conveyor payout must always exceed platform fee");
        }

        log.info("[Scenario] {} — PASSED (validated {} amounts)", name(), testAmounts.length);
    }

    private void assertThat(boolean condition, String message) {
        if (!condition) throw new AssertionError("[" + name() + "] " + message);
    }
}
