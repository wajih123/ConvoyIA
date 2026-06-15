package com.goweyy.convoyia.dispatcher.convoy.broadcast;

import lombok.Data;

@Data
public class ConvoyAcceptRequest {
    private String missionId;
    private String driverId;
    private String tenantId;
}
