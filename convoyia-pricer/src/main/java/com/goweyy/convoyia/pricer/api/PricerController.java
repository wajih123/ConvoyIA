package com.goweyy.convoyia.pricer.api;

import com.goweyy.convoyia.pricer.agent.PricerAgent;
import com.goweyy.convoyia.pricer.domain.PricingRequest;
import com.goweyy.convoyia.pricer.domain.PricingResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/api/v1/convoy/price")
@RequiredArgsConstructor
public class PricerController {

    private final PricerAgent pricerAgent;

    @PostMapping
    public Mono<ResponseEntity<PricingResult>> price(
            @RequestBody PricingRequest request,
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantId) {
        return pricerAgent.price(request)
                .map(ResponseEntity::ok)
                .doOnError(e -> log.error("Pricing failed: {}", e.getMessage()));
    }
}
