package com.goweyy.convoyia.inspector.convoy.agent;

import com.goweyy.convoyia.common.domain.enums.ConvoyAlertSeverity;
import com.goweyy.convoyia.common.domain.enums.ConvoyInspectionPhase;
import com.goweyy.convoyia.common.domain.enums.ConvoyMissionState;
import com.goweyy.convoyia.common.domain.enums.ConvoyVerificationStatus;
import com.goweyy.convoyia.common.kafka.ConvoyKafkaEventPublisher;
import com.goweyy.convoyia.common.kafka.ConvoyKafkaTopicsConfig;
import com.goweyy.convoyia.common.kafka.events.ConvoyInspectionCompletedEvent;
import com.goweyy.convoyia.common.kafka.events.ConvoyVerificationCompletedEvent;
import com.goweyy.convoyia.common.repository.ConvoyMissionContext;
import com.goweyy.convoyia.common.repository.ConvoyMissionContextRepository;
import com.goweyy.convoyia.inspector.convoy.dto.ConvoyDamageReport;
import com.goweyy.convoyia.inspector.convoy.dto.ConvoyInspectionRequest;
import com.goweyy.convoyia.inspector.convoy.dto.ConvoyInspectionResult;
import com.goweyy.convoyia.inspector.convoy.vision.ConvoyQwenVlAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConvoyInspectorAgent {

    private final ConvoyInspectorPromptBuilder promptBuilder;
    private final ConvoyQwenVlAdapter qwenVlAdapter;
    private final ConvoySignatureService signatureService;
    private final ConvoyMissionContextRepository missionContextRepository;
    private final ConvoyKafkaEventPublisher kafkaEventPublisher;

    private final Map<String, ConvoyInspectionResult> preMissionResults = new ConcurrentHashMap<>();

    public ConvoyInspectionResult inspect(ConvoyInspectionRequest request) {
        log.info("ConvoyInspectorAgent inspecting missionId={} phase={}", request.getMissionId(), request.getPhase());

        String analysisResult = "";
        if (request.getPhotoUrls() != null && !request.getPhotoUrls().isEmpty()) {
            String prompt = promptBuilder.buildInspectionPrompt(request);
            analysisResult = qwenVlAdapter.analyze(prompt, request.getPhotoUrls());
        }

        boolean damageDetected = containsDamage(analysisResult);
        ConvoyAlertSeverity severity = damageDetected ? ConvoyAlertSeverity.WARNING : ConvoyAlertSeverity.INFO;
        if (analysisResult.toLowerCase().contains("critical")) {
            severity = ConvoyAlertSeverity.CRITICAL;
        }

        ConvoyDamageReport damageReport = ConvoyDamageReport.builder()
                .damageDetected(damageDetected)
                .damagedZones(Collections.emptyList())
                .severity(severity)
                .description(analysisResult.isEmpty() ? "No photos provided" : analysisResult)
                .build();

        ConvoyInspectionResult result = ConvoyInspectionResult.builder()
                .missionId(request.getMissionId())
                .tenantId(request.getTenantId())
                .phase(request.getPhase())
                .passed(!damageDetected)
                .damageReport(damageReport)
                .notes(analysisResult)
                .inspectedAt(Instant.now())
                .build();

        if (request.getPhase() == ConvoyInspectionPhase.PRE_MISSION) {
            preMissionResults.put(request.getMissionId(), result);
            if (request.getPhotoUrls() != null && !request.getPhotoUrls().isEmpty()) {
                signatureService.requestSignature("inspection/" + request.getMissionId() + "/pre-mission.pdf", "");
            }
            updateState(request.getMissionId(), ConvoyMissionState.IN_PROGRESS);
        } else {
            ConvoyInspectionResult preResult = preMissionResults.get(request.getMissionId());
            if (isMajorOrCritical(result, preResult)) {
                updateState(request.getMissionId(), ConvoyMissionState.ESCALATED_HUMAN);
            } else {
                updateState(request.getMissionId(), ConvoyMissionState.PENDING_BILLING);
            }
        }

        kafkaEventPublisher.publishEvent(ConvoyInspectionCompletedEvent.builder()
                        .missionId(result.getMissionId())
                        .tenantId(result.getTenantId())
                        .phase(result.getPhase())
                        .damageDetected(result.getDamageReport() != null && result.getDamageReport().isDamageDetected())
                        .occurredAt(Instant.now())
                        .build(),
                ConvoyKafkaTopicsConfig.TOPIC_CONVOY_INSPECTION_COMPLETED);
        return result;
    }

    @KafkaListener(topics = ConvoyKafkaTopicsConfig.TOPIC_CONVOY_VERIFICATION_COMPLETED,
            groupId = "${spring.kafka.consumer.group-id:convoy-inspector}")
    public void onVerificationCompleted(ConvoyVerificationCompletedEvent event) {
        if (event.getStatus() == ConvoyVerificationStatus.BLOCKED) {
            log.info("Skipping inspection for blocked missionId={}", event.getMissionId());
            return;
        }
        inspect(ConvoyInspectionRequest.builder()
                .missionId(event.getMissionId())
                .tenantId(event.getTenantId())
                .phase(ConvoyInspectionPhase.PRE_MISSION)
                .photoUrls(List.of())
                .build());
    }

    private boolean containsDamage(String analysisResult) {
        String normalized = analysisResult == null ? "" : analysisResult.toLowerCase();
        return normalized.contains("damage") || normalized.contains("scratch") || normalized.contains("dent");
    }

    private boolean isMajorOrCritical(ConvoyInspectionResult postResult, ConvoyInspectionResult preResult) {
        if (postResult.getDamageReport() == null) {
            return false;
        }
        boolean preDetected = preResult != null && preResult.getDamageReport() != null && preResult.getDamageReport().isDamageDetected();
        String description = postResult.getDamageReport().getDescription() == null ? "" : postResult.getDamageReport().getDescription().toLowerCase();
        return (!preDetected && postResult.getDamageReport().getSeverity() == ConvoyAlertSeverity.CRITICAL)
                || description.contains("major");
    }

    private void updateState(String missionId, ConvoyMissionState state) {
        missionContextRepository.findByMissionId(UUID.fromString(missionId)).ifPresent(context -> {
            context.setCurrentState(state);
            missionContextRepository.save(context);
        });
    }
}
