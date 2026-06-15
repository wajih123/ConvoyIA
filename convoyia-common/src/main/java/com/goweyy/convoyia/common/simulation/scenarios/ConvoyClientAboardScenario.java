package com.goweyy.convoyia.common.simulation.scenarios;

import com.goweyy.convoyia.common.domain.enums.ConvoyMissionState;
import com.goweyy.convoyia.common.domain.enums.ConvoyMissionType;
import com.goweyy.convoyia.common.repository.ConvoyMissionContext;
import com.goweyy.convoyia.common.simulation.ConvoyScenario;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
public class ConvoyClientAboardScenario implements ConvoyScenario {

    @Override
    public String name() {
        return "ConvoyClientAboardScenario";
    }

    @Override
    public void run() {
        ConvoyMissionContext context = ConvoyMissionContext.builder()
                .missionId(UUID.randomUUID())
                .tenantId("goweyy")
                .clientAboard(true)
                .missionType(ConvoyMissionType.SCHEDULED)
                .currentState(ConvoyMissionState.RECEIVED)
                .build();
        context.setCurrentState(ConvoyMissionState.PRE_INSPECTION);
        assertThat(context.isClientAboard(), "clientAboard should stay true");
        assertThat(context.getCurrentState() == ConvoyMissionState.PRE_INSPECTION, "State should proceed normally");
        log.info("[Scenario] {} — PASSED", name());
    }

    private void assertThat(boolean condition, String message) {
        if (!condition) throw new AssertionError("[" + name() + "] " + message);
    }
}
