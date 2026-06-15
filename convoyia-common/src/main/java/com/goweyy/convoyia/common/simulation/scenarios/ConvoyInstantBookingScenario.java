package com.goweyy.convoyia.common.simulation.scenarios;

import com.goweyy.convoyia.common.domain.enums.ConvoyMissionType;
import com.goweyy.convoyia.common.repository.ConvoyMissionContext;
import com.goweyy.convoyia.common.simulation.ConvoyScenario;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Component
public class ConvoyInstantBookingScenario implements ConvoyScenario {

    @Override
    public String name() {
        return "ConvoyInstantBookingScenario";
    }

    @Override
    public void run() {
        LocalDateTime requestedAt = LocalDateTime.now().plusMinutes(30);
        ConvoyMissionType missionType = requestedAt.isBefore(LocalDateTime.now().plusHours(2))
                ? ConvoyMissionType.INSTANT : ConvoyMissionType.SCHEDULED;
        ConvoyMissionContext context = ConvoyMissionContext.builder()
                .missionId(UUID.randomUUID())
                .tenantId("goweyy")
                .missionType(missionType)
                .surgeMultiplier(BigDecimal.ONE)
                .build();
        assertThat(context.getMissionType() == ConvoyMissionType.INSTANT, "Mission type should be INSTANT");
        assertThat(BigDecimal.ONE.compareTo(context.getSurgeMultiplier()) == 0, "Surge multiplier should default to 1.00");
        log.info("[Scenario] {} — PASSED", name());
    }

    private void assertThat(boolean condition, String message) {
        if (!condition) throw new AssertionError("[" + name() + "] " + message);
    }
}
