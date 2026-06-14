package com.goweyy.convoyia.common.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConvoyKafkaEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishEvent(Object event, String topic) {
        log.info("Publishing convoy event to topic={} type={}", topic, event.getClass().getSimpleName());
        try {
            kafkaTemplate.send(topic, event).get();
        } catch (Exception e) {
            log.error("Failed to publish convoy event to topic={} type={}: {}",
                    topic, event.getClass().getSimpleName(), e.getMessage());
        }
    }
}
