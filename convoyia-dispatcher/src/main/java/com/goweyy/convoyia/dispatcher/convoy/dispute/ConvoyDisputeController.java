package com.goweyy.convoyia.dispatcher.convoy.dispute;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/convoy/dispute")
@RequiredArgsConstructor
public class ConvoyDisputeController {

    private final ConvoyDisputeAgent disputeAgent;

    @PostMapping("/open")
    public ResponseEntity<ConvoyDisputeResult> open(@RequestBody ConvoyDisputeRequest request) {
        return ResponseEntity.ok(disputeAgent.openDispute(request));
    }

    @GetMapping("/{disputeId}/status")
    public ResponseEntity<ConvoyDisputeResult> status(@PathVariable String disputeId) {
        return ResponseEntity.ok(disputeAgent.getStatus(disputeId));
    }

    @PostMapping("/{disputeId}/resolve")
    public ResponseEntity<ConvoyDisputeResult> resolve(@PathVariable String disputeId,
                                                       @RequestBody ConvoyAdminResolutionRequest request) {
        return ResponseEntity.ok(disputeAgent.resolveDispute(disputeId, request.getResolution(), request.getBy()));
    }

    @Data
    public static class ConvoyAdminResolutionRequest {
        private String resolution;
        private String by;
    }
}
