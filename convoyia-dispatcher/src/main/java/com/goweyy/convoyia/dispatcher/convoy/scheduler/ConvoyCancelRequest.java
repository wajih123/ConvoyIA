package com.goweyy.convoyia.dispatcher.convoy.scheduler;

import lombok.Data;

@Data
public class ConvoyCancelRequest {
    private String missionId;
    private String reason;
    private String requestedBy;
}
