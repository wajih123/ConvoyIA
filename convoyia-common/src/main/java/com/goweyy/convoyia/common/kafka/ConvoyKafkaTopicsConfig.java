package com.goweyy.convoyia.common.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class ConvoyKafkaTopicsConfig {

    public static final String TOPIC_CONVOY_MISSION_DISPATCHED = "convoy.mission.dispatched";
    public static final String TOPIC_CONVOY_VERIFICATION_COMPLETED = "convoy.mission.verification.completed";
    public static final String TOPIC_CONVOY_PRICING_COMPLETED = "convoy.mission.pricing.completed";
    public static final String TOPIC_CONVOY_INSPECTION_COMPLETED = "convoy.mission.inspection.completed";
    public static final String TOPIC_CONVOY_MISSION_COMPLETED = "convoy.mission.completed";
    public static final String TOPIC_CONVOY_DRIVER_MATCHED = "convoy.driver.matched";
    public static final String TOPIC_CONVOY_MISSION_CANCELLED = "convoy.mission.cancelled";
    public static final String TOPIC_CONVOY_MISSION_RESCHEDULED = "convoy.mission.rescheduled";
    public static final String TOPIC_CONVOY_TRACKER_ANOMALY = "convoy.tracker.anomaly";
    public static final String TOPIC_CONVOY_WEBHOOKS_OUTBOUND = "convoy.webhooks.outbound";

    @Bean
    public NewTopic convoyMissionDispatched() {
        return TopicBuilder.name(TOPIC_CONVOY_MISSION_DISPATCHED).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic convoyVerificationCompleted() {
        return TopicBuilder.name(TOPIC_CONVOY_VERIFICATION_COMPLETED).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic convoyPricingCompleted() {
        return TopicBuilder.name(TOPIC_CONVOY_PRICING_COMPLETED).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic convoyInspectionCompleted() {
        return TopicBuilder.name(TOPIC_CONVOY_INSPECTION_COMPLETED).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic convoyMissionCompleted() {
        return TopicBuilder.name(TOPIC_CONVOY_MISSION_COMPLETED).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic convoyDriverMatched() {
        return TopicBuilder.name(TOPIC_CONVOY_DRIVER_MATCHED).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic convoyMissionCancelled() {
        return TopicBuilder.name(TOPIC_CONVOY_MISSION_CANCELLED).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic convoyMissionRescheduled() {
        return TopicBuilder.name(TOPIC_CONVOY_MISSION_RESCHEDULED).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic convoyTrackerAnomaly() {
        return TopicBuilder.name(TOPIC_CONVOY_TRACKER_ANOMALY).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic convoyWebhooksOutbound() {
        return TopicBuilder.name(TOPIC_CONVOY_WEBHOOKS_OUTBOUND).partitions(3).replicas(1).build();
    }
}
