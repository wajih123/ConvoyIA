package com.goweyy.convoyia.common.simulation;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class ConvoySimulationReport {
    private String runId;
    private int totalScenarios;
    private int passed;
    private int failed;
    private Map<String, String> results;
    private List<String> errors;
    private Instant startedAt;
    private Instant completedAt;
    private long durationMs;
}
