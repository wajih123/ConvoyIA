package com.goweyy.convoyia.dispatcher.convoy.broadcast;

import com.goweyy.convoyia.common.domain.enums.ConvoyVehicleSegment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class ConvoyDriverPoolService {

    // TODO: replace with Redis when spring-boot-starter-data-redis is added to pom.xml
    private static final Map<String, ConvoyDriverAvailability> DRIVER_POOL = new ConcurrentHashMap<>();
    private static final int HEARTBEAT_TTL_SECONDS = 300;

    public void updateHeartbeat(String driverId, String tenantId, double lat, double lng,
                                boolean available, List<ConvoyVehicleSegment> segments,
                                double reputationScore, String fcmToken) {
        String key = tenantId + ':' + driverId;
        DRIVER_POOL.put(key, ConvoyDriverAvailability.builder()
                .driverId(driverId)
                .tenantId(tenantId)
                .latitude(lat)
                .longitude(lng)
                .available(available)
                .segments(segments)
                .reputationScore(reputationScore)
                .fcmToken(fcmToken)
                .lastSeen(Instant.now())
                .build());
    }

    public List<ConvoyDriverAvailability> getAvailableInZone(String tenantId, ConvoyVehicleSegment segment,
                                                             double originLat, double originLng, double radiusKm) {
        List<ConvoyDriverAvailability> result = new ArrayList<>();
        Instant cutoff = Instant.now().minusSeconds(HEARTBEAT_TTL_SECONDS);
        for (ConvoyDriverAvailability availability : DRIVER_POOL.values()) {
            if (!tenantId.equals(availability.getTenantId()) || !availability.isAvailable()) {
                continue;
            }
            if (availability.getSegments() == null || !availability.getSegments().contains(segment)) {
                continue;
            }
            if (availability.getReputationScore() < 3.0d) {
                continue;
            }
            if (availability.getLastSeen() == null || availability.getLastSeen().isBefore(cutoff)) {
                continue;
            }
            double distance = haversine(originLat, originLng, availability.getLatitude(), availability.getLongitude());
            if (distance > radiusKm) {
                continue;
            }
            availability.setDistanceKm(distance);
            result.add(availability);
        }
        result.sort(Comparator.comparingDouble(ConvoyDriverAvailability::getDistanceKm));
        return result;
    }

    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        final int earthRadiusKm = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadiusKm * c;
    }
}
