package com.goweyy.convoyia.dispatcher.convoy.scheduler;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ConvoyScheduleResult {
    private String missionId;
    private String outcome;
    private BigDecimal refundAmount;
    private String currency;
    private String message;
}
