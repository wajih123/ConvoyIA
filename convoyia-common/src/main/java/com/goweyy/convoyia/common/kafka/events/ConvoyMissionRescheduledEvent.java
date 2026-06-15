package com.goweyy.convoyia.common.kafka.events;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

@Data
@Builder
public class ConvoyMissionRescheduledEvent {
    private String missionId;
    private String tenantId;
    private String requestedBy;
    private LocalDateTime newDateTime;
    private BigDecimal rescheduleFee;
    private Instant occurredAt;
}
