package com.goweyy.convoyia.verifier.convoy.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.goweyy.convoyia.common.domain.enums.ConvoyAlertSeverity;
import com.goweyy.convoyia.common.domain.enums.ConvoyLlmModel;
import com.goweyy.convoyia.common.llm.ConvoyLlmGateway;
import com.goweyy.convoyia.common.llm.ConvoyLlmRequest;
import com.goweyy.convoyia.verifier.convoy.dto.ConvoyVerificationAlert;
import com.goweyy.convoyia.verifier.convoy.dto.ConvoyVerificationBlock;
import com.goweyy.convoyia.verifier.convoy.dto.ConvoyVerificationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConvoyVehicleVerifier {

    private final ConvoyLlmGateway llmGateway;
    private final ObjectMapper objectMapper;

    public ConvoyVerificationBlock verify(ConvoyVerificationRequest request) {
        log.debug("Verifying vehicle plate={}", request.getVehiclePlate());
        List<ConvoyVerificationAlert> alerts = new ArrayList<>();
        List<String> details = new ArrayList<>();
        boolean passed = true;
        LocalDate today = LocalDate.now();

        if (request.getCarteGriseUrl() != null && !request.getCarteGriseUrl().isBlank()) {
            details.add("CARTE_GRISE_OK");
        } else {
            passed = false;
            alerts.add(alert("CARTE_GRISE_MISSING", "Carte grise URL is missing", ConvoyAlertSeverity.CRITICAL));
        }

        if (request.getAssuranceExpiryDate() != null && request.getAssuranceExpiryDate().isAfter(today)) {
            details.add("ASSURANCE_OK");
        } else {
            passed = false;
            alerts.add(alert("ASSURANCE_EXPIRED", "Vehicle insurance is expired or missing", ConvoyAlertSeverity.CRITICAL));
        }

        if (request.getControleTechniqueDate() == null) {
            passed = false;
            alerts.add(alert("CONTROLE_TECHNIQUE_MISSING", "Controle technique date is missing", ConvoyAlertSeverity.CRITICAL));
        } else {
            long monthsOld = ChronoUnit.MONTHS.between(request.getControleTechniqueDate(), today);
            if (monthsOld > 24) {
                passed = false;
                alerts.add(alert("CONTROLE_TECHNIQUE_EXPIRED", "Controle technique is older than 24 months", ConvoyAlertSeverity.CRITICAL));
            } else if (monthsOld >= 21) {
                details.add("CONTROLE_TECHNIQUE_WARNING");
                alerts.add(alert("CONTROLE_TECHNIQUE_EXPIRING", "Controle technique is between 21 and 24 months old", ConvoyAlertSeverity.WARNING));
            } else {
                details.add("CONTROLE_TECHNIQUE_OK");
            }
        }

        try {
            String response = llmGateway.invoke(ConvoyLlmRequest.builder()
                    .model(ConvoyLlmModel.PHI3_MINI)
                    .prompt(buildCoherencePrompt(request))
                    .maxTokens(300)
                    .build());
            Map<?, ?> parsed = objectMapper.readValue(response, Map.class);
            boolean coherent = Boolean.TRUE.equals(parsed.get("coherent"));
            double confidence = parsed.get("confidence") instanceof Number number ? number.doubleValue() : 0.0d;
            if (coherent && confidence >= 0.7d) {
                details.add("COHERENCE_OK");
            } else {
                alerts.add(alert("VEHICLE_COHERENCE_WARNING",
                        "Vehicle coherence requires manual review", ConvoyAlertSeverity.WARNING));
            }
        } catch (Exception exception) {
            log.warn("Vehicle coherence check failed for plate={}: {}", request.getVehiclePlate(), exception.getMessage());
            alerts.add(alert("VEHICLE_COHERENCE_UNAVAILABLE", "Vehicle coherence check unavailable", ConvoyAlertSeverity.WARNING));
        }

        return ConvoyVerificationBlock.builder()
                .blockName("VEHICLE")
                .passed(passed)
                .details(String.join(", ", details))
                .alerts(alerts)
                .build();
    }

    private String buildCoherencePrompt(ConvoyVerificationRequest request) {
        return """
                Analyse vehicle coherence and respond with JSON only.
                {"coherent":true|false,"confidence":0.0,"reason":"..."}
                plate=%s brand=%s model=%s declaredValue=%s
                """.formatted(request.getVehiclePlate(), request.getVehicleBrand(), request.getVehicleModel(), request.getVehicleDeclaredValue());
    }

    private ConvoyVerificationAlert alert(String code, String message, ConvoyAlertSeverity severity) {
        return ConvoyVerificationAlert.builder()
                .code(code)
                .message(message)
                .severity(severity)
                .build();
    }
}
