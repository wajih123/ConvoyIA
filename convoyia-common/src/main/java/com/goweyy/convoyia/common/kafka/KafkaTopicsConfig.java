package com.goweyy.convoyia.common.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicsConfig {

    public static final String TOPIC_MISSION_DISPATCH_COMPLETED = "mission.dispatch.completed";
    public static final String TOPIC_MISSION_VERIFICATION_COMPLETED = "mission.verification.completed";
    public static final String TOPIC_MISSION_PRICING_COMPLETED = "mission.pricing.completed";
    public static final String TOPIC_MISSION_INSPECTION_COMPLETED = "mission.inspection.completed";
    public static final String TOPIC_MISSION_COMPLETED = "mission.completed";
    public static final String TOPIC_MISSION_TRACKER_ANOMALY = "mission.tracker.anomaly";

    @Bean
    public NewTopic missionDispatchCompleted() {
        return TopicBuilder.name(TOPIC_MISSION_DISPATCH_COMPLETED).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic missionVerificationCompleted() {
        return TopicBuilder.name(TOPIC_MISSION_VERIFICATION_COMPLETED).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic missionPricingCompleted() {
        return TopicBuilder.name(TOPIC_MISSION_PRICING_COMPLETED).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic missionInspectionCompleted() {
        return TopicBuilder.name(TOPIC_MISSION_INSPECTION_COMPLETED).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic missionCompleted() {
        return TopicBuilder.name(TOPIC_MISSION_COMPLETED).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic missionTrackerAnomaly() {
        return TopicBuilder.name(TOPIC_MISSION_TRACKER_ANOMALY).partitions(3).replicas(1).build();
    }
}
