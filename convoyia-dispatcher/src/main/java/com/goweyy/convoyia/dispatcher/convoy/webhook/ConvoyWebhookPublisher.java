package com.goweyy.convoyia.dispatcher.convoy.webhook;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;

/**
 * ConvoyWebhookPublisher — publishes platform events to tenant webhook endpoints.
 * Uses WebClient only for external HTTP calls (rule 2).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConvoyWebhookPublisher {

    private final WebClient.Builder webClientBuilder;

    public void publish(String webhookUrl, ConvoyWebhookEvent event) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            log.debug("No webhook URL for tenant={} event={}", event.getTenantId(), event.getEventType());
            return;
        }
        log.info("Publishing webhook event={} to url={}", event.getEventType(), webhookUrl);
        try {
            webClientBuilder.build()
                    .post()
                    .uri(webhookUrl)
                    .bodyValue(event)
                    .retrieve()
                    .toBodilessEntity()
                    .subscribe(
                            r -> log.debug("Webhook delivered event={}", event.getEventType()),
                            e -> log.warn("Webhook delivery failed event={}: {}", event.getEventType(), e.getMessage())
                    );
        } catch (Exception e) {
            log.warn("ConvoyWebhookPublisher error: {}", e.getMessage());
        }
    }
}
