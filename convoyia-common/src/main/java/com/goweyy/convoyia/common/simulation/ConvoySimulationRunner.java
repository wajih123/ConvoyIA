package com.goweyy.convoyia.common.simulation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConvoySimulationRunner {

    private final List<ConvoyScenario> scenarios;

    @Value("${convoyia.simulation.enabled:true}")
    private boolean simulationEnabled;

    public ConvoySimulationReport runAll() {
        if (!simulationEnabled) {
            log.info("Simulation disabled");
            return ConvoySimulationReport.builder()
                    .runId(UUID.randomUUID().toString())
                    .totalScenarios(0)
                    .passed(0).failed(0)
                    .results(Map.of()).errors(List.of())
                    .startedAt(Instant.now()).completedAt(Instant.now()).durationMs(0)
                    .build();
        }

        Instant start = Instant.now();
        String runId = UUID.randomUUID().toString();
        Map<String, String> results = new HashMap<>();
        List<String> errors = new ArrayList<>();
        int passed = 0, failed = 0;

        log.info("ConvoySimulationRunner starting {} scenarios", scenarios.size());

        for (ConvoyScenario scenario : scenarios) {
            try {
                scenario.run();
                results.put(scenario.name(), "PASSED");
                passed++;
                log.info("Scenario [{}] PASSED", scenario.name());
            } catch (Exception e) {
                results.put(scenario.name(), "FAILED: " + e.getMessage());
                errors.add(scenario.name() + ": " + e.getMessage());
                failed++;
                log.error("Scenario [{}] FAILED: {}", scenario.name(), e.getMessage());
            }
        }

        Instant end = Instant.now();
        return ConvoySimulationReport.builder()
                .runId(runId)
                .totalScenarios(scenarios.size())
                .passed(passed).failed(failed)
                .results(results).errors(errors)
                .startedAt(start).completedAt(end)
                .durationMs(end.toEpochMilli() - start.toEpochMilli())
                .build();
    }
}
