package com.goweyy.convoyia.dispatcher.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.goweyy.convoyia.common.domain.enums.LlmModel;
import com.goweyy.convoyia.common.domain.enums.MissionState;
import com.goweyy.convoyia.common.domain.enums.MissionUrgency;
import com.goweyy.convoyia.common.domain.enums.ResponseFormat;
import com.goweyy.convoyia.common.domain.enums.VehicleSegment;
import com.goweyy.convoyia.common.domain.events.MissionDispatchedEvent;
import com.goweyy.convoyia.common.domain.records.DispatchDecision;
import com.goweyy.convoyia.common.domain.records.LlmRequest;
import com.goweyy.convoyia.common.domain.records.MissionContext;
import com.goweyy.convoyia.common.domain.records.MissionRequest;
import com.goweyy.convoyia.common.kafka.KafkaEventPublisher;
import com.goweyy.convoyia.common.kafka.KafkaTopicsConfig;
import com.goweyy.convoyia.common.llm.LlmGateway;
import com.goweyy.convoyia.dispatcher.repository.MissionContextRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DispatcherAgent {

    public static final double CONFIDENCE_THRESHOLD = 0.72;

    private final LlmGateway llmGateway;
    private final DispatcherPromptBuilder promptBuilder;
    private final MissionContextRepository contextRepository;
    private final KafkaEventPublisher kafkaEventPublisher;
    private final ObjectMapper objectMapper;

    public Mono<DispatchDecision> dispatch(MissionRequest request) {
        String missionId = UUID.randomUUID().toString();
        List<String> agentTrace = new ArrayList<>();

        log.info("Starting dispatch for missionId={} tenantId={}", missionId, request.getTenantId());

        MissionContext initialContext = MissionContext.builder()
                .missionId(missionId)
                .tenantId(request.getTenantId())
                .originalRequest(request)
                .currentState(MissionState.RECEIVED)
                .agentTrace(agentTrace)
                .enrichedData(new java.util.HashMap<>())
                .lastUpdated(Instant.now())
                .build();

        return Mono.just(initialContext)
                .flatMap(ctx -> qualify(ctx, request))
                .flatMap(ctx -> route(ctx))
                .flatMap(ctx -> orchestrate(ctx));
    }

    private Mono<MissionContext> qualify(MissionContext context, MissionRequest request) {
        log.info("Qualifying missionId={} state={}", context.getMissionId(), MissionState.QUALIFYING);
        context.getAgentTrace().add("QUALIFYING: started");
        context.setCurrentState(MissionState.QUALIFYING);
        context.setLastUpdated(Instant.now());

        String prompt = promptBuilder.buildQualificationPrompt(request);
        LlmRequest llmRequest = LlmRequest.builder()
                .model(LlmModel.PHI3_MINI)
                .prompt(prompt)
                .expectedFormat(ResponseFormat.JSON)
                .build();

        return llmGateway.invoke(llmRequest)
                .map(response -> {
                    try {
                        @SuppressWarnings("unchecked") Map<String, Object> parsed = (Map<String, Object>) objectMapper.readValue(response, Map.class);
                        String segmentStr = (String) parsed.get("segment");
                        double confidence = ((Number) parsed.getOrDefault("confidence", 0.0)).doubleValue();
                        String urgencyStr = (String) parsed.get("urgencyConfirmed");

                        context.setConfidenceScore(confidence);

                        if (confidence < CONFIDENCE_THRESHOLD) {
                            log.info("Confidence {} below threshold for missionId={}, escalating to human",
                                    confidence, context.getMissionId());
                            context.setCurrentState(MissionState.ESCALATED_HUMAN);
                            context.getAgentTrace().add("QUALIFYING: escalated to human (confidence=" + confidence + ")");
                        } else {
                            try {
                                context.setVehicleSegment(VehicleSegment.valueOf(segmentStr));
                            } catch (IllegalArgumentException e) {
                                context.setVehicleSegment(VehicleSegment.STANDARD);
                            }
                            context.setCurrentState(MissionState.ROUTING);
                            context.getAgentTrace().add("QUALIFYING: completed segment=" + context.getVehicleSegment() + " confidence=" + confidence);
                        }
                    } catch (Exception e) {
                        log.warn("Failed to parse qualification response for missionId={}: {}", context.getMissionId(), e.getMessage());
                        context.setCurrentState(MissionState.ESCALATED_HUMAN);
                        context.getAgentTrace().add("QUALIFYING: failed to parse LLM response - " + e.getMessage());
                    }
                    context.setLastUpdated(Instant.now());
                    return context;
                });
    }

    private Mono<MissionContext> route(MissionContext context) {
        if (context.getCurrentState() == MissionState.ESCALATED_HUMAN) {
            log.info("Skipping routing for missionId={} - escalated to human", context.getMissionId());
            return Mono.just(context);
        }

        log.info("Routing missionId={} state={}", context.getMissionId(), MissionState.ROUTING);
        context.getAgentTrace().add("ROUTING: started");
        context.setLastUpdated(Instant.now());

        String prompt = promptBuilder.buildRoutingPrompt(context);
        LlmRequest llmRequest = LlmRequest.builder()
                .model(LlmModel.MISTRAL_7B)
                .prompt(prompt)
                .expectedFormat(ResponseFormat.JSON)
                .build();

        return llmGateway.invoke(llmRequest)
                .map(response -> {
                    try {
                        @SuppressWarnings("unchecked") Map<String, Object> parsed = (Map<String, Object>) objectMapper.readValue(response, Map.class);
                        int duration = ((Number) parsed.getOrDefault("estimatedDurationMin", 60)).intValue();
                        String notes = (String) parsed.getOrDefault("notes", "");
                        context.getEnrichedData().put("estimatedDurationMin", duration);
                        context.getEnrichedData().put("routingNotes", notes);
                        context.getEnrichedData().put("returnMode", parsed.get("returnMode"));
                        context.setCurrentState(MissionState.PENDING_VERIFICATION);
                        context.getAgentTrace().add("ROUTING: completed duration=" + duration + "min");
                    } catch (Exception e) {
                        log.warn("Failed to parse routing response for missionId={}: {}", context.getMissionId(), e.getMessage());
                        context.getEnrichedData().put("estimatedDurationMin", 60);
                        context.setCurrentState(MissionState.PENDING_VERIFICATION);
                        context.getAgentTrace().add("ROUTING: completed with defaults (parse error)");
                    }
                    context.setLastUpdated(Instant.now());
                    return context;
                });
    }

    private Mono<DispatchDecision> orchestrate(MissionContext context) {
        log.info("Orchestrating missionId={} finalState={}", context.getMissionId(), context.getCurrentState());
        context.getAgentTrace().add("ORCHESTRATING: saving and publishing");

        return contextRepository.saveContext(context)
                .then(kafkaEventPublisher.publishEvent(
                        MissionDispatchedEvent.builder()
                                .missionId(context.getMissionId())
                                .tenantId(context.getTenantId())
                                .vehicleSegment(context.getVehicleSegment())
                                .urgency(context.getOriginalRequest() != null
                                        ? context.getOriginalRequest().getUrgency()
                                        : MissionUrgency.STANDARD)
                                .occurredAt(Instant.now())
                                .build(),
                        KafkaTopicsConfig.TOPIC_MISSION_DISPATCH_COMPLETED
                ))
                .thenReturn(DispatchDecision.builder()
                        .missionId(context.getMissionId())
                        .tenantId(context.getTenantId())
                        .finalState(context.getCurrentState())
                        .estimatedDurationMin(context.getEnrichedData() != null
                                ? (int) context.getEnrichedData().getOrDefault("estimatedDurationMin", 60)
                                : 60)
                        .routingNotes(context.getEnrichedData() != null
                                ? (String) context.getEnrichedData().getOrDefault("routingNotes", "")
                                : "")
                        .agentTrace(new ArrayList<>(context.getAgentTrace()))
                        .decidedAt(Instant.now())
                        .build());
    }
}
