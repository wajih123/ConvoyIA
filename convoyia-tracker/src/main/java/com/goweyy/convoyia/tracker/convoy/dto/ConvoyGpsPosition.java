package com.goweyy.convoyia.tracker.convoy.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class ConvoyGpsPosition {
    private String missionId;
    private String tenantId;
    private String driverId;
    private double latitude;
    private double longitude;
    private double speedKmh;
    private Instant recordedAt;
}
