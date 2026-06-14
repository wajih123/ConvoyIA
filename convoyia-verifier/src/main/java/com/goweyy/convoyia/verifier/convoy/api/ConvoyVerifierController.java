package com.goweyy.convoyia.verifier.convoy.api;

import com.goweyy.convoyia.verifier.convoy.agent.ConvoyVerifierAgent;
import com.goweyy.convoyia.verifier.convoy.dto.ConvoyVerificationRequest;
import com.goweyy.convoyia.verifier.convoy.dto.ConvoyVerificationResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/convoy/verify")
@RequiredArgsConstructor
public class ConvoyVerifierController {

    private final ConvoyVerifierAgent verifierAgent;

    @PostMapping
    public ResponseEntity<ConvoyVerificationResult> verify(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestBody ConvoyVerificationRequest request) {
        request.setTenantId(tenantId);
        return ResponseEntity.ok(verifierAgent.verify(request));
    }
}
