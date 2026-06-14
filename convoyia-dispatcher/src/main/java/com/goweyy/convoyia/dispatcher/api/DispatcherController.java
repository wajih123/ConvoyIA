package com.goweyy.convoyia.dispatcher.api;

import com.goweyy.convoyia.common.domain.records.DispatchDecision;
import com.goweyy.convoyia.common.domain.records.MissionContext;
import com.goweyy.convoyia.common.domain.records.MissionRequest;
import com.goweyy.convoyia.dispatcher.agent.DispatcherAgent;
import com.goweyy.convoyia.dispatcher.repository.MissionContextRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/api/v1/convoy/dispatch")
@RequiredArgsConstructor
public class DispatcherController {

    private final DispatcherAgent dispatcherAgent;
    private final MissionContextRepository contextRepository;

    @PostMapping
    public Mono<ResponseEntity<DispatchDecision>> dispatch(
            @Valid @RequestBody MissionRequest request,
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantId) {

        MissionRequest enrichedRequest = tenantId != null
                ? MissionRequest.builder()
                .clientId(request.getClientId())
                .vehicleId(request.getVehicleId())
                .originAddress(request.getOriginAddress())
                .destinationAddress(request.getDestinationAddress())
                .requestedAt(request.getRequestedAt())
                .urgency(request.getUrgency())
                .metadata(request.getMetadata())
                .tenantId(tenantId)
                .build()
                : request;

        return dispatcherAgent.dispatch(enrichedRequest)
                .map(ResponseEntity::ok)
                .doOnError(e -> log.error("Dispatch failed: {}", e.getMessage()));
    }

    @GetMapping("/{missionId}/state")
    public Mono<ResponseEntity<MissionContext>> getMissionState(
            @PathVariable String missionId,
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantId) {
        return contextRepository.findByMissionId(missionId)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}
