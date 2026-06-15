package com.goweyy.convoyia.dispatcher.convoy.scheduler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.goweyy.convoyia.biller.convoy.agent.ConvoyStripeConnectService;
import com.goweyy.convoyia.common.domain.enums.ConvoyMarket;
import com.goweyy.convoyia.common.domain.enums.ConvoyMissionState;
import com.goweyy.convoyia.common.domain.enums.ConvoyNotificationTemplate;
import com.goweyy.convoyia.common.domain.tenant.ConvoyTenantConfig;
import com.goweyy.convoyia.common.domain.tenant.ConvoyTenantConfigRepository;
import com.goweyy.convoyia.common.kafka.ConvoyKafkaEventPublisher;
import com.goweyy.convoyia.common.kafka.ConvoyKafkaTopicsConfig;
import com.goweyy.convoyia.common.kafka.events.ConvoyMissionCancelledEvent;
import com.goweyy.convoyia.common.kafka.events.ConvoyMissionRescheduledEvent;
import com.goweyy.convoyia.common.repository.ConvoyMissionContext;
import com.goweyy.convoyia.common.repository.ConvoyMissionContextRepository;
import com.goweyy.convoyia.dispatcher.convoy.notifier.ConvoyNotifierAgent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConvoySchedulerAgent {

    private static final Map<String, AtomicInteger> RESCHEDULE_COUNTS = new ConcurrentHashMap<>();

    private final ConvoyMissionContextRepository missionContextRepository;
    private final ConvoyKafkaEventPublisher kafkaEventPublisher;
    private final ConvoyNotifierAgent notifierAgent;
    private final ConvoyStripeConnectService stripeConnectService;
    private final ConvoyTenantConfigRepository tenantConfigRepository;
    private final ObjectMapper objectMapper;

    public ConvoyScheduleResult cancel(String missionId, String requestedBy, String reason) {
        ConvoyMissionContext context = missionContextRepository.findByMissionId(UUID.fromString(missionId))
                .orElseThrow(() -> new IllegalArgumentException("Mission context not found for missionId=" + missionId));
        Map<String, Object> enriched = parseEnrichedData(context.getEnrichedData());
        BigDecimal totalTtc = valueAsBigDecimal(enriched.get("pricingTotalTtc"));
        long hoursUntil = Duration.between(Instant.now(), context.getLastUpdated() != null ? context.getLastUpdated().plus(Duration.ofHours(30)) : Instant.now().plus(Duration.ofHours(30))).toHours();
        BigDecimal refundRatio = hoursUntil > 48 ? BigDecimal.ONE : hoursUntil > 24 ? new BigDecimal("0.50") : BigDecimal.ZERO;
        BigDecimal refundAmount = totalTtc.multiply(refundRatio).setScale(2, RoundingMode.HALF_UP);
        String currencyCode = resolveCurrency(context.getTenantId());

        if ("CONVEYOR".equalsIgnoreCase(requestedBy) && hoursUntil < 24) {
            log.warn("TODO: store conveyor penalty missionId={} requestedBy={} reason={}", missionId, requestedBy, reason);
        }
        String paymentIntentId = valueAsString(enriched.get("paymentIntentId"));
        if (refundRatio.compareTo(BigDecimal.ZERO) > 0 && paymentIntentId != null && !paymentIntentId.isBlank()) {
            stripeConnectService.createRefund(paymentIntentId, refundAmount, currencyCode.toLowerCase(), reason);
        }

        context.setCurrentState(ConvoyMissionState.CANCELLED);
        missionContextRepository.save(context);
        kafkaEventPublisher.publishEvent(ConvoyMissionCancelledEvent.builder()
                        .missionId(missionId)
                        .tenantId(context.getTenantId())
                        .requestedBy(requestedBy)
                        .refundAmount(refundAmount)
                        .currencyCode(currencyCode)
                        .occurredAt(Instant.now())
                        .build(),
                ConvoyKafkaTopicsConfig.TOPIC_CONVOY_MISSION_CANCELLED);
        notifierAgent.notify(missionId, ConvoyNotificationTemplate.MISSION_CANCELLED, Map.of(
                "refundAmount", refundAmount,
                "currency", currencyCode,
                "missionId", missionId));
        return ConvoyScheduleResult.builder()
                .missionId(missionId)
                .outcome("CANCELLED")
                .refundAmount(refundAmount)
                .currency(currencyCode)
                .message("Mission cancelled")
                .build();
    }

    public ConvoyScheduleResult reschedule(String missionId, LocalDateTime newDateTime, String requestedBy) {
        ConvoyMissionContext context = missionContextRepository.findByMissionId(UUID.fromString(missionId))
                .orElseThrow(() -> new IllegalArgumentException("Mission context not found for missionId=" + missionId));
        Map<String, Object> enriched = parseEnrichedData(context.getEnrichedData());
        BigDecimal totalTtc = valueAsBigDecimal(enriched.get("pricingTotalTtc"));
        long hoursUntil = Duration.between(Instant.now(), context.getLastUpdated() != null ? context.getLastUpdated().plus(Duration.ofHours(30)) : Instant.now().plus(Duration.ofHours(30))).toHours();
        int count = RESCHEDULE_COUNTS.computeIfAbsent(missionId, ignored -> new AtomicInteger()).getAndIncrement();
        BigDecimal fee = (count == 0 && hoursUntil >= 24)
                ? BigDecimal.ZERO
                : totalTtc.multiply(new BigDecimal("0.10")).setScale(2, RoundingMode.HALF_UP);
        context.setCurrentState(ConvoyMissionState.RESCHEDULED);
        missionContextRepository.save(context);
        kafkaEventPublisher.publishEvent(ConvoyMissionRescheduledEvent.builder()
                        .missionId(missionId)
                        .tenantId(context.getTenantId())
                        .requestedBy(requestedBy)
                        .newDateTime(newDateTime)
                        .rescheduleFee(fee)
                        .occurredAt(Instant.now())
                        .build(),
                ConvoyKafkaTopicsConfig.TOPIC_CONVOY_MISSION_RESCHEDULED);
        notifierAgent.notify(missionId, ConvoyNotificationTemplate.MISSION_RESCHEDULED, Map.of(
                "missionId", missionId,
                "newDateTime", newDateTime,
                "fee", fee));
        return ConvoyScheduleResult.builder()
                .missionId(missionId)
                .outcome("RESCHEDULED")
                .refundAmount(BigDecimal.ZERO)
                .currency(resolveCurrency(context.getTenantId()))
                .message("Mission rescheduled with fee=" + fee)
                .build();
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

    private BigDecimal valueAsBigDecimal(Object value) {
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue()).setScale(2, RoundingMode.HALF_UP);
        }
        if (value instanceof String text && !text.isBlank()) {
            return new BigDecimal(text).setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    private String valueAsString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String resolveCurrency(String tenantId) {
        ConvoyTenantConfig tenantConfig = tenantConfigRepository.findActiveByTenantId(tenantId).block();
        return tenantConfig != null && tenantConfig.getCurrencyCode() != null
                ? tenantConfig.getCurrencyCode()
                : ConvoyMarket.FRANCE.getCurrencyCode();
    }
}
