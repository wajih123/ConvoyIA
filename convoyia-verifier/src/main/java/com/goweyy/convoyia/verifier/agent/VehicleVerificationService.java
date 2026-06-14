package com.goweyy.convoyia.verifier.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.goweyy.convoyia.common.domain.enums.AlertSeverity;
import com.goweyy.convoyia.common.domain.enums.LlmModel;
import com.goweyy.convoyia.common.domain.enums.ResponseFormat;
import com.goweyy.convoyia.common.domain.enums.VerificationStatus;
import com.goweyy.convoyia.common.domain.enums.VehicleSegment;
import com.goweyy.convoyia.common.domain.records.LlmRequest;
import com.goweyy.convoyia.common.domain.records.VerificationAlert;
import com.goweyy.convoyia.common.domain.records.VerificationBlock;
import com.goweyy.convoyia.common.domain.records.VerificationRequest;
import com.goweyy.convoyia.common.llm.LlmGateway;
import com.goweyy.convoyia.verifier.llm.VerifierPromptBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleVerificationService {

    private final LlmGateway llmGateway;
    private final VerifierPromptBuilder promptBuilder;
    private final ObjectMapper objectMapper;

    public Mono<VerificationBlock> verify(VerificationRequest request) {
        List<String> passed = new ArrayList<>();
        List<String> failed = new ArrayList<>();
        List<VerificationAlert> alerts = new ArrayList<>();

        var vehicle = request.getVehicleData();
        if (vehicle == null) {
            failed.add("VEHICLE_DATA_MISSING");
            return Mono.just(buildBlock(passed, failed, alerts, null, "Données véhicule manquantes"));
        }

        // CHECK 1: Carte grise
        if (vehicle.getCarteGriseUrl() != null && !vehicle.getCarteGriseUrl().isBlank()) {
            passed.add("CARTE_GRISE_PRESENT");
        } else {
            failed.add("CARTE_GRISE_MISSING");
            alerts.add(VerificationAlert.builder()
                    .code("CARTE_GRISE_MISSING")
                    .message("Carte grise absente")
                    .severity(AlertSeverity.CRITICAL)
                    .blocking(true)
                    .build());
        }

        // CHECK 2: Assurance expiry
        if (vehicle.getAssuranceExpiry() != null && vehicle.getAssuranceExpiry().isAfter(LocalDate.now())) {
            passed.add("ASSURANCE_VALIDE");
        } else {
            failed.add("ASSURANCE_EXPIREE");
            alerts.add(VerificationAlert.builder()
                    .code("ASSURANCE_EXPIREE")
                    .message("Assurance expirée")
                    .severity(AlertSeverity.CRITICAL)
                    .blocking(true)
                    .build());
        }

        // CHECK 3: Controle technique
        if (vehicle.getControleTechniqueDate() != null) {
            long monthsOld = ChronoUnit.MONTHS.between(vehicle.getControleTechniqueDate(), LocalDate.now());
            if (monthsOld <= 24) {
                passed.add("CT_VALIDE");
                if (monthsOld >= 21) {
                    alerts.add(VerificationAlert.builder()
                            .code("CT_EXPIRING_SOON")
                            .message("Contrôle technique expire bientôt (" + monthsOld + " mois)")
                            .severity(AlertSeverity.WARNING)
                            .blocking(false)
                            .build());
                }
            } else {
                failed.add("CT_EXPIRE");
                alerts.add(VerificationAlert.builder()
                        .code("CT_EXPIRE")
                        .message("Contrôle technique expiré (" + monthsOld + " mois)")
                        .severity(AlertSeverity.CRITICAL)
                        .blocking(true)
                        .build());
            }
        } else {
            failed.add("CT_ABSENT");
            alerts.add(VerificationAlert.builder()
                    .code("CT_ABSENT")
                    .message("Date de contrôle technique absente")
                    .severity(AlertSeverity.CRITICAL)
                    .blocking(true)
                    .build());
        }

        // CHECK 4: LLM coherence check
        String llmPrompt = promptBuilder.buildVehicleCoherencePrompt(vehicle);
        LlmRequest llmRequest = LlmRequest.builder()
                .model(LlmModel.PHI3_MINI)
                .prompt(llmPrompt)
                .expectedFormat(ResponseFormat.JSON)
                .build();

        return llmGateway.invoke(llmRequest)
                .map(response -> {
                    try {
                        Map<?, ?> parsed = objectMapper.readValue(response, Map.class);
                        boolean coherent = Boolean.TRUE.equals(parsed.get("coherent"));
                        if (!coherent) {
                            alerts.add(VerificationAlert.builder()
                                    .code("SEGMENT_MISMATCH")
                                    .message("Incohérence entre le véhicule et le segment déclaré: " + parsed.get("reason"))
                                    .severity(AlertSeverity.WARNING)
                                    .blocking(false)
                                    .build());
                        }
                    } catch (Exception e) {
                        log.warn("Failed to parse vehicle coherence LLM response: {}", e.getMessage());
                    }
                    return buildBlock(passed, failed, alerts, null, null);
                })
                .onErrorResume(e -> {
                    log.warn("Vehicle coherence LLM check failed: {}", e.getMessage());
                    return Mono.just(buildBlock(passed, failed, alerts, null, null));
                });
    }

    private VerificationBlock buildBlock(List<String> passed, List<String> failed,
                                          List<VerificationAlert> alerts, Double confidence, String llmReasoning) {
        VerificationStatus status = deriveStatus(failed, alerts);
        return VerificationBlock.builder()
                .blockId(UUID.randomUUID().toString())
                .status(status)
                .passed(passed)
                .failed(failed)
                .alerts(alerts)
                .confidenceScore(confidence)
                .llmReasoning(llmReasoning)
                .build();
    }

    private VerificationStatus deriveStatus(List<String> failed, List<VerificationAlert> alerts) {
        if (!failed.isEmpty()) return VerificationStatus.BLOCKED;
        boolean hasCritical = alerts.stream().anyMatch(a -> a.getSeverity() == AlertSeverity.CRITICAL);
        if (hasCritical) return VerificationStatus.ESCALATED;
        boolean hasWarning = alerts.stream().anyMatch(a -> a.getSeverity() == AlertSeverity.WARNING);
        if (hasWarning) return VerificationStatus.PARTIAL;
        return VerificationStatus.VERIFIED;
    }
}
