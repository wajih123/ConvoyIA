package com.goweyy.convoyia.common.simulation.scenarios;

import com.goweyy.convoyia.common.simulation.ConvoyScenario;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Validates urgent mission pricing bonuses.
 * Express bonus = subtotal × 0.15
 * Urgent bonus  = subtotal × 0.30
 * Urgent must always cost more than Express, which must cost more than Standard.
 */
@Slf4j
@Component
public class ConvoyUrgentMissionScenario implements ConvoyScenario {

    private static final BigDecimal FLAT_BASE       = new BigDecimal("15.00");
    private static final BigDecimal RATE_PER_KM     = new BigDecimal("0.80");
    private static final BigDecimal EXPRESS_MULT    = new BigDecimal("0.15");
    private static final BigDecimal URGENT_MULT     = new BigDecimal("0.30");
    private static final BigDecimal VAT_RATE         = new BigDecimal("0.20");
    private static final BigDecimal MINIMUM_FARE     = new BigDecimal("30.00");

    @Override
    public String name() { return "ConvoyUrgentMissionScenario"; }

    @Override
    public void run() {
        log.info("[Scenario] {} — starting", name());

        double distanceKm = 100.0;
        BigDecimal transport = FLAT_BASE
                .add(BigDecimal.valueOf(distanceKm).multiply(RATE_PER_KM))
                .setScale(2, RoundingMode.HALF_UP);  // 15 + 80 = 95.00

        // Standard: no bonus
        BigDecimal htStandard = transport.max(MINIMUM_FARE);

        // Express: +15%
        BigDecimal expressBonus = transport.multiply(EXPRESS_MULT).setScale(2, RoundingMode.HALF_UP);
        BigDecimal htExpress = transport.add(expressBonus).max(MINIMUM_FARE).setScale(2, RoundingMode.HALF_UP);

        // Urgent: +30%
        BigDecimal urgentBonus = transport.multiply(URGENT_MULT).setScale(2, RoundingMode.HALF_UP);
        BigDecimal htUrgent = transport.add(urgentBonus).max(MINIMUM_FARE).setScale(2, RoundingMode.HALF_UP);

        assertThat(htExpress.compareTo(htStandard) > 0,
                "Express should cost more than Standard: express=" + htExpress + " standard=" + htStandard);
        assertThat(htUrgent.compareTo(htExpress) > 0,
                "Urgent should cost more than Express: urgent=" + htUrgent + " express=" + htExpress);

        // TTC checks
        BigDecimal ttcExpress = htExpress.multiply(BigDecimal.ONE.add(VAT_RATE)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal ttcUrgent  = htUrgent.multiply(BigDecimal.ONE.add(VAT_RATE)).setScale(2, RoundingMode.HALF_UP);
        assertThat(ttcUrgent.compareTo(ttcExpress) > 0,
                "Urgent TTC must exceed Express TTC");

        // Verify exact express HT: 95 + 14.25 = 109.25
        assertThat(htExpress.compareTo(new BigDecimal("109.25")) == 0,
                "Express HT should be 109.25, got " + htExpress);
        // Verify exact urgent HT: 95 + 28.50 = 123.50
        assertThat(htUrgent.compareTo(new BigDecimal("123.50")) == 0,
                "Urgent HT should be 123.50, got " + htUrgent);

        log.info("[Scenario] {} — PASSED (standard={}, express={}, urgent={})",
                name(), htStandard, htExpress, htUrgent);
    }

    private void assertThat(boolean condition, String message) {
        if (!condition) throw new AssertionError("[" + name() + "] " + message);
    }
}
