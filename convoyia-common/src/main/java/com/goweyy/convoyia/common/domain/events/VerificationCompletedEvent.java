package com.goweyy.convoyia.common.domain.events;

import com.goweyy.convoyia.common.domain.enums.VerificationStatus;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class VerificationCompletedEvent {
    String missionId;
    String tenantId;
    VerificationStatus status;
    Instant occurredAt;
}
