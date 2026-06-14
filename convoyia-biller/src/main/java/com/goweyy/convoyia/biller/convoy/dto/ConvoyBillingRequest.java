package com.goweyy.convoyia.biller.convoy.dto;

import com.goweyy.convoyia.pricer.convoy.dto.ConvoyPricingResult;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ConvoyBillingRequest {
    private String missionId;
    private String tenantId;
    private String paymentIntentId;
    private String conveyorStripeAccountId;
    private ConvoyPricingResult pricingResult;
    private boolean damageDetected;
}
