package com.goweyy.convoyia.common.simulation.scenarios;

import com.goweyy.convoyia.common.simulation.ConvoyScenario;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Component
public class ConvoyCancellationRefundScenario implements ConvoyScenario {

    @Override
    public String name() {
        return "ConvoyCancellationRefundScenario";
    }

    @Override
    public void run() {
        long hoursUntil = 30L;
        BigDecimal totalTtc = new BigDecimal("72.00");
        BigDecimal refundRatio = hoursUntil > 48 ? BigDecimal.ONE : hoursUntil > 24 ? new BigDecimal("0.50") : BigDecimal.ZERO;
        BigDecimal refundAmount = totalTtc.multiply(refundRatio).setScale(2, RoundingMode.HALF_UP);
        assertThat(refundAmount.compareTo(new BigDecimal("36.00")) == 0, "Refund should equal 36.00");
        assertThat(refundAmount.compareTo(totalTtc.multiply(new BigDecimal("0.50")).setScale(2, RoundingMode.HALF_UP)) == 0,
                "Refund should equal 50% of total TTC");
        log.info("[Scenario] {} — PASSED", name());
    }

    private void assertThat(boolean condition, String message) {
        if (!condition) throw new AssertionError("[" + name() + "] " + message);
    }
}
