package com.goweyy.convoyia.biller.domain;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.Instant;

@Value
@Builder
public class BillingResult {
    String missionId;
    String tenantId;
    String status; // BILLED | PENDING_DAMAGE_REVIEW | REFUNDED
    String chargeId;
    String transferId;
    BigDecimal conveyorShare;
    BigDecimal platformShare;
    BigDecimal totalTtc;
    String clientInvoiceUrl;
    String conveyorReceiptUrl;
    Instant billedAt;
}
