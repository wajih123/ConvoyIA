package com.goweyy.convoyia.verifier.convoy.agent;

import com.goweyy.convoyia.verifier.convoy.dto.ConvoyVerificationBlock;
import com.goweyy.convoyia.verifier.convoy.dto.ConvoyVerificationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConvoyDriverVerifierTest {

    private ConvoyDriverVerifier verifier;

    @BeforeEach
    void setUp() {
        verifier = new ConvoyDriverVerifier();
    }

    @Test
    void verify_returns_passed_block() {
        ConvoyVerificationRequest req = buildRequest("driver-001");
        ConvoyVerificationBlock block = verifier.verify(req);
        assertThat(block.isPassed()).isTrue();
    }

    @Test
    void verify_block_name_is_driver() {
        ConvoyVerificationRequest req = buildRequest("driver-001");
        ConvoyVerificationBlock block = verifier.verify(req);
        assertThat(block.getBlockName()).isEqualTo("DRIVER");
    }

    @Test
    void verify_alerts_are_empty_when_passed() {
        ConvoyVerificationRequest req = buildRequest("driver-001");
        ConvoyVerificationBlock block = verifier.verify(req);
        assertThat(block.getAlerts()).isEmpty();
    }

    @Test
    void verify_details_is_not_null() {
        ConvoyVerificationRequest req = buildRequest("driver-002");
        ConvoyVerificationBlock block = verifier.verify(req);
        assertThat(block.getDetails()).isNotNull();
    }

    private ConvoyVerificationRequest buildRequest(String driverId) {
        return ConvoyVerificationRequest.builder()
                .missionId("mission-001")
                .tenantId("tenant-001")
                .driverId(driverId)
                .vehiclePlate("AA-123-BB")
                .build();
    }
}
