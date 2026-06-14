package com.goweyy.convoyia.common.kafka.events;

import com.goweyy.convoyia.common.domain.enums.ConvoyInspectionPhase;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class ConvoyInspectionCompletedEvent {
    private String missionId;
    private String tenantId;
    private ConvoyInspectionPhase phase;
    private boolean damageDetected;
    private Instant occurredAt;
}
