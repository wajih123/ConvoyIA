package com.goweyy.convoyia.inspector.convoy.api;

import com.goweyy.convoyia.inspector.convoy.agent.ConvoyInspectorAgent;
import com.goweyy.convoyia.inspector.convoy.dto.ConvoyInspectionRequest;
import com.goweyy.convoyia.inspector.convoy.dto.ConvoyInspectionResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/convoy/inspect")
@RequiredArgsConstructor
public class ConvoyInspectorController {

    private final ConvoyInspectorAgent inspectorAgent;

    @PostMapping
    public ResponseEntity<ConvoyInspectionResult> inspect(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestBody ConvoyInspectionRequest request) {
        request.setTenantId(tenantId);
        return ResponseEntity.ok(inspectorAgent.inspect(request));
    }
}
