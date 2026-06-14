package com.goweyy.convoyia.dispatcher.convoy.api;

import com.goweyy.convoyia.dispatcher.convoy.agent.ConvoyDispatcherAgent;
import com.goweyy.convoyia.dispatcher.convoy.dto.ConvoyDispatchDecision;
import com.goweyy.convoyia.dispatcher.convoy.dto.ConvoyDispatchRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/convoy/dispatch")
@RequiredArgsConstructor
public class ConvoyDispatcherController {

    private final ConvoyDispatcherAgent dispatcherAgent;

    @PostMapping
    public ResponseEntity<ConvoyDispatchDecision> dispatch(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestBody ConvoyDispatchRequest request) {
        request.setTenantId(tenantId);
        ConvoyDispatchDecision decision = dispatcherAgent.dispatch(request);
        return ResponseEntity.ok(decision);
    }
}
