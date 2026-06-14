package com.goweyy.convoyia.tracker.api;

import com.goweyy.convoyia.tracker.agent.TrackerAgent;
import com.goweyy.convoyia.tracker.domain.GpsPosition;
import com.goweyy.convoyia.tracker.domain.TrackingEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/api/v1/convoy/track")
@RequiredArgsConstructor
public class TrackerController {

    private final TrackerAgent trackerAgent;

    @PostMapping("/{missionId}/position")
    public Mono<ResponseEntity<Void>> receivePosition(
            @PathVariable String missionId,
            @RequestBody GpsPosition position,
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantId) {
        GpsPosition positionWithMissionId = GpsPosition.builder()
                .missionId(missionId)
                .tenantId(tenantId != null ? tenantId : position.getTenantId())
                .conveyorId(position.getConveyorId())
                .latitude(position.getLatitude())
                .longitude(position.getLongitude())
                .speedKmh(position.getSpeedKmh())
                .timestamp(position.getTimestamp())
                .accuracy(position.getAccuracy())
                .build();

        return trackerAgent.processPosition(positionWithMissionId)
                .thenReturn(ResponseEntity.<Void>ok().build());
    }

    @GetMapping("/{missionId}/status")
    public Mono<ResponseEntity<java.util.Map<String, Object>>> getStatus(@PathVariable String missionId) {
        return Mono.just(ResponseEntity.ok(java.util.Map.of(
                "missionId", missionId,
                "status", "IN_PROGRESS"
        )));
    }

    @GetMapping(value = "/{missionId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<TrackingEvent>> streamEvents(@PathVariable String missionId) {
        return trackerAgent.streamEvents(missionId)
                .map(event -> ServerSentEvent.<TrackingEvent>builder()
                        .id(missionId)
                        .event(event.getType())
                        .data(event)
                        .build());
    }
}
