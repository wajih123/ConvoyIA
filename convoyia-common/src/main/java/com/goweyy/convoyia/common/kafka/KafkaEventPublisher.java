package com.goweyy.convoyia.common.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public Mono<Void> publishEvent(Object event, String topic) {
        return Mono.fromCallable(() -> {
                    log.info("Publishing event to topic={} type={}", topic, event.getClass().getSimpleName());
                    kafkaTemplate.send(topic, event);
                    return null;
                })
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }
}
