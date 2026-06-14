package com.goweyy.convoyia.verifier.api;

import com.goweyy.convoyia.common.domain.records.VerificationRequest;
import com.goweyy.convoyia.common.domain.records.VerificationResult;
import com.goweyy.convoyia.verifier.agent.VerifierAgent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/api/v1/convoy/verify")
@RequiredArgsConstructor
public class VerifierController {

    private final VerifierAgent verifierAgent;

    @PostMapping
    public Mono<ResponseEntity<VerificationResult>> verify(
            @RequestBody VerificationRequest request,
            @RequestHeader(value = "X-Tenant-Id", required = true) String tenantId) {
        return verifierAgent.verify(request)
                .map(ResponseEntity::ok)
                .doOnError(e -> log.error("Verification failed: {}", e.getMessage()));
    }

    @PostMapping("/internal")
    public Mono<ResponseEntity<VerificationResult>> verifyInternal(
            @RequestBody VerificationRequest request) {
        return verifierAgent.verify(request)
                .map(ResponseEntity::ok)
                .doOnError(e -> log.error("Internal verification failed: {}", e.getMessage()));
    }
}
