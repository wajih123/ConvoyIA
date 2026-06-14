package com.goweyy.convoyia.common.simulation.scenarios;

import com.goweyy.convoyia.common.simulation.ConvoyScenario;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ConvoyBillingCompletionScenario implements ConvoyScenario {

    @Override
    public String name() { return "ConvoyBillingCompletionScenario"; }

    @Override
    public void run() throws Exception {
        log.info("[Scenario] {} — run (placeholder)", name());
        // TODO: implement full scenario logic
    }
}
