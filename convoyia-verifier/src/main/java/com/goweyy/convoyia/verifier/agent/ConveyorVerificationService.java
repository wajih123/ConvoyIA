package com.goweyy.convoyia.verifier.agent;

import com.goweyy.convoyia.common.domain.enums.AlertSeverity;
import com.goweyy.convoyia.common.domain.enums.VerificationStatus;
import com.goweyy.convoyia.common.domain.enums.VehicleSegment;
import com.goweyy.convoyia.common.domain.records.ConveyorData;
import com.goweyy.convoyia.common.domain.records.VerificationAlert;
import com.goweyy.convoyia.common.domain.records.VerificationBlock;
import com.goweyy.convoyia.common.domain.records.VerificationRequest;
import com.goweyy.convoyia.verifier.integration.KeycloakBiometricAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConveyorVerificationService {

    private final KeycloakBiometricAdapter keycloakBiometricAdapter;

    public Mono<VerificationBlock> verify(VerificationRequest request) {
        var conveyor = request.getConveyorData();
        if (conveyor == null) {
            List<String> failed = List.of("CONVEYOR_DATA_MISSING");
            return Mono.just(buildBlock(new ArrayList<>(), new ArrayList<>(failed), new ArrayList<>(), "Données convoyeur manquantes"));
        }

        return Mono.zip(
                checkPermis(conveyor),
                checkCasierB3(conveyor),
                checkHabilitation(conveyor, request.getVehicleSegment()),
                checkReputationScore(conveyor),
                keycloakBiometricAdapter.verifyIdentity(conveyor.getConveyorId())
        ).map(tuple -> {
            List<String> passed = new ArrayList<>();
            List<String> failed = new ArrayList<>();
            List<VerificationAlert> alerts = new ArrayList<>();

            mergeCheckResult(tuple.getT1(), passed, failed, alerts);
            mergeCheckResult(tuple.getT2(), passed, failed, alerts);
            mergeCheckResult(tuple.getT3(), passed, failed, alerts);
            mergeCheckResult(tuple.getT4(), passed, failed, alerts);

            // Biometric check
            if (Boolean.TRUE.equals(tuple.getT5())) {
                passed.add("BIOMETRIC_VERIFIED");
            } else {
                failed.add("BIOMETRIC_FAILED");
                alerts.add(VerificationAlert.builder()
                        .code("BIOMETRIC_FAILED")
                        .message("Vérification biométrique échouée")
                        .severity(AlertSeverity.CRITICAL)
                        .blocking(true)
                        .build());
            }

            return buildBlock(passed, failed, alerts, null);
        });
    }

    private Mono<CheckResult> checkPermis(ConveyorData conveyor) {
        List<String> passed = new ArrayList<>();
        List<String> failed = new ArrayList<>();
        List<VerificationAlert> alerts = new ArrayList<>();

        boolean permisValid = conveyor.getPermisExpiry() != null
                && conveyor.getPermisExpiry().isAfter(LocalDate.now())
                && conveyor.getPermisCategories() != null
                && conveyor.getPermisCategories().contains("B");

        if (permisValid) {
            passed.add("PERMIS_VALIDE");
        } else {
            failed.add("PERMIS_INVALIDE");
            alerts.add(VerificationAlert.builder()
                    .code("PERMIS_INVALIDE")
                    .message("Permis de conduire invalide ou expiré")
                    .severity(AlertSeverity.CRITICAL)
                    .blocking(true)
                    .build());
        }
        return Mono.just(new CheckResult(passed, failed, alerts));
    }

    private Mono<CheckResult> checkCasierB3(ConveyorData conveyor) {
        List<String> passed = new ArrayList<>();
        List<String> failed = new ArrayList<>();
        List<VerificationAlert> alerts = new ArrayList<>();

        if (conveyor.getCasierB3Date() == null) {
            failed.add("CASIER_B3_ABSENT");
            alerts.add(VerificationAlert.builder()
                    .code("CASIER_B3_ABSENT")
                    .message("Casier judiciaire B3 absent")
                    .severity(AlertSeverity.CRITICAL)
                    .blocking(true)
                    .build());
        } else {
            long daysOld = ChronoUnit.DAYS.between(conveyor.getCasierB3Date(), LocalDate.now());
            if (daysOld <= 90) {
                passed.add("CASIER_B3_VALIDE");
            } else {
                failed.add("CASIER_B3_EXPIRE");
                alerts.add(VerificationAlert.builder()
                        .code("CASIER_B3_EXPIRE")
                        .message("Casier judiciaire B3 expiré (" + daysOld + " jours)")
                        .severity(AlertSeverity.CRITICAL)
                        .blocking(true)
                        .build());
            }
        }
        return Mono.just(new CheckResult(passed, failed, alerts));
    }

    private Mono<CheckResult> checkHabilitation(ConveyorData conveyor, VehicleSegment segment) {
        List<String> passed = new ArrayList<>();
        List<String> failed = new ArrayList<>();
        List<VerificationAlert> alerts = new ArrayList<>();

        if (segment == VehicleSegment.LUXE_PLATEAU && !conveyor.isHabilitationLuxe()) {
            failed.add("HABILITATION_LUXE_REQUIRED");
            alerts.add(VerificationAlert.builder()
                    .code("HABILITATION_LUXE_REQUIRED")
                    .message("Habilitation Luxe requise pour le segment LUXE_PLATEAU")
                    .severity(AlertSeverity.CRITICAL)
                    .blocking(true)
                    .build());
        } else {
            passed.add("HABILITATION_OK");
        }
        return Mono.just(new CheckResult(passed, failed, alerts));
    }

    private Mono<CheckResult> checkReputationScore(ConveyorData conveyor) {
        List<String> passed = new ArrayList<>();
        List<String> failed = new ArrayList<>();
        List<VerificationAlert> alerts = new ArrayList<>();

        double score = conveyor.getReputationScore();
        if (score >= 4.0) {
            passed.add("REPUTATION_EXCELLENTE");
        } else if (score >= 3.0) {
            passed.add("REPUTATION_OK");
            alerts.add(VerificationAlert.builder()
                    .code("REPUTATION_MOYENNE")
                    .message("Score de réputation moyen: " + score)
                    .severity(AlertSeverity.INFO)
                    .blocking(false)
                    .build());
        } else {
            failed.add("REPUTATION_INSUFFISANTE");
            alerts.add(VerificationAlert.builder()
                    .code("REPUTATION_INSUFFISANTE")
                    .message("Score de réputation insuffisant: " + score)
                    .severity(AlertSeverity.WARNING)
                    .blocking(false)
                    .build());
        }
        return Mono.just(new CheckResult(passed, failed, alerts));
    }

    private void mergeCheckResult(CheckResult result, List<String> passed, List<String> failed, List<VerificationAlert> alerts) {
        passed.addAll(result.passed());
        failed.addAll(result.failed());
        alerts.addAll(result.alerts());
    }

    private VerificationBlock buildBlock(List<String> passed, List<String> failed,
                                          List<VerificationAlert> alerts, String llmReasoning) {
        VerificationStatus status = deriveStatus(failed, alerts);
        return VerificationBlock.builder()
                .blockId(UUID.randomUUID().toString())
                .status(status)
                .passed(passed)
                .failed(failed)
                .alerts(alerts)
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

    private record CheckResult(List<String> passed, List<String> failed, List<VerificationAlert> alerts) {}
}
