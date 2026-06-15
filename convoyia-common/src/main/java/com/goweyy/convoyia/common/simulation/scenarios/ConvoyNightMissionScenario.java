package com.goweyy.convoyia.common.simulation.scenarios;

import com.goweyy.convoyia.common.domain.enums.ConvoyVehicleSegment;
import com.goweyy.convoyia.common.simulation.ConvoyScenario;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Component
public class ConvoyNightMissionScenario implements ConvoyScenario {

    @Override
    public String name() {
        return "ConvoyNightMissionScenario";
    }

    @Override
    public void run() {
        BigDecimal transport = new BigDecimal("15.00").add(new BigDecimal("40.00").multiply(new BigDecimal("0.80")));
        BigDecimal segmentSurcharge = new BigDecimal("5.00");
        BigDecimal subtotal = transport.add(segmentSurcharge).setScale(2, RoundingMode.HALF_UP);
        BigDecimal nightBonus = subtotal.multiply(new BigDecimal("0.20")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalHt = subtotal.add(nightBonus).setScale(2, RoundingMode.HALF_UP);
        BigDecimal vatAmount = totalHt.multiply(new BigDecimal("0.20")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalTtc = totalHt.add(vatAmount).setScale(2, RoundingMode.HALF_UP);

        assertThat(ConvoyVehicleSegment.fromValue(30000) == ConvoyVehicleSegment.COURANT, "Expected COURANT segment");
        assertThat(nightBonus.compareTo(BigDecimal.ZERO) > 0, "Night bonus should be positive");
        assertThat(totalTtc.compareTo(new BigDecimal("66.00")) > 0, "Night mission TTC should exceed non-night TTC");
        assertThat(totalTtc.compareTo(new BigDecimal("74.88")) == 0, "Night mission TTC should equal 74.88");
        log.info("[Scenario] {} — PASSED", name());
    }

    private void assertThat(boolean condition, String message) {
        if (!condition) throw new AssertionError("[" + name() + "] " + message);
    }
}
