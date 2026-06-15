package com.goweyy.convoyia.common.kafka.events;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class ConvoyMissionCancelledEvent {
    private String missionId;
    private String tenantId;
    private String requestedBy;
    private BigDecimal refundAmount;
    private String currencyCode;
    private Instant occurredAt;
}
