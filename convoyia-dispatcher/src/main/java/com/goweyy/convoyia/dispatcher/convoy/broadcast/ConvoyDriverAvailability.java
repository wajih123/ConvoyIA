package com.goweyy.convoyia.dispatcher.convoy.broadcast;

import com.goweyy.convoyia.common.domain.enums.ConvoyVehicleSegment;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
public class ConvoyDriverAvailability {
    private String driverId;
    private String tenantId;
    private double latitude;
    private double longitude;
    private boolean available;
    private List<ConvoyVehicleSegment> segments;
    private double reputationScore;
    private String fcmToken;
    private Instant lastSeen;
    private double distanceKm;
}
