package com.goweyy.convoyia.biller.convoy.api;

import com.goweyy.convoyia.biller.convoy.agent.ConvoyBillerAgent;
import com.goweyy.convoyia.biller.convoy.dto.ConvoyBillingRequest;
import com.goweyy.convoyia.biller.convoy.dto.ConvoyBillingResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/convoy/bill")
@RequiredArgsConstructor
public class ConvoyBillerController {

    private final ConvoyBillerAgent billerAgent;

    @PostMapping
    public ResponseEntity<ConvoyBillingResult> bill(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestBody ConvoyBillingRequest request) {
        request.setTenantId(tenantId);
        return ResponseEntity.ok(billerAgent.bill(request));
    }
}
