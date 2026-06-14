package com.goweyy.convoyia.verifier.convoy.agent;

import com.goweyy.convoyia.verifier.convoy.dto.ConvoyVerificationRequest;
import org.springframework.stereotype.Component;

@Component
public class ConvoyVerifierPromptBuilder {

    public String buildVerificationPrompt(ConvoyVerificationRequest request) {
        return String.format("""
                Verify the following conveyance mission:
                - Mission ID: %s
                - Tenant: %s
                - Driver ID: %s
                - Vehicle: %s %s (plate: %s)
                - Vehicle Value: %.0f EUR
                Return JSON: { "driverOk": true/false, "vehicleOk": true/false, "notes": "..." }
                """,
                request.getMissionId(), request.getTenantId(),
                request.getDriverId(), request.getVehicleBrand(), request.getVehicleModel(),
                request.getVehiclePlate(), request.getVehicleDeclaredValue());
    }
}
