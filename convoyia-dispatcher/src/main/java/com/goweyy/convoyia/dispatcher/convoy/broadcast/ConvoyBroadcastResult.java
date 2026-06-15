package com.goweyy.convoyia.dispatcher.convoy.broadcast;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ConvoyBroadcastResult {
    private String missionId;
    private boolean accepted;
    private String assignedDriverId;
    private String circle;
    private int driversNotified;
    private String outcome;
}
