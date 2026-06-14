package com.goweyy.convoyia.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

@Data
@ConfigurationProperties(prefix = "convoyia")
public class ConvoyIaProperties {

    private Llm llm = new Llm();
    private Insurance insurance = new Insurance();
    private Simulation simulation = new Simulation();

    @Data
    public static class Llm {
        private String ollamaBaseUrl = "http://ollama-service:11434";
        private double confidenceThreshold = 0.72;
        private int timeoutSeconds = 8;
        private int visionTimeoutSeconds = 30;
    }

    @Data
    public static class Insurance {
        private BigDecimal hiscoxRcProAnnualCost = BigDecimal.ZERO;
        private int estimatedAnnualMissions = 500;
        private boolean vehicleCoverageEnabled = false;
    }

    @Data
    public static class Simulation {
        private boolean enabled = true;
        private boolean autoRunOnStartup = false;
    }
}
