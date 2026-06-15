package com.goweyy.convoyia.dispatcher.convoy.dispute;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.goweyy.convoyia.common.domain.enums.ConvoyDisputeType;
import com.goweyy.convoyia.common.domain.enums.ConvoyLlmModel;
import com.goweyy.convoyia.common.llm.ConvoyLlmGateway;
import com.goweyy.convoyia.common.llm.ConvoyLlmRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConvoyDisputeRulesEngine {

    private final ConvoyLlmGateway llmGateway;
    private final ObjectMapper objectMapper;

    public ConvoyDisputeResult resolve(ConvoyDisputeRequest request, BigDecimal missionTotalTtc) {
        BigDecimal total = missionTotalTtc == null ? BigDecimal.ZERO : missionTotalTtc;
        return switch (request.getDisputeType()) {
            case LATE_DRIVER -> build(request, "AUTO_RESOLVED", total.multiply(new BigDecimal("0.10")).setScale(2, RoundingMode.HALF_UP), BigDecimal.ZERO, "Driver delay warning stored", false);
            case MINOR_DAMAGE -> build(request, "AUTO_RESOLVED_PENDING_CLIENT", BigDecimal.ZERO, total.multiply(new BigDecimal("0.15")).setScale(2, RoundingMode.HALF_UP), "Conveyor payout partially withheld", false);
            case MAJOR_DAMAGE -> build(request, "ESCALATED", BigDecimal.ZERO, total, "Conveyor payment blocked and Hiscox alert TODO logged", true);
            case EXCESS_MILEAGE -> build(request, "AUTO_RESOLVED", BigDecimal.ZERO, BigDecimal.ZERO, "Supplement calculated with TODO ratePerKm = ZERO", false);
            case CONVEYOR_NO_SHOW -> build(request, "AUTO_RESOLVED", total, BigDecimal.ZERO, "Full refund issued", false);
            case CLIENT_NO_SHOW -> build(request, "AUTO_RESOLVED", BigDecimal.ZERO, BigDecimal.ZERO, "No refund due to client no-show", false);
            case OTHER -> classifyOther(request, total);
        };
    }

    private ConvoyDisputeResult classifyOther(ConvoyDisputeRequest request, BigDecimal total) {
        try {
            String response = llmGateway.invoke(ConvoyLlmRequest.builder()
                    .model(ConvoyLlmModel.LLAMA3_8B)
                    .prompt("""
                            Classify this convoy dispute and respond JSON only.
                            {"type":"LATE_DRIVER|MINOR_DAMAGE|MAJOR_DAMAGE|EXCESS_MILEAGE|CONVEYOR_NO_SHOW|CLIENT_NO_SHOW|OTHER","confidence":0.0,"recommendation":"..."}
                            description=%s evidence=%s
                            """.formatted(request.getDescription(), request.getEvidenceUrls()))
                    .maxTokens(300)
                    .build());
            Map<?, ?> parsed = objectMapper.readValue(response, Map.class);
            double confidence = parsed.get("confidence") instanceof Number number ? number.doubleValue() : 0.0d;
            ConvoyDisputeType type = parsed.get("type") != null
                    ? ConvoyDisputeType.valueOf(String.valueOf(parsed.get("type")).toUpperCase())
                    : ConvoyDisputeType.OTHER;
            if (confidence > 0.80d && type != ConvoyDisputeType.OTHER) {
                ConvoyDisputeRequest classified = new ConvoyDisputeRequest();
                classified.setMissionId(request.getMissionId());
                classified.setDisputeType(type);
                classified.setDescription(request.getDescription());
                classified.setEvidenceUrls(request.getEvidenceUrls());
                classified.setOpenedBy(request.getOpenedBy());
                return resolve(classified, total);
            }
            String recommendation = parsed.get("recommendation") != null
                    ? String.valueOf(parsed.get("recommendation"))
                    : "Manual review required";
            return build(request, "ESCALATED_HUMAN", BigDecimal.ZERO, BigDecimal.ZERO,
                    recommendation, true);
        } catch (Exception exception) {
            log.warn("Failed to classify OTHER dispute: {}", exception.getMessage());
            return build(request, "ESCALATED_HUMAN", BigDecimal.ZERO, BigDecimal.ZERO, "Manual review required", true);
        }
    }

    private ConvoyDisputeResult build(ConvoyDisputeRequest request, String outcome, BigDecimal refundAmount,
                                      BigDecimal penaltyAmount, String resolution, boolean escalated) {
        return ConvoyDisputeResult.builder()
                .disputeId(UUID.randomUUID().toString())
                .missionId(request.getMissionId())
                .type(request.getDisputeType())
                .outcome(outcome)
                .refundAmount(refundAmount)
                .penaltyAmount(penaltyAmount)
                .resolution(resolution)
                .escalated(escalated)
                .build();
    }
}
