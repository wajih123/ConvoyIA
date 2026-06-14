package com.goweyy.convoyia.biller.api;

import com.goweyy.convoyia.biller.agent.BillerAgent;
import com.goweyy.convoyia.biller.domain.BillingRequest;
import com.goweyy.convoyia.biller.domain.BillingResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/api/v1/convoy/bill")
@RequiredArgsConstructor
public class BillerController {

    private final BillerAgent billerAgent;

    @PostMapping
    public Mono<ResponseEntity<BillingResult>> bill(
            @RequestBody BillingRequest request,
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantId) {
        return billerAgent.bill(request)
                .map(ResponseEntity::ok)
                .doOnError(e -> log.error("Billing failed: {}", e.getMessage()));
    }
}
