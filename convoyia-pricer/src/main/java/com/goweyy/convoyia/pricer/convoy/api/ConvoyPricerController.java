package com.goweyy.convoyia.pricer.convoy.api;

import com.goweyy.convoyia.pricer.convoy.agent.ConvoyPricerAgent;
import com.goweyy.convoyia.pricer.convoy.dto.ConvoyPricingRequest;
import com.goweyy.convoyia.pricer.convoy.dto.ConvoyPricingResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/convoy/price")
@RequiredArgsConstructor
public class ConvoyPricerController {

    private final ConvoyPricerAgent pricerAgent;

    @PostMapping
    public ResponseEntity<ConvoyPricingResult> price(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestBody ConvoyPricingRequest request) {
        request.setTenantId(tenantId);
        return ResponseEntity.ok(pricerAgent.price(request));
    }
}
