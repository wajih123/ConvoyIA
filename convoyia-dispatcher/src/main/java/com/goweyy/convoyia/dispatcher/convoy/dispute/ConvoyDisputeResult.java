package com.goweyy.convoyia.dispatcher.convoy.dispute;

import com.goweyy.convoyia.common.domain.enums.ConvoyDisputeType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ConvoyDisputeResult {
    private String disputeId;
    private String missionId;
    private ConvoyDisputeType type;
    private String outcome;
    private BigDecimal refundAmount;
    private BigDecimal penaltyAmount;
    private String resolution;
    private boolean escalated;
}
