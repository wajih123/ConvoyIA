package com.goweyy.convoyia.verifier.convoy.agent;

import com.goweyy.convoyia.verifier.convoy.dto.ConvoyVerificationBlock;
import com.goweyy.convoyia.verifier.convoy.dto.ConvoyVerificationRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Slf4j
@Component
public class ConvoyDriverVerifier {

    public ConvoyVerificationBlock verify(ConvoyVerificationRequest request) {
        log.debug("Verifying driver driverId={}", request.getDriverId());
        // TODO: integrate with driver service (check license, background check document, age)
        return ConvoyVerificationBlock.builder()
                .blockName("DRIVER")
                .passed(true)
                .details("Driver checks passed (placeholder)")
                .alerts(Collections.emptyList())
                .build();
    }
}
