package com.goweyy.convoyia.verifier.convoy.agent;

import com.goweyy.convoyia.common.domain.enums.ConvoyAlertSeverity;
import com.goweyy.convoyia.common.domain.enums.ConvoyVehicleSegment;
import com.goweyy.convoyia.verifier.convoy.dto.ConvoyVerificationAlert;
import com.goweyy.convoyia.verifier.convoy.dto.ConvoyVerificationBlock;
import com.goweyy.convoyia.verifier.convoy.dto.ConvoyVerificationRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class ConvoyDriverVerifier {

    public ConvoyVerificationBlock verify(ConvoyVerificationRequest request) {
        return verifyInternal(request, false);
    }

    public ConvoyVerificationBlock verifyExpress(ConvoyVerificationRequest request) {
        return verifyInternal(request, true);
    }

    private ConvoyVerificationBlock verifyInternal(ConvoyVerificationRequest request, boolean expressMode) {
        log.debug("Verifying driver driverId={} expressMode={}", request.getDriverId(), expressMode);
        List<ConvoyVerificationAlert> alerts = new ArrayList<>();
        List<String> details = new ArrayList<>();
        boolean passed = true;
        LocalDate today = LocalDate.now();

        boolean licenseValid = request.getLicenseExpiryDate() != null
                && request.getLicenseExpiryDate().isAfter(today)
                && containsCategory(request.getLicenseCategories(), "B");
        if (licenseValid) {
            details.add("LICENSE_OK");
        } else {
            passed = false;
            alerts.add(alert("LICENSE_INVALID", "Driving license missing category B or expired", ConvoyAlertSeverity.CRITICAL));
        }

        boolean sessionPresent = request.getKeycloakSessionId() != null && !request.getKeycloakSessionId().isBlank();
        if (sessionPresent) {
            details.add("SESSION_OK");
        } else {
            alerts.add(alert("KEYCLOAK_SESSION_MISSING", "Keycloak session not provided", ConvoyAlertSeverity.WARNING));
        }

        if (!expressMode) {
            if (request.getBackgroundCheckDate() != null && request.getBackgroundCheckMaxAgeDays() > 0) {
                long ageDays = ChronoUnit.DAYS.between(request.getBackgroundCheckDate(), today);
                if (ageDays <= request.getBackgroundCheckMaxAgeDays()) {
                    details.add("BACKGROUND_CHECK_OK");
                } else {
                    passed = false;
                    String docName = request.getBackgroundCheckDocName() != null && !request.getBackgroundCheckDocName().isBlank()
                            ? request.getBackgroundCheckDocName() : "background check";
                    alerts.add(alert("BACKGROUND_CHECK_EXPIRED",
                            docName + " exceeds max age of " + request.getBackgroundCheckMaxAgeDays() + " days",
                            ConvoyAlertSeverity.CRITICAL));
                }
            } else {
                passed = false;
                alerts.add(alert("BACKGROUND_CHECK_MISSING", "Background check date is missing", ConvoyAlertSeverity.CRITICAL));
            }

            if (ConvoyVehicleSegment.fromValue(request.getVehicleDeclaredValue()) == ConvoyVehicleSegment.LUXE_PLATEAU
                    && !request.isHabilitationLuxe()) {
                passed = false;
                alerts.add(alert("LUXE_HABILITATION_REQUIRED", "Luxe habilitation is required for LUXE_PLATEAU missions", ConvoyAlertSeverity.CRITICAL));
            } else {
                details.add("SEGMENT_AUTH_OK");
            }

            if (request.getDriverReputationScore() >= 4.0d) {
                details.add("REPUTATION_EXCELLENT");
            } else if (request.getDriverReputationScore() >= 3.0d) {
                details.add("REPUTATION_OK");
                alerts.add(alert("REPUTATION_OK", "Driver reputation acceptable but below excellence threshold", ConvoyAlertSeverity.INFO));
            } else {
                passed = false;
                alerts.add(alert("REPUTATION_TOO_LOW", "Driver reputation below 3.0", ConvoyAlertSeverity.CRITICAL));
            }
        }

        return ConvoyVerificationBlock.builder()
                .blockName("DRIVER")
                .passed(passed)
                .details(String.join(", ", details))
                .alerts(alerts)
                .build();
    }

    private boolean containsCategory(String categories, String expected) {
        if (categories == null || categories.isBlank()) {
            return false;
        }
        for (String category : categories.split(",")) {
            if (expected.equalsIgnoreCase(category.trim())) {
                return true;
            }
        }
        return false;
    }

    private ConvoyVerificationAlert alert(String code, String message, ConvoyAlertSeverity severity) {
        return ConvoyVerificationAlert.builder()
                .code(code)
                .message(message)
                .severity(severity)
                .build();
    }
}
