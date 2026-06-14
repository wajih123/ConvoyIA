package com.goweyy.convoyia.biller.domain;

import com.goweyy.convoyia.common.domain.enums.VehicleSegment;
import com.goweyy.convoyia.pricer.domain.PricingResult;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class BillingRequest {
    String missionId;
    String tenantId;
    String conveyorId;
    String conveyorStripeAccountId;
    String clientId;
    String paymentIntentId;
    PricingResult pricingResult;
    boolean damageDetected;
    VehicleSegment vehicleSegment;
}
