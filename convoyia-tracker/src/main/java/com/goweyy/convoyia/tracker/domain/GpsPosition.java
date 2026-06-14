package com.goweyy.convoyia.tracker.domain;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class GpsPosition {
    String missionId;
    String tenantId;
    String conveyorId;
    double latitude;
    double longitude;
    double speedKmh;
    Instant timestamp;
    double accuracy;
}
