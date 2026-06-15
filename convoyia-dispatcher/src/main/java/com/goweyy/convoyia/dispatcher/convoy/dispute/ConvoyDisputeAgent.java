package com.goweyy.convoyia.dispatcher.convoy.dispute;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.goweyy.convoyia.common.domain.enums.ConvoyNotificationTemplate;
import com.goweyy.convoyia.common.repository.ConvoyMissionContextRepository;
import com.goweyy.convoyia.dispatcher.convoy.notifier.ConvoyNotifierAgent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConvoyDisputeAgent {

    private static final Map<String, ConvoyDisputeResult> DISPUTES = new ConcurrentHashMap<>();

    private final ConvoyDisputeRulesEngine rulesEngine;
    private final ConvoyMissionContextRepository missionContextRepository;
    private final ConvoyNotifierAgent notifierAgent;
    private final ObjectMapper objectMapper;

    public ConvoyDisputeResult openDispute(ConvoyDisputeRequest request) {
        BigDecimal totalTtc = missionContextRepository.findByMissionId(UUID.fromString(request.getMissionId()))
                .map(context -> valueAsBigDecimal(parseEnrichedData(context.getEnrichedData()).get("pricingTotalTtc")))
                .orElse(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        ConvoyDisputeResult result = rulesEngine.resolve(request, totalTtc);
        DISPUTES.put(result.getDisputeId(), result);
        notifierAgent.notify(request.getMissionId(), ConvoyNotificationTemplate.DISPUTE_OPENED, Map.of(
                "missionId", request.getMissionId(),
                "resolution", result.getResolution()));
        return result;
    }

    public ConvoyDisputeResult resolveDispute(String disputeId, String resolution, String by) {
        ConvoyDisputeResult existing = DISPUTES.get(disputeId);
        if (existing == null) {
            throw new IllegalArgumentException("Dispute not found: " + disputeId);
        }
        existing.setOutcome("ADMIN_RESOLVED");
        existing.setResolution(resolution + " by " + by);
        existing.setEscalated(false);
        notifierAgent.notify(existing.getMissionId(), ConvoyNotificationTemplate.DISPUTE_RESOLVED, Map.of(
                "missionId", existing.getMissionId(),
                "resolution", existing.getResolution()));
        return existing;
    }

    public ConvoyDisputeResult getStatus(String disputeId) {
        return DISPUTES.get(disputeId);
    }

    private Map<String, Object> parseEnrichedData(String enrichedData) {
        if (enrichedData == null || enrichedData.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(enrichedData, new TypeReference<>() {});
        } catch (Exception exception) {
            log.warn("Failed to parse mission enrichedData for dispute: {}", exception.getMessage());
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
}
