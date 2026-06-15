package com.goweyy.convoyia.dispatcher.convoy.broadcast;

import com.goweyy.convoyia.common.domain.enums.ConvoyVehicleSegment;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/convoy/broadcast")
@RequiredArgsConstructor
public class ConvoyBroadcastController {

    private final ConvoyBroadcastAgent broadcastAgent;
    private final ConvoyDriverPoolService driverPoolService;

    @PostMapping("/accept")
    public ResponseEntity<ConvoyBroadcastResult> accept(@RequestBody ConvoyAcceptRequest request) {
        ConvoyBroadcastResult result = broadcastAgent.accept(request);
        return result.isAccepted() ? ResponseEntity.ok(result) : ResponseEntity.status(HttpStatus.CONFLICT).body(result);
    }

    @PostMapping("/heartbeat")
    public ResponseEntity<Void> heartbeat(@RequestBody ConvoyDriverAvailability availability) {
        driverPoolService.updateHeartbeat(
                availability.getDriverId(),
                availability.getTenantId(),
                availability.getLatitude(),
                availability.getLongitude(),
                availability.isAvailable(),
                availability.getSegments(),
                availability.getReputationScore(),
                availability.getFcmToken());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{missionId}/confirm-surge")
    public ResponseEntity<Void> confirmSurge(@PathVariable String missionId) {
        broadcastAgent.confirmSurge(missionId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/availability")
    public ResponseEntity<List<ConvoyDriverAvailability>> availability(@RequestParam String tenantId,
                                                                       @RequestParam ConvoyVehicleSegment segment,
                                                                       @RequestParam double originLat,
                                                                       @RequestParam double originLng,
                                                                       @RequestParam double radiusKm) {
        return ResponseEntity.ok(broadcastAgent.getAvailability(tenantId, segment, originLat, originLng, radiusKm));
    }

    @GetMapping("/{missionId}/status")
    public ResponseEntity<ConvoyBroadcastResult> status(@PathVariable String missionId) {
        return ResponseEntity.ok(broadcastAgent.getStatus(missionId));
    }
}
