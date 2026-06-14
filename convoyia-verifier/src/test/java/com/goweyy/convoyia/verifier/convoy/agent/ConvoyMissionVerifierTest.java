package com.goweyy.convoyia.verifier.convoy.agent;

import com.goweyy.convoyia.verifier.convoy.dto.ConvoyVerificationBlock;
import com.goweyy.convoyia.verifier.convoy.dto.ConvoyVerificationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConvoyMissionVerifierTest {

    private ConvoyMissionVerifier verifier;

    @BeforeEach
    void setUp() {
        verifier = new ConvoyMissionVerifier();
    }

    @Test
    void verify_returns_passed_block() {
        ConvoyVerificationBlock block = verifier.verify(buildRequest("mission-003"));
        assertThat(block.isPassed()).isTrue();
    }

    @Test
    void verify_block_name_is_mission() {
        ConvoyVerificationBlock block = verifier.verify(buildRequest("mission-003"));
        assertThat(block.getBlockName()).isEqualTo("MISSION");
    }

    @Test
    void verify_alerts_are_empty_when_passed() {
        ConvoyVerificationBlock block = verifier.verify(buildRequest("mission-004"));
        assertThat(block.getAlerts()).isEmpty();
    }

    @Test
    void verify_details_is_not_null() {
        ConvoyVerificationBlock block = verifier.verify(buildRequest("mission-005"));
        assertThat(block.getDetails()).isNotNull();
    }

    private ConvoyVerificationRequest buildRequest(String missionId) {
        return ConvoyVerificationRequest.builder()
                .missionId(missionId)
                .tenantId("tenant-001")
                .driverId("driver-001")
                .vehiclePlate("AA-123-BB")
                .vehicleBrand("Renault")
                .vehicleModel("Clio")
                .vehicleDeclaredValue(15_000)
                .build();
    }
}
