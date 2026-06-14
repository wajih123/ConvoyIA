package com.goweyy.convoyia.inspector.api;

import com.goweyy.convoyia.inspector.agent.InspectorAgent;
import com.goweyy.convoyia.inspector.domain.InspectionRequest;
import com.goweyy.convoyia.inspector.domain.InspectionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/api/v1/convoy/inspect")
@RequiredArgsConstructor
public class InspectorController {

    private final InspectorAgent inspectorAgent;

    @PostMapping
    public Mono<ResponseEntity<InspectionResult>> inspect(
            @RequestBody InspectionRequest request,
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantId) {
        return inspectorAgent.inspect(request)
                .map(ResponseEntity::ok)
                .doOnError(e -> log.error("Inspection failed: {}", e.getMessage()));
    }
}
