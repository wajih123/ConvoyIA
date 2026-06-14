package com.goweyy.convoyia.verifier.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.goweyy.convoyia.common.domain.enums.AlertSeverity;
import com.goweyy.convoyia.common.domain.enums.LlmModel;
import com.goweyy.convoyia.common.domain.enums.ResponseFormat;
import com.goweyy.convoyia.common.domain.enums.VerificationStatus;
import com.goweyy.convoyia.common.domain.records.HiscoxCoverageResult;
import com.goweyy.convoyia.common.domain.records.LlmRequest;
import com.goweyy.convoyia.common.domain.records.VerificationAlert;
import com.goweyy.convoyia.common.domain.records.VerificationBlock;
import com.goweyy.convoyia.common.domain.records.VerificationRequest;
import com.goweyy.convoyia.common.llm.LlmGateway;
import com.goweyy.convoyia.verifier.integration.HiscoxAdapter;
import com.goweyy.convoyia.verifier.llm.VerifierPromptBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MissionVerificationService {

    private final LlmGateway llmGateway;
    private final VerifierPromptBuilder promptBuilder;
    private final HiscoxAdapter hiscoxAdapter;
    private final ObjectMapper objectMapper;

    public Mono<VerificationBlock> verify(VerificationRequest request) {
        return Mono.zip(
                checkAddresses(request),
                checkTimeSlot(request),
                checkHiscoxCoverage(request)
        ).map(tuple -> {
            List<String> passed = new ArrayList<>();
            List<String> failed = new ArrayList<>();
            List<VerificationAlert> alerts = new ArrayList<>();

            mergeResult(tuple.getT1(), passed, failed, alerts);
            mergeResult(tuple.getT2(), passed, failed, alerts);
            mergeResult(tuple.getT3(), passed, failed, alerts);

            VerificationStatus status = deriveStatus(failed, alerts);
            return VerificationBlock.builder()
                    .blockId(UUID.randomUUID().toString())
                    .status(status)
                    .passed(passed)
                    .failed(failed)
                    .alerts(alerts)
                    .build();
        });
    }

    private Mono<CheckResult> checkAddresses(VerificationRequest request) {
        String prompt = promptBuilder.buildAddressValidationPrompt(
                request.getOriginAddress(), request.getDestinationAddress());
        LlmRequest llmRequest = LlmRequest.builder()
                .model(LlmModel.PHI3_MINI)
                .prompt(prompt)
                .expectedFormat(ResponseFormat.JSON)
                .build();

        return llmGateway.invoke(llmRequest)
                .map(response -> {
                    try {
                        Map<?, ?> parsed = objectMapper.readValue(response, Map.class);
                        boolean valid = Boolean.TRUE.equals(parsed.get("valid"));
                        if (valid) {
                            return new CheckResult(List.of("ADDRESSES_VALID"), List.of(), List.of());
                        } else {
                            return new CheckResult(List.of(), List.of("ADDRESSES_INVALID"),
                                    List.of(VerificationAlert.builder()
                                            .code("ADDRESSES_INVALID")
                                            .message("Adresses invalides: " + parsed.get("reason"))
                                            .severity(AlertSeverity.CRITICAL)
                                            .blocking(true)
                                            .build()));
                        }
                    } catch (Exception e) {
                        return new CheckResult(List.of("ADDRESSES_VALID"), List.of(), List.of());
                    }
                })
                .onErrorResume(e -> Mono.just(new CheckResult(List.of("ADDRESSES_VALID"), List.of(), List.of())));
    }

    private Mono<CheckResult> checkTimeSlot(VerificationRequest request) {
        List<String> passed = new ArrayList<>();
        List<String> failed = new ArrayList<>();
        List<VerificationAlert> alerts = new ArrayList<>();

        LocalDateTime requestedAt = request.getRequestedAt();
        if (requestedAt == null || requestedAt.isBefore(LocalDateTime.now())) {
            failed.add("TIMESLOT_IN_PAST");
            alerts.add(VerificationAlert.builder()
                    .code("TIMESLOT_IN_PAST")
                    .message("Le créneau horaire est dans le passé")
                    .severity(AlertSeverity.CRITICAL)
                    .blocking(true)
                    .build());
        } else {
            passed.add("TIMESLOT_VALID");
            int hour = requestedAt.getHour();
            if (hour >= 22 || hour < 6) {
                alerts.add(VerificationAlert.builder()
                        .code("NIGHT_BONUS_APPLICABLE")
                        .message("Convoyage de nuit - bonus nuit applicable")
                        .severity(AlertSeverity.INFO)
                        .blocking(false)
                        .build());
            }
        }
        return Mono.just(new CheckResult(passed, failed, alerts));
    }

    private Mono<CheckResult> checkHiscoxCoverage(VerificationRequest request) {
        if (request.getVehicleData() == null) {
            return Mono.just(new CheckResult(List.of(), List.of("HISCOX_NO_VEHICLE_DATA"), List.of()));
        }
        return hiscoxAdapter.checkCoverage(request.getVehicleData(), request.getMissionId())
                .map(result -> {
                    if (request.getVehicleData().getDeclaredValue() > 120_000) {
                        return new CheckResult(List.of(), List.of("HISCOX_COVERAGE_EXCEEDED"),
                                List.of(VerificationAlert.builder()
                                        .code("HISCOX_COVERAGE_EXCEEDED")
                                        .message("Valeur véhicule dépasse le plafond Hiscox (120 000 EUR)")
                                        .severity(AlertSeverity.CRITICAL)
                                        .blocking(true)
                                        .build()));
                    } else if (result.isCovered()) {
                        return new CheckResult(List.of("HISCOX_COVERAGE"), List.of(), List.of());
                    } else {
                        return new CheckResult(List.of(), List.of(),
                                List.of(VerificationAlert.builder()
                                        .code("HISCOX_MANUAL_REVIEW")
                                        .message("Couverture Hiscox à vérifier manuellement")
                                        .severity(AlertSeverity.WARNING)
                                        .blocking(false)
                                        .build()));
                    }
                });
    }

    private void mergeResult(CheckResult result, List<String> passed, List<String> failed, List<VerificationAlert> alerts) {
        passed.addAll(result.passed());
        failed.addAll(result.failed());
        alerts.addAll(result.alerts());
    }

    private VerificationStatus deriveStatus(List<String> failed, List<VerificationAlert> alerts) {
        if (!failed.isEmpty()) return VerificationStatus.BLOCKED;
        boolean hasCritical = alerts.stream().anyMatch(a -> a.getSeverity() == AlertSeverity.CRITICAL);
        if (hasCritical) return VerificationStatus.ESCALATED;
        boolean hasWarning = alerts.stream().anyMatch(a -> a.getSeverity() == AlertSeverity.WARNING);
        if (hasWarning) return VerificationStatus.PARTIAL;
        return VerificationStatus.VERIFIED;
    }

    private record CheckResult(List<String> passed, List<String> failed, List<VerificationAlert> alerts) {}
}
