package com.goweyy.convoyia.inspector.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.goweyy.convoyia.common.domain.enums.LlmModel;
import com.goweyy.convoyia.common.domain.enums.ResponseFormat;
import com.goweyy.convoyia.common.domain.events.InspectionCompletedEvent;
import com.goweyy.convoyia.common.domain.records.LlmRequest;
import com.goweyy.convoyia.common.kafka.KafkaEventPublisher;
import com.goweyy.convoyia.common.kafka.KafkaTopicsConfig;
import com.goweyy.convoyia.common.llm.LlmGateway;
import com.goweyy.convoyia.inspector.domain.DamageReport;
import com.goweyy.convoyia.inspector.domain.InspectionPhase;
import com.goweyy.convoyia.inspector.domain.InspectionRequest;
import com.goweyy.convoyia.inspector.domain.InspectionResult;
import com.goweyy.convoyia.inspector.vision.QwenVlAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class InspectorAgent {

    private final QwenVlAdapter qwenVlAdapter;
    private final LlmGateway llmGateway;
    private final InspectorPromptBuilder promptBuilder;
    private final KafkaEventPublisher kafkaEventPublisher;
    private final ObjectMapper objectMapper;

    // Simple in-memory store for pre-mission results (use DB in production)
    private final java.util.concurrent.ConcurrentHashMap<String, InspectionResult> preMissionResults
            = new java.util.concurrent.ConcurrentHashMap<>();

    public Mono<InspectionResult> inspect(InspectionRequest request) {
        log.info("Starting inspection missionId={} phase={}", request.getMissionId(), request.getPhase());

        List<Mono<DamageReport>> photoAnalyses = new ArrayList<>();
        if (request.getPhotoUrls() != null) {
            for (String photoUrl : request.getPhotoUrls()) {
                photoAnalyses.add(qwenVlAdapter.analyzeImage(photoUrl, request.getPhase(), request.getMissionId()));
            }
        }

        Mono<List<DamageReport>> allAnalyses = photoAnalyses.isEmpty()
                ? Mono.just(List.of())
                : Flux.fromIterable(photoAnalyses).flatMap(m -> m).collectList();

        return allAnalyses.flatMap(reports -> {
            DamageReport aggregated = aggregateReports(reports, request.getPhotoUrls());

            if (request.getPhase() == InspectionPhase.POST_MISSION) {
                InspectionResult preResult = preMissionResults.get(request.getMissionId());
                if (preResult != null) {
                    return compareMissions(preResult, aggregated, request);
                }
            }

            boolean damageDetected = !"NONE".equals(aggregated.getSeverity());
            InspectionResult result = InspectionResult.builder()
                    .missionId(request.getMissionId())
                    .tenantId(request.getTenantId())
                    .phase(request.getPhase())
                    .damageReport(aggregated)
                    .damageDetected(damageDetected)
                    .odometerAtInspection(request.getOdometerReading())
                    .fuelLevelAtInspection(request.getFuelLevelPercent())
                    .inspectedAt(Instant.now())
                    .inspectedBy(request.getConveyorId())
                    .build();

            if (request.getPhase() == InspectionPhase.PRE_MISSION) {
                preMissionResults.put(request.getMissionId(), result);
            }

            return publishAndReturn(result);
        });
    }

    private Mono<InspectionResult> compareMissions(InspectionResult pre, DamageReport postReport, InspectionRequest request) {
        InspectionResult postResult = InspectionResult.builder()
                .missionId(request.getMissionId())
                .tenantId(request.getTenantId())
                .phase(request.getPhase())
                .damageReport(postReport)
                .damageDetected(!"NONE".equals(postReport.getSeverity()))
                .odometerAtInspection(request.getOdometerReading())
                .fuelLevelAtInspection(request.getFuelLevelPercent())
                .inspectedAt(Instant.now())
                .inspectedBy(request.getConveyorId())
                .build();

        String comparisonPrompt = promptBuilder.buildComparisonPrompt(pre, postResult);
        LlmRequest llmRequest = LlmRequest.builder()
                .model(LlmModel.MISTRAL_7B)
                .prompt(comparisonPrompt)
                .expectedFormat(ResponseFormat.JSON)
                .build();

        return llmGateway.invoke(llmRequest)
                .map(response -> {
                    try {
                        Map<?, ?> parsed = objectMapper.readValue(response, Map.class);
                        @SuppressWarnings("unchecked")
                        List<String> newDamages = (List<String>) parsed.getOrDefault("newDamages", List.of());
                        String verdict = (String) parsed.getOrDefault("verdict", "CLEAN");
                        boolean hasNewDamage = !newDamages.isEmpty() || !"CLEAN".equals(verdict);

                        DamageReport updatedReport = DamageReport.builder()
                                .areas(postReport.getAreas())
                                .severity(postReport.getSeverity())
                                .newDamagesVsDeparture(newDamages)
                                .requiresHiscoxAlert(hasNewDamage)
                                .photoUrls(postReport.getPhotoUrls())
                                .build();

                        return InspectionResult.builder()
                                .missionId(request.getMissionId())
                                .tenantId(request.getTenantId())
                                .phase(request.getPhase())
                                .damageReport(updatedReport)
                                .damageDetected(hasNewDamage)
                                .odometerAtInspection(request.getOdometerReading())
                                .fuelLevelAtInspection(request.getFuelLevelPercent())
                                .inspectedAt(Instant.now())
                                .inspectedBy(request.getConveyorId())
                                .build();
                    } catch (Exception e) {
                        log.warn("Failed to parse comparison response: {}", e.getMessage());
                        return postResult;
                    }
                })
                .flatMap(this::publishAndReturn);
    }

    private Mono<InspectionResult> publishAndReturn(InspectionResult result) {
        return kafkaEventPublisher.publishEvent(
                InspectionCompletedEvent.builder()
                        .missionId(result.getMissionId())
                        .tenantId(result.getTenantId())
                        .damageDetected(result.isDamageDetected())
                        .occurredAt(Instant.now())
                        .build(),
                KafkaTopicsConfig.TOPIC_MISSION_INSPECTION_COMPLETED
        ).thenReturn(result);
    }

    private DamageReport aggregateReports(List<DamageReport> reports, List<String> photoUrls) {
        if (reports.isEmpty()) {
            return DamageReport.builder()
                    .areas(List.of())
                    .severity("NONE")
                    .newDamagesVsDeparture(List.of())
                    .requiresHiscoxAlert(false)
                    .photoUrls(photoUrls != null ? photoUrls : List.of())
                    .build();
        }

        List<String> allAreas = new ArrayList<>();
        String worstSeverity = "NONE";
        boolean requiresAlert = false;

        String[] severityOrder = {"NONE", "MINOR", "MAJOR", "CRITICAL"};
        int worstIdx = 0;

        for (DamageReport report : reports) {
            allAreas.addAll(report.getAreas());
            requiresAlert = requiresAlert || report.isRequiresHiscoxAlert();
            for (int i = 0; i < severityOrder.length; i++) {
                if (severityOrder[i].equals(report.getSeverity()) && i > worstIdx) {
                    worstIdx = i;
                    worstSeverity = severityOrder[i];
                }
            }
        }

        return DamageReport.builder()
                .areas(allAreas)
                .severity(worstSeverity)
                .newDamagesVsDeparture(List.of())
                .requiresHiscoxAlert(requiresAlert)
                .photoUrls(photoUrls != null ? photoUrls : List.of())
                .build();
    }
}
