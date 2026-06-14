package com.goweyy.convoyia.common.domain.records;

import com.goweyy.convoyia.common.domain.enums.MissionUrgency;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.Map;

@Value
@Builder
public class MissionRequest {
    String clientId;
    String vehicleId;
    String originAddress;
    String destinationAddress;
    LocalDateTime requestedAt;
    MissionUrgency urgency;
    Map<String, String> metadata;
    String tenantId;
}
