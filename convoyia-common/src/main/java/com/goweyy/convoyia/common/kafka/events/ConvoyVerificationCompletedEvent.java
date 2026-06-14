package com.goweyy.convoyia.common.kafka.events;

import com.goweyy.convoyia.common.domain.enums.ConvoyVerificationStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class ConvoyVerificationCompletedEvent {
    private String missionId;
    private String tenantId;
    private ConvoyVerificationStatus status;
    private Instant occurredAt;
}
