package com.goweyy.convoyia.dispatcher.convoy.broadcast;

import com.goweyy.convoyia.common.domain.enums.ConvoyBroadcastCircle;
import com.goweyy.convoyia.common.domain.enums.ConvoyMissionState;
import com.goweyy.convoyia.common.kafka.ConvoyKafkaEventPublisher;
import com.goweyy.convoyia.common.kafka.ConvoyKafkaTopicsConfig;
import com.goweyy.convoyia.common.kafka.events.ConvoyDriverMatchedEvent;
import com.goweyy.convoyia.common.repository.ConvoyMissionContext;
import com.goweyy.convoyia.common.repository.ConvoyMissionContextRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConvoyBroadcastAgent {

    private static final String MISSION_LOCK_KEY = "convoy:mission:lock:";
    private static final BigDecimal SURGE_ONE = new BigDecimal("1.15");
    private static final BigDecimal SURGE_TWO = new BigDecimal("1.30");

    // TODO: replace in-memory maps with Redis SET NX semantics when spring-boot-starter-data-redis is added to pom.xml
    private static final Map<String, String> MISSION_LOCKS = new ConcurrentHashMap<>();
    private static final Map<String, String> SURGE_REQUESTS = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> SURGE_CONFIRMATIONS = new ConcurrentHashMap<>();
    private static final Map<String, ConvoyBroadcastResult> BROADCAST_RESULTS = new ConcurrentHashMap<>();

    private final ConvoyDriverPoolService driverPoolService;
    private final ConvoyKafkaEventPublisher kafkaEventPublisher;
    private final ConvoyMissionContextRepository missionContextRepository;

    public ConvoyBroadcastResult broadcast(UUID missionId) {
        ConvoyMissionContext context = missionContextRepository.findByMissionId(missionId)
                .orElseThrow(() -> new IllegalArgumentException("Mission context not found for missionId=" + missionId));
        context.setCurrentState(ConvoyMissionState.PENDING_BROADCAST);
        missionContextRepository.save(context);
        long startedAt = System.currentTimeMillis();

        for (ConvoyBroadcastCircle circle : List.of(ConvoyBroadcastCircle.CERCLE_1, ConvoyBroadcastCircle.CERCLE_2, ConvoyBroadcastCircle.CERCLE_3)) {
            ConvoyBroadcastResult result = notifyDrivers(context, circle, startedAt);
            if (result.isAccepted()) {
                return result;
            }
        }

        for (BigDecimal surgeMultiplier : List.of(SURGE_ONE, SURGE_TWO)) {
            SURGE_REQUESTS.put("convoy:surge:requested:" + missionId, surgeMultiplier.toPlainString());
            if (!waitForSurgeConfirmation(missionId, ConvoyBroadcastCircle.SURGE_1.getTimeoutSec())) {
                continue;
            }
            context.setSurgeMultiplier(surgeMultiplier);
            missionContextRepository.save(context);
            ConvoyBroadcastCircle surgeCircle = surgeMultiplier.compareTo(SURGE_ONE) == 0
                    ? ConvoyBroadcastCircle.SURGE_1 : ConvoyBroadcastCircle.SURGE_2;
            ConvoyBroadcastResult result = notifyDrivers(context, surgeCircle, startedAt);
            if (result.isAccepted()) {
                return result;
            }
        }

        return handleUnavailable(context);
    }

    public ConvoyBroadcastResult accept(ConvoyAcceptRequest request) {
        String lockKey = MISSION_LOCK_KEY + request.getMissionId();
        String existing = MISSION_LOCKS.putIfAbsent(lockKey, request.getDriverId());
        if (existing != null) {
            return ConvoyBroadcastResult.builder()
                    .missionId(request.getMissionId())
                    .accepted(false)
                    .assignedDriverId(existing)
                    .outcome("ALREADY_ASSIGNED")
                    .build();
        }

        ConvoyBroadcastResult result = BROADCAST_RESULTS.getOrDefault(request.getMissionId(), ConvoyBroadcastResult.builder()
                .missionId(request.getMissionId())
                .circle(ConvoyBroadcastCircle.CERCLE_1.name())
                .driversNotified(0)
                .build());
        result.setAccepted(true);
        result.setAssignedDriverId(request.getDriverId());
        result.setOutcome("ACCEPTED");
        BROADCAST_RESULTS.put(request.getMissionId(), result);

        missionContextRepository.findByMissionId(UUID.fromString(request.getMissionId())).ifPresent(context -> {
            context.setAssignedDriverId(request.getDriverId());
            context.setCurrentState(ConvoyMissionState.PRE_INSPECTION);
            missionContextRepository.save(context);
            kafkaEventPublisher.publishEvent(ConvoyDriverMatchedEvent.builder()
                            .missionId(request.getMissionId())
                            .tenantId(context.getTenantId())
                            .driverId(request.getDriverId())
                            .circle(ConvoyBroadcastCircle.valueOf(result.getCircle()))
                            .durationMs(0L)
                            .occurredAt(Instant.now())
                            .build(),
                    ConvoyKafkaTopicsConfig.TOPIC_CONVOY_DRIVER_MATCHED);
        });
        return result;
    }

    public void confirmSurge(String missionId) {
        SURGE_CONFIRMATIONS.put("convoy:surge:confirmed:" + missionId, Boolean.TRUE);
    }

    public List<ConvoyDriverAvailability> getAvailability(String tenantId, com.goweyy.convoyia.common.domain.enums.ConvoyVehicleSegment segment,
                                                          double originLat, double originLng, double radiusKm) {
        return driverPoolService.getAvailableInZone(tenantId, segment, originLat, originLng, radiusKm);
    }

    public ConvoyBroadcastResult getStatus(String missionId) {
        return BROADCAST_RESULTS.getOrDefault(missionId, ConvoyBroadcastResult.builder()
                .missionId(missionId)
                .accepted(isAccepted(missionId))
                .assignedDriverId(MISSION_LOCKS.get(MISSION_LOCK_KEY + missionId))
                .outcome("PENDING")
                .build());
    }

    public boolean isAccepted(String missionId) {
        return MISSION_LOCKS.containsKey(MISSION_LOCK_KEY + missionId);
    }

    public boolean waitForSurgeConfirmation(UUID missionId, int timeoutSec) {
        String confirmationKey = "convoy:surge:confirmed:" + missionId;
        long deadline = System.currentTimeMillis() + timeoutSec * 1000L;
        while (System.currentTimeMillis() < deadline) {
            if (Boolean.TRUE.equals(SURGE_CONFIRMATIONS.remove(confirmationKey))) {
                return true;
            }
            try {
                Thread.sleep(500L);
            } catch (InterruptedException interruptedException) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    private ConvoyBroadcastResult notifyDrivers(ConvoyMissionContext context, ConvoyBroadcastCircle circle, long startedAt) {
        List<ConvoyDriverAvailability> drivers = driverPoolService.getAvailableInZone(
                context.getTenantId(), context.getVehicleSegment(), 0.0d, 0.0d, circle.getRadiusKm());
        for (ConvoyDriverAvailability driver : drivers) {
            log.warn("Broadcasting mission payload missionId={} driverId={} circle={} surgeMultiplier={}",
                    context.getMissionId(), driver.getDriverId(), circle, context.getSurgeMultiplier());
            // TODO: inject FcmPushService from Nevyo and call fcmPushService.send()
        }
        ConvoyBroadcastResult result = ConvoyBroadcastResult.builder()
                .missionId(context.getMissionId().toString())
                .accepted(isAccepted(context.getMissionId().toString()))
                .assignedDriverId(MISSION_LOCKS.get(MISSION_LOCK_KEY + context.getMissionId()))
                .circle(circle.name())
                .driversNotified(drivers.size())
                .outcome(drivers.isEmpty() ? "NO_DRIVERS_IN_ZONE" : "WAITING_ACCEPTANCE")
                .build();
        BROADCAST_RESULTS.put(context.getMissionId().toString(), result);
        long deadline = System.currentTimeMillis() + circle.getTimeoutSec() * 1000L;
        while (System.currentTimeMillis() < deadline) {
            if (isAccepted(context.getMissionId().toString())) {
                String driverId = MISSION_LOCKS.get(MISSION_LOCK_KEY + context.getMissionId());
                result.setAccepted(true);
                result.setAssignedDriverId(driverId);
                result.setOutcome("ACCEPTED");
                return result;
            }
            try {
                Thread.sleep(500L);
            } catch (InterruptedException interruptedException) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return result;
    }

    private ConvoyBroadcastResult handleUnavailable(ConvoyMissionContext context) {
        context.setCurrentState(ConvoyMissionState.UNAVAILABLE);
        missionContextRepository.save(context);
        log.warn("No driver available for missionId={}", context.getMissionId());
        ConvoyBroadcastResult result = ConvoyBroadcastResult.builder()
                .missionId(context.getMissionId().toString())
                .accepted(false)
                .driversNotified(0)
                .outcome("UNAVAILABLE")
                .build();
        BROADCAST_RESULTS.put(context.getMissionId().toString(), result);
        return result;
    }
}
