package com.goweyy.convoyia.dispatcher.convoy.dispute;

import com.goweyy.convoyia.common.domain.enums.ConvoyDisputeType;
import lombok.Data;

import java.util.List;

@Data
public class ConvoyDisputeRequest {
    private String missionId;
    private ConvoyDisputeType disputeType;
    private String description;
    private List<String> evidenceUrls;
    private String openedBy;
}
