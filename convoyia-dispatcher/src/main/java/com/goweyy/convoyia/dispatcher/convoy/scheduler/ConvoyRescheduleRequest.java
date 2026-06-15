package com.goweyy.convoyia.dispatcher.convoy.scheduler;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ConvoyRescheduleRequest {
    private String missionId;
    private LocalDateTime newDateTime;
    private String requestedBy;
}
