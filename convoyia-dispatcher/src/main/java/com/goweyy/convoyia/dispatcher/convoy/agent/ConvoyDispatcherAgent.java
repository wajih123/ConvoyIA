package com.goweyy.convoyia.dispatcher.convoy.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.goweyy.convoyia.common.config.ConvoyIaProperties;
import com.goweyy.convoyia.common.domain.enums.ConvoyMissionState;
import com.goweyy.convoyia.common.domain.enums.ConvoyUrgency;
import com.goweyy.convoyia.common.domain.enums.ConvoyVehicleSegment;
import com.goweyy.convoyia.common.kafka.ConvoyKafkaEventPublisher;
import com.goweyy.convoyia.common.kafka.ConvoyKafkaTopicsConfig;
import com.goweyy.convoyia.common.kafka.events.ConvoyMissionDispatchedEvent;
import com.goweyy.convoyia.common.llm.ConvoyLlmGateway;
import com.goweyy.convoyia.common.llm.ConvoyLlmRequest;
import com.goweyy.convoyia.common.domain.enums.ConvoyLlmModel;
import com.goweyy.convoyia.common.repository.ConvoyMissionContext;
import com.goweyy.convoyia.common.repository.ConvoyMissionContextRepository;
import com.goweyy.convoyia.dispatcher.convoy.dto.ConvoyDispatchDecision;
import com.goweyy.convoyia.dispatcher.convoy.dto.ConvoyDispatchRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConvoyDispatcherAgent {

    private final ConvoyLlmGateway llmGateway;
    private final ConvoyDispatcherPromptBuilder promptBuilder;
    private final ConvoyMissionContextRepository contextRepository;
    private final ConvoyKafkaEventPublisher kafkaEventPublisher;
    private final ObjectMapper objectMapper;
    private final ConvoyIaProperties convoyIaProperties;

    public ConvoyDispatchDecision dispatch(ConvoyDispatchRequest request) {
        UUID missionId = UUID.randomUUID();
        List<String> agentTrace = new ArrayList<>();
        log.info("ConvoyDispatcherAgent starting missionId={} tenantId={}", missionId, request.getTenantId());

        ConvoyMissionContext ctx = ConvoyMissionContext.builder()
                .missionId(missionId)
                .tenantId(request.getTenantId())
                .currentState(ConvoyMissionState.RECEIVED)
                .urgency(request.getUrgency())
                .originAddress(request.getOriginAddress())
                .destinationAddress(request.getDestinationAddress())
                .clientAboard(request.isClientAboard())
                .agentTrace("[]")
                .enrichedData("{}")
                .build();
        contextRepository.save(ctx);
        agentTrace.add("RECEIVED");

        // QUALIFY
        ctx.setCurrentState(ConvoyMissionState.QUALIFYING);
        String qualPrompt = promptBuilder.buildQualificationPrompt(request);
        String qualResponse = llmGateway.invoke(ConvoyLlmRequest.builder()
                .model(ConvoyLlmModel.PHI3_MINI).prompt(qualPrompt).build());

        ConvoyVehicleSegment segment = ConvoyVehicleSegment.fromValue(request.getVehicleDeclaredValue());
        double confidence = 0.80;
        try {
            @SuppressWarnings("unchecked") Map<String, Object> parsed = (Map<String, Object>) objectMapper.readValue(qualResponse, Map.class);
            String seg = (String) parsed.get("segment");
            confidence = ((Number) parsed.getOrDefault("confidence", 0.80)).doubleValue();
            if (seg != null) segment = ConvoyVehicleSegment.valueOf(seg);
        } catch (Exception e) {
            log.warn("Could not parse qualification response: {}", e.getMessage());
        }

        ctx.setVehicleSegment(segment);
        ctx.setConfidenceScore(confidence);
        agentTrace.add("QUALIFYING: segment=" + segment + " confidence=" + confidence);

        if (confidence < convoyIaProperties.getLlm().getConfidenceThreshold()) {
            ctx.setCurrentState(ConvoyMissionState.ESCALATED_HUMAN);
        } else {
            ctx.setCurrentState(ConvoyMissionState.ROUTING);

            // ROUTE
            String routePrompt = promptBuilder.buildRoutingPrompt(request, segment);
            String routeResponse = llmGateway.invoke(ConvoyLlmRequest.builder()
                    .model(ConvoyLlmModel.MISTRAL_7B).prompt(routePrompt).build());

            int durationMin = 60;
            try {
                @SuppressWarnings("unchecked") Map<String, Object> parsed = (Map<String, Object>) objectMapper.readValue(routeResponse, Map.class);
                durationMin = ((Number) parsed.getOrDefault("estimatedDurationMin", 60)).intValue();
            } catch (Exception e) {
                log.warn("Could not parse routing response: {}", e.getMessage());
            }
            ctx.setEstimatedDurationMin(durationMin);
            ctx.setCurrentState(ConvoyMissionState.PENDING_VERIFICATION);
            agentTrace.add("ROUTING: durationMin=" + durationMin);
        }

        try {
            ctx.setAgentTrace(objectMapper.writeValueAsString(agentTrace));
        } catch (Exception e) {
            ctx.setAgentTrace(agentTrace.toString());
        }
        contextRepository.save(ctx);

        kafkaEventPublisher.publishEvent(
                ConvoyMissionDispatchedEvent.builder()
                        .missionId(missionId.toString())
                        .tenantId(request.getTenantId())
                        .vehicleSegment(segment)
                        .urgency(request.getUrgency() != null ? request.getUrgency() : ConvoyUrgency.STANDARD)
                        .originAddress(request.getOriginAddress())
                        .destinationAddress(request.getDestinationAddress())
                        .occurredAt(Instant.now())
                        .build(),
                ConvoyKafkaTopicsConfig.TOPIC_CONVOY_MISSION_DISPATCHED);

        return ConvoyDispatchDecision.builder()
                .missionId(missionId.toString())
                .tenantId(request.getTenantId())
                .finalState(ctx.getCurrentState())
                .vehicleSegment(segment)
                .confidenceScore(confidence)
                .estimatedDurationMin(ctx.getEstimatedDurationMin())
                .agentTrace(agentTrace)
                .decidedAt(Instant.now())
                .build();
    }
}
