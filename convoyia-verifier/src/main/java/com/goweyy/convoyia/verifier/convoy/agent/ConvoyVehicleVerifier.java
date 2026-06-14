package com.goweyy.convoyia.verifier.convoy.agent;

import com.goweyy.convoyia.verifier.convoy.dto.ConvoyVerificationBlock;
import com.goweyy.convoyia.verifier.convoy.dto.ConvoyVerificationRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Slf4j
@Component
public class ConvoyVehicleVerifier {

    public ConvoyVerificationBlock verify(ConvoyVerificationRequest request) {
        log.debug("Verifying vehicle plate={}", request.getVehiclePlate());
        // TODO: integrate with vehicle registration service
        return ConvoyVerificationBlock.builder()
                .blockName("VEHICLE")
                .passed(true)
                .details("Vehicle checks passed (placeholder)")
                .alerts(Collections.emptyList())
                .build();
    }
}
