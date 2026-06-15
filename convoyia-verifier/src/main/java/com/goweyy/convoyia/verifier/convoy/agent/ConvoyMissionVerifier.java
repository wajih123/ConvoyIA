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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConvoyMissionVerifier {

    private final ConvoyLlmGateway llmGateway;
    private final ObjectMapper objectMapper;

    public ConvoyVerificationBlock verify(ConvoyVerificationRequest request) {
        log.debug("Verifying mission missionId={}", request.getMissionId());
        List<ConvoyVerificationAlert> alerts = new ArrayList<>();
        List<String> details = new ArrayList<>();
        boolean passed = true;

        try {
            String response = llmGateway.invoke(ConvoyLlmRequest.builder()
                    .model(ConvoyLlmModel.PHI3_MINI)
                    .prompt("Are these addresses valid?: origin=[%s] destination=[%s]. Respond JSON only as {\"valid\":true|false,\"reason\":\"...\"}."
                            .formatted(request.getOriginAddress(), request.getDestinationAddress()))
                    .maxTokens(300)
                    .build());
            Map<?, ?> parsed = objectMapper.readValue(response, Map.class);
            if (Boolean.TRUE.equals(parsed.get("valid"))) {
                details.add("ADDRESSES_OK");
            } else {
                passed = false;
                alerts.add(alert("ADDRESSES_INVALID", "Origin or destination address is invalid", ConvoyAlertSeverity.CRITICAL));
            }
        } catch (Exception exception) {
            log.warn("Address validation unavailable for missionId={}: {}", request.getMissionId(), exception.getMessage());
            alerts.add(alert("ADDRESS_VALIDATION_UNAVAILABLE", "Address validation unavailable", ConvoyAlertSeverity.WARNING));
        }

        if (request.getRequestedAt() != null) {
            if (request.getRequestedAt().isBefore(LocalDateTime.now())) {
                passed = false;
                alerts.add(alert("REQUESTED_AT_PAST", "Requested time is in the past", ConvoyAlertSeverity.CRITICAL));
            } else {
                details.add("TIMESLOT_OK");
                int hour = request.getRequestedAt().getHour();
                if (hour >= 22 || hour < 6) {
                    alerts.add(alert("NIGHT_BONUS_APPLICABLE", "Night mission bonus applies", ConvoyAlertSeverity.INFO));
                }
            }
        }

        BigDecimal ceiling = request.getInsuranceCeilingAmount();
        if (ceiling != null && ceiling.compareTo(BigDecimal.ZERO) > 0
                && BigDecimal.valueOf(request.getVehicleDeclaredValue()).compareTo(ceiling) > 0) {
            passed = false;
            alerts.add(alert("HISCOX_CEILING", "Vehicle declared value exceeds insurance ceiling", ConvoyAlertSeverity.CRITICAL));
        } else {
            details.add("INSURANCE_CEILING_OK");
        }

        return ConvoyVerificationBlock.builder()
                .blockName("MISSION")
                .passed(passed)
                .details(String.join(", ", details))
                .alerts(alerts)
                .build();
    }

    private ConvoyVerificationAlert alert(String code, String message, ConvoyAlertSeverity severity) {
        return ConvoyVerificationAlert.builder()
                .code(code)
                .message(message)
                .severity(severity)
                .build();
    }
}
