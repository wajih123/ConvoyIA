package com.goweyy.convoyia.verifier.convoy.integration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConvoyHiscoxAdapter {

    private final WebClient.Builder webClientBuilder;

    @Value("${hiscox.base-url:http://hiscox-api}")
    private String hiscoxBaseUrl;

    /**
     * Placeholder for Hiscox RC Pro coverage verification.
     * Returns placeholder coverage data until Hiscox API contract is finalised.
     * Insurance cost = BigDecimal.ZERO. TODO: fill after Hiscox contract signature.
     */
    public Map<String, Object> getCoverageStatus(String tenantId) {
        log.debug("ConvoyHiscoxAdapter getCoverageStatus tenantId={}", tenantId);
        // TODO: Call real Hiscox API when contract is signed
        return Map.of("covered", true, "provider", "Hiscox", "note", "placeholder");
    }
}
