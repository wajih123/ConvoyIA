package com.goweyy.convoyia.biller.convoy.dto;

import com.goweyy.convoyia.common.domain.enums.ConvoyBillingStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class ConvoyBillingResult {
    private String missionId;
    private String tenantId;
    private ConvoyBillingStatus status;
    private String chargeId;
    private String transferId;
    private BigDecimal conveyorShare;
    private BigDecimal platformShare;
    private BigDecimal totalTtc;
    private String clientInvoiceUrl;
    private String conveyorReceiptUrl;
    private Instant billedAt;
}
