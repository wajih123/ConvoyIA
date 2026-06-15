package com.goweyy.convoyia.verifier.convoy.agent;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.goweyy.convoyia.common.domain.enums.ConvoyAlertSeverity;
import com.goweyy.convoyia.common.domain.enums.ConvoyMissionState;
import com.goweyy.convoyia.common.domain.enums.ConvoyMissionType;
import com.goweyy.convoyia.common.domain.enums.ConvoyPricingStatus;
import com.goweyy.convoyia.common.domain.enums.ConvoyVerificationStatus;
import com.goweyy.convoyia.common.domain.tenant.ConvoyTenantConfig;
import com.goweyy.convoyia.common.domain.tenant.ConvoyTenantConfigRepository;
import com.goweyy.convoyia.common.kafka.ConvoyKafkaEventPublisher;
import com.goweyy.convoyia.common.kafka.ConvoyKafkaTopicsConfig;
import com.goweyy.convoyia.common.kafka.events.ConvoyPricingCompletedEvent;
import com.goweyy.convoyia.common.kafka.events.ConvoyVerificationCompletedEvent;
import com.goweyy.convoyia.common.repository.ConvoyMissionContext;
import com.goweyy.convoyia.common.repository.ConvoyMissionContextRepository;
import com.goweyy.convoyia.verifier.convoy.dto.ConvoyVerificationAlert;
import com.goweyy.convoyia.verifier.convoy.dto.ConvoyVerificationBlock;
import com.goweyy.convoyia.verifier.convoy.dto.ConvoyVerificationRequest;
import com.goweyy.convoyia.verifier.convoy.dto.ConvoyVerificationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConvoyVerifierAgent {

    private final ConvoyDriverVerifier driverVerifier;
    private final ConvoyVehicleVerifier vehicleVerifier;
    private final ConvoyMissionVerifier missionVerifier;
    private final ConvoyMissionContextRepository missionContextRepository;
    private final ConvoyKafkaEventPublisher kafkaEventPublisher;
    private final ObjectMapper objectMapper;
    private final ConvoyTenantConfigRepository tenantConfigRepository;

    public ConvoyVerificationResult verify(ConvoyVerificationRequest request) {
        log.info("ConvoyVerifierAgent verifying missionId={}", request.getMissionId());
        List<ConvoyVerificationBlock> blocks;
        if (isInstantMission(request.getMissionId())) {
            log.info("EXPRESS_VERIFICATION mode missionId={}", request.getMissionId());
            blocks = List.of(driverVerifier.verifyExpress(request));
        } else {
            CompletableFuture<ConvoyVerificationBlock> driverFuture = CompletableFuture.supplyAsync(() -> driverVerifier.verify(request));
            CompletableFuture<ConvoyVerificationBlock> vehicleFuture = CompletableFuture.supplyAsync(() -> vehicleVerifier.verify(request));
            CompletableFuture<ConvoyVerificationBlock> missionFuture = CompletableFuture.supplyAsync(() -> missionVerifier.verify(request));
            blocks = List.of(driverFuture.join(), vehicleFuture.join(), missionFuture.join());
        }

        List<ConvoyVerificationAlert> alerts = blocks.stream()
                .flatMap(block -> block.getAlerts().stream())
                .toList();
        ConvoyVerificationStatus status = worstOf(blocks.stream().map(this::statusOf).toList());
        updateMissionState(request.getMissionId(), status);

        ConvoyVerificationResult result = ConvoyVerificationResult.builder()
                .missionId(request.getMissionId())
                .tenantId(request.getTenantId())
                .status(status)
                .blocks(blocks)
                .alerts(alerts)
                .verifiedAt(Instant.now())
                .build();

        kafkaEventPublisher.publishEvent(ConvoyVerificationCompletedEvent.builder()
                        .missionId(request.getMissionId())
                        .tenantId(request.getTenantId())
                        .status(status)
                        .occurredAt(Instant.now())
                        .build(),
                ConvoyKafkaTopicsConfig.TOPIC_CONVOY_VERIFICATION_COMPLETED);
        return result;
    }

    @KafkaListener(topics = ConvoyKafkaTopicsConfig.TOPIC_CONVOY_PRICING_COMPLETED,
            groupId = "${spring.kafka.consumer.group-id:convoy-verifier}")
    public void onPricingCompleted(ConvoyPricingCompletedEvent event) {
        if (event.getStatus() != ConvoyPricingStatus.PRICED) {
            log.info("Skipping verification for missionId={} pricingStatus={}", event.getMissionId(), event.getStatus());
            return;
        }
        missionContextRepository.findByMissionId(UUID.fromString(event.getMissionId())).ifPresentOrElse(context -> {
            ConvoyVerificationRequest request = buildRequest(context);
            verify(request);
        }, () -> log.warn("Mission context not found for pricing event missionId={}", event.getMissionId()));
    }

    private ConvoyVerificationRequest buildRequest(ConvoyMissionContext context) {
        Map<String, Object> enriched = parseEnrichedData(context.getEnrichedData());
        ConvoyTenantConfig tenantConfig = tenantConfigRepository.findActiveByTenantId(context.getTenantId()).block();
        return ConvoyVerificationRequest.builder()
                .missionId(context.getMissionId().toString())
                .tenantId(context.getTenantId())
                .driverId(valueAsString(enriched, "driverId", context.getAssignedDriverId()))
                .vehiclePlate(valueAsString(enriched, "vehiclePlate", null))
                .vehicleBrand(valueAsString(enriched, "vehicleBrand", null))
                .vehicleModel(valueAsString(enriched, "vehicleModel", null))
                .vehicleDeclaredValue(valueAsDouble(enriched, "vehicleDeclaredValue"))
                .licenseCategories(valueAsString(enriched, "licenseCategories", null))
                .licenseExpiryDate(valueAsLocalDate(enriched, "licenseExpiryDate"))
                .backgroundCheckDate(valueAsLocalDate(enriched, "backgroundCheckDate"))
                .habilitationLuxe(valueAsBoolean(enriched, "habilitationLuxe"))
                .driverReputationScore(valueAsDouble(enriched, "driverReputationScore"))
                .keycloakSessionId(valueAsString(enriched, "keycloakSessionId", null))
                .carteGriseUrl(valueAsString(enriched, "carteGriseUrl", null))
                .assuranceExpiryDate(valueAsLocalDate(enriched, "assuranceExpiryDate"))
                .controleTechniqueDate(valueAsLocalDate(enriched, "controleTechniqueDate"))
                .requestedAt(valueAsLocalDateTime(enriched, "requestedAt", context.getCreatedAt()))
                .backgroundCheckDocName(tenantConfig != null ? tenantConfig.getBackgroundCheckDocName() : null)
                .backgroundCheckMaxAgeDays(tenantConfig != null && tenantConfig.getBackgroundCheckMaxAgeDays() != null
                        ? tenantConfig.getBackgroundCheckMaxAgeDays() : 0)
                .insuranceCeilingAmount(tenantConfig != null ? tenantConfig.getInsuranceCeilingAmount() : BigDecimal.ZERO)
                .originAddress(context.getOriginAddress())
                .destinationAddress(context.getDestinationAddress())
                .build();
    }

    private boolean isInstantMission(String missionId) {
        return missionContextRepository.findByMissionId(UUID.fromString(missionId))
                .map(ConvoyMissionContext::getMissionType)
                .filter(ConvoyMissionType.INSTANT::equals)
                .isPresent();
    }

    private void updateMissionState(String missionId, ConvoyVerificationStatus status) {
        missionContextRepository.findByMissionId(UUID.fromString(missionId)).ifPresent(context -> {
            context.setCurrentState(status == ConvoyVerificationStatus.BLOCKED
                    ? ConvoyMissionState.FAILED
                    : ConvoyMissionState.PRE_INSPECTION);
            missionContextRepository.save(context);
        });
    }

    private ConvoyVerificationStatus statusOf(ConvoyVerificationBlock block) {
        if (!block.isPassed()) {
            return ConvoyVerificationStatus.BLOCKED;
        }
        boolean hasCritical = block.getAlerts().stream().anyMatch(alert -> alert.getSeverity() == ConvoyAlertSeverity.CRITICAL);
        if (hasCritical) {
            return ConvoyVerificationStatus.ESCALATED;
        }
        boolean hasWarning = block.getAlerts().stream().anyMatch(alert -> alert.getSeverity() == ConvoyAlertSeverity.WARNING);
        if (hasWarning) {
            return ConvoyVerificationStatus.PARTIAL;
        }
        return ConvoyVerificationStatus.VERIFIED;
    }

    private ConvoyVerificationStatus worstOf(List<ConvoyVerificationStatus> statuses) {
        if (statuses.stream().anyMatch(status -> status == ConvoyVerificationStatus.BLOCKED)) {
            return ConvoyVerificationStatus.BLOCKED;
        }
        if (statuses.stream().anyMatch(status -> status == ConvoyVerificationStatus.ESCALATED)) {
            return ConvoyVerificationStatus.ESCALATED;
        }
        if (statuses.stream().anyMatch(status -> status == ConvoyVerificationStatus.PARTIAL)) {
            return ConvoyVerificationStatus.PARTIAL;
        }
        return ConvoyVerificationStatus.VERIFIED;
    }

    private Map<String, Object> parseEnrichedData(String enrichedData) {
        if (enrichedData == null || enrichedData.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(enrichedData, new TypeReference<>() {});
        } catch (Exception exception) {
            log.warn("Failed to parse mission enrichedData: {}", exception.getMessage());
            return Map.of();
        }
    }

    private String valueAsString(Map<String, Object> source, String key, String defaultValue) {
        Object value = source.get(key);
        return value == null ? defaultValue : String.valueOf(value);
    }

    private double valueAsDouble(Map<String, Object> source, String key) {
        Object value = source.get(key);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Double.parseDouble(text);
            } catch (NumberFormatException ignored) {
                return 0.0d;
            }
        }
        return 0.0d;
    }

    private boolean valueAsBoolean(Map<String, Object> source, String key) {
        Object value = source.get(key);
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof String text) {
            return Boolean.parseBoolean(text);
        }
        return false;
    }

    private LocalDate valueAsLocalDate(Map<String, Object> source, String key) {
        Object value = source.get(key);
        if (value instanceof String text && !text.isBlank()) {
            return LocalDate.parse(text);
        }
        return null;
    }

    private LocalDateTime valueAsLocalDateTime(Map<String, Object> source, String key, Instant fallback) {
        Object value = source.get(key);
        if (value instanceof String text && !text.isBlank()) {
            return LocalDateTime.parse(text);
        }
        return fallback != null ? LocalDateTime.ofInstant(fallback, ZoneId.systemDefault()) : null;
    }
}
