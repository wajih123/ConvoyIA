package com.goweyy.convoyia.dispatcher.convoy.webhook;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class ConvoyWebhookEvent {
    private String missionId;
    private String tenantId;
    private ConvoyWebhookEventType eventType;
    private Object payload;
    private Instant occurredAt;
}
