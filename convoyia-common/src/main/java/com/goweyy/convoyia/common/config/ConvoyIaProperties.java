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
    private Broadcast broadcast = new Broadcast();
    private Notifier notifier = new Notifier();
    private Signature signature = new Signature();

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

    @Data
    public static class Broadcast {
        private int circle1RadiusKm = 5;
        private int circle2RadiusKm = 15;
        private int circle3RadiusKm = 30;
        private int circleTimeoutSeconds = 30;
        private int driverHeartbeatTtlSeconds = 300;
    }

    @Data
    public static class Notifier {
        private String twilioAccountSid = "";
        private String twilioAuthToken = "";
        private String twilioFromNumber = "";
        private boolean smsEnabled = false;
    }

    @Data
    public static class Signature {
        private String yousignApiKey = "";
        private String yousignBaseUrl = "https://api.yousign.app/v3";
        private boolean enabled = false;
    }
}
