package com.goweyy.convoyia.common.kafka.events;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class ConvoyMissionCompletedEvent {
    private String missionId;
    private String tenantId;
    private BigDecimal totalTtc;
    private String currencyCode;
    private String clientInvoiceUrl;
    private String conveyorReceiptUrl;
    private Instant occurredAt;
}
