package com.goweyy.convoyia.tracker.convoy.api;

import com.goweyy.convoyia.tracker.convoy.agent.ConvoyTrackerAgent;
import com.goweyy.convoyia.tracker.convoy.dto.ConvoyGpsPosition;
import com.goweyy.convoyia.tracker.convoy.dto.ConvoyTrackingEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/convoy/track")
@RequiredArgsConstructor
public class ConvoyTrackerController {

    private final ConvoyTrackerAgent trackerAgent;

    @PostMapping("/position")
    public ResponseEntity<ConvoyTrackingEvent> updatePosition(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestBody ConvoyGpsPosition position) {
        position.setTenantId(tenantId);
        return ResponseEntity.ok(trackerAgent.track(position));
    }
}
