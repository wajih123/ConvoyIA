package com.goweyy.convoyia.verifier.convoy.agent;

import com.goweyy.convoyia.verifier.convoy.dto.ConvoyVerificationBlock;
import com.goweyy.convoyia.verifier.convoy.dto.ConvoyVerificationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConvoyVehicleVerifierTest {

    private ConvoyVehicleVerifier verifier;

    @BeforeEach
    void setUp() {
        verifier = new ConvoyVehicleVerifier();
    }

    @Test
    void verify_returns_passed_block() {
        ConvoyVerificationBlock block = verifier.verify(buildRequest("AA-123-BB"));
        assertThat(block.isPassed()).isTrue();
    }

    @Test
    void verify_block_name_is_vehicle() {
        ConvoyVerificationBlock block = verifier.verify(buildRequest("AA-123-BB"));
        assertThat(block.getBlockName()).isEqualTo("VEHICLE");
    }

    @Test
    void verify_alerts_are_empty_when_passed() {
        ConvoyVerificationBlock block = verifier.verify(buildRequest("BB-456-CC"));
        assertThat(block.getAlerts()).isEmpty();
    }

    @Test
    void verify_details_is_not_null() {
        ConvoyVerificationBlock block = verifier.verify(buildRequest("CC-789-DD"));
        assertThat(block.getDetails()).isNotNull();
    }

    private ConvoyVerificationRequest buildRequest(String plate) {
        return ConvoyVerificationRequest.builder()
                .missionId("mission-002")
                .tenantId("tenant-001")
                .driverId("driver-001")
                .vehiclePlate(plate)
                .vehicleBrand("Peugeot")
                .vehicleModel("308")
                .vehicleDeclaredValue(20_000)
                .build();
    }
}
