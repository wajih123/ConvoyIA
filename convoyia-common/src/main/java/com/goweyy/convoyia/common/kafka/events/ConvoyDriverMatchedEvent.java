package com.goweyy.convoyia.common.kafka.events;

import com.goweyy.convoyia.common.domain.enums.ConvoyBroadcastCircle;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class ConvoyDriverMatchedEvent {
    private String missionId;
    private String tenantId;
    private String driverId;
    private ConvoyBroadcastCircle circle;
    private long durationMs;
    private Instant occurredAt;
}
