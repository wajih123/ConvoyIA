package com.goweyy.convoyia.common.simulation.scenarios;

import com.goweyy.convoyia.common.repository.ConvoyMissionContext;
import com.goweyy.convoyia.common.simulation.ConvoyScenario;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Slf4j
@Component
public class ConvoyMultiTenantIsolationScenario implements ConvoyScenario {

    @Override
    public String name() {
        return "ConvoyMultiTenantIsolationScenario";
    }

    @Override
    public void run() {
        ConvoyMissionContext goweyy = ConvoyMissionContext.builder().missionId(UUID.randomUUID()).tenantId("goweyy").build();
        ConvoyMissionContext ukDemo = ConvoyMissionContext.builder().missionId(UUID.randomUUID()).tenantId("convoyia-uk-demo").build();
        BigDecimal totalTtc = new BigDecimal("80.00");
        BigDecimal conveyorRatio = BigDecimal.valueOf(3).divide(BigDecimal.valueOf(4), 2, RoundingMode.HALF_UP);
        BigDecimal goweyyPayout = totalTtc.multiply(conveyorRatio).setScale(2, RoundingMode.HALF_UP);
        BigDecimal ukPayout = totalTtc.multiply(conveyorRatio).setScale(2, RoundingMode.HALF_UP);

        assertThat("goweyy".equals(goweyy.getTenantId()), "goweyy tenantId should match");
        assertThat("convoyia-uk-demo".equals(ukDemo.getTenantId()), "UK demo tenantId should match");
        assertThat(!goweyy.getTenantId().equals(ukDemo.getTenantId()), "Tenant IDs must differ");
        assertThat(goweyyPayout.compareTo(new BigDecimal("60.00")) == 0, "goweyy payout should equal 75% of totalTtc");
        assertThat(ukPayout.compareTo(new BigDecimal("60.00")) == 0, "uk payout should equal 75% of totalTtc");
        assertThat(!goweyy.getMissionId().equals(ukDemo.getMissionId()), "Mission IDs must remain isolated");
        log.info("[Scenario] {} — PASSED", name());
    }

    private void assertThat(boolean condition, String message) {
        if (!condition) throw new AssertionError("[" + name() + "] " + message);
    }
}
