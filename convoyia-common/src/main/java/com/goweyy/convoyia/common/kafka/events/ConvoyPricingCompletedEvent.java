package com.goweyy.convoyia.common.kafka.events;

import com.goweyy.convoyia.common.domain.enums.ConvoyPricingStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class ConvoyPricingCompletedEvent {
    private String missionId;
    private String tenantId;
    private ConvoyPricingStatus status;
    private BigDecimal totalTtc;
    private String currencyCode;
    private Instant occurredAt;
}
