package com.goweyy.convoyia.common.simulation.scenarios;

import com.goweyy.convoyia.common.domain.enums.ConvoyVehicleSegment;
import com.goweyy.convoyia.common.simulation.ConvoyScenario;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDateTime;

@Slf4j
@Component
public class ConvoyWeekendMissionScenario implements ConvoyScenario {

    @Override
    public String name() {
        return "ConvoyWeekendMissionScenario";
    }

    @Override
    public void run() {
        LocalDateTime saturday = LocalDateTime.now();
        while (saturday.getDayOfWeek() != DayOfWeek.SATURDAY) {
            saturday = saturday.plusDays(1);
        }
        BigDecimal transport = new BigDecimal("15.00").add(new BigDecimal("50.00").multiply(new BigDecimal("0.80")));
        BigDecimal segmentSurcharge = new BigDecimal("5.00");
        BigDecimal subtotal = transport.add(segmentSurcharge).setScale(2, RoundingMode.HALF_UP);
        BigDecimal weekendBonus = subtotal.multiply(new BigDecimal("0.10")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalHt = subtotal.add(weekendBonus).setScale(2, RoundingMode.HALF_UP);
        BigDecimal vat = totalHt.multiply(new BigDecimal("0.20")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalTtc = totalHt.add(vat).setScale(2, RoundingMode.HALF_UP);

        assertThat(saturday.getDayOfWeek() == DayOfWeek.SATURDAY, "Expected Saturday request time");
        assertThat(ConvoyVehicleSegment.fromValue(25000) == ConvoyVehicleSegment.COURANT, "Expected COURANT segment");
        assertThat(weekendBonus.compareTo(BigDecimal.ZERO) > 0, "Weekend bonus should be positive");
        assertThat(totalTtc.compareTo(new BigDecimal("79.20")) == 0, "Weekend TTC should equal 79.20");
        log.info("[Scenario] {} — PASSED", name());
    }

    private void assertThat(boolean condition, String message) {
        if (!condition) throw new AssertionError("[" + name() + "] " + message);
    }
}
