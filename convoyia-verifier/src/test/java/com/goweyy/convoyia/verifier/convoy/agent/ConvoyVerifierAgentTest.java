package com.goweyy.convoyia.verifier.convoy.agent;

import com.goweyy.convoyia.common.domain.enums.ConvoyVerificationStatus;
import com.goweyy.convoyia.verifier.convoy.dto.ConvoyVerificationBlock;
import com.goweyy.convoyia.verifier.convoy.dto.ConvoyVerificationRequest;
import com.goweyy.convoyia.verifier.convoy.dto.ConvoyVerificationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConvoyVerifierAgentTest {

    @Mock
    private ConvoyDriverVerifier driverVerifier;
    @Mock
    private ConvoyVehicleVerifier vehicleVerifier;
    @Mock
    private ConvoyMissionVerifier missionVerifier;

    @InjectMocks
    private ConvoyVerifierAgent verifierAgent;

    private ConvoyVerificationRequest request;

    @BeforeEach
    void setUp() {
        request = ConvoyVerificationRequest.builder()
                .missionId("mission-verify-001")
                .tenantId("tenant-goweyy")
                .driverId("driver-001")
                .vehiclePlate("AA-123-BB")
                .vehicleBrand("Renault")
                .vehicleModel("Megane")
                .vehicleDeclaredValue(25_000)
                .build();
    }

    // ── All pass → VERIFIED ────────────────────────────────────────────────

    @Test
    void all_blocks_pass_returns_verified_status() {
        when(driverVerifier.verify(any())).thenReturn(passBlock("DRIVER"));
        when(vehicleVerifier.verify(any())).thenReturn(passBlock("VEHICLE"));
        when(missionVerifier.verify(any())).thenReturn(passBlock("MISSION"));

        ConvoyVerificationResult result = verifierAgent.verify(request);
        assertThat(result.getStatus()).isEqualTo(ConvoyVerificationStatus.VERIFIED);
    }

    @Test
    void verified_result_has_three_blocks() {
        when(driverVerifier.verify(any())).thenReturn(passBlock("DRIVER"));
        when(vehicleVerifier.verify(any())).thenReturn(passBlock("VEHICLE"));
        when(missionVerifier.verify(any())).thenReturn(passBlock("MISSION"));

        ConvoyVerificationResult result = verifierAgent.verify(request);
        assertThat(result.getBlocks()).hasSize(3);
    }

    // ── One block fails with alerts → BLOCKED ─────────────────────────────

    @Test
    void driver_blocked_returns_blocked_status() {
        when(driverVerifier.verify(any())).thenReturn(failBlock("DRIVER"));
        when(vehicleVerifier.verify(any())).thenReturn(passBlock("VEHICLE"));
        when(missionVerifier.verify(any())).thenReturn(passBlock("MISSION"));

        ConvoyVerificationResult result = verifierAgent.verify(request);
        assertThat(result.getStatus()).isEqualTo(ConvoyVerificationStatus.BLOCKED);
    }

    @Test
    void vehicle_blocked_returns_blocked_status() {
        when(driverVerifier.verify(any())).thenReturn(passBlock("DRIVER"));
        when(vehicleVerifier.verify(any())).thenReturn(failBlock("VEHICLE"));
        when(missionVerifier.verify(any())).thenReturn(passBlock("MISSION"));

        ConvoyVerificationResult result = verifierAgent.verify(request);
        assertThat(result.getStatus()).isEqualTo(ConvoyVerificationStatus.BLOCKED);
    }

    @Test
    void mission_blocked_returns_blocked_status() {
        when(driverVerifier.verify(any())).thenReturn(passBlock("DRIVER"));
        when(vehicleVerifier.verify(any())).thenReturn(passBlock("VEHICLE"));
        when(missionVerifier.verify(any())).thenReturn(failBlock("MISSION"));

        ConvoyVerificationResult result = verifierAgent.verify(request);
        assertThat(result.getStatus()).isEqualTo(ConvoyVerificationStatus.BLOCKED);
    }

    @Test
    void all_blocked_returns_blocked_status() {
        when(driverVerifier.verify(any())).thenReturn(failBlock("DRIVER"));
        when(vehicleVerifier.verify(any())).thenReturn(failBlock("VEHICLE"));
        when(missionVerifier.verify(any())).thenReturn(failBlock("MISSION"));

        ConvoyVerificationResult result = verifierAgent.verify(request);
        assertThat(result.getStatus()).isEqualTo(ConvoyVerificationStatus.BLOCKED);
    }

    // ── Mission and tenant propagation ────────────────────────────────────

    @Test
    void result_propagates_mission_and_tenant_id() {
        when(driverVerifier.verify(any())).thenReturn(passBlock("DRIVER"));
        when(vehicleVerifier.verify(any())).thenReturn(passBlock("VEHICLE"));
        when(missionVerifier.verify(any())).thenReturn(passBlock("MISSION"));

        ConvoyVerificationResult result = verifierAgent.verify(request);
        assertThat(result.getMissionId()).isEqualTo("mission-verify-001");
        assertThat(result.getTenantId()).isEqualTo("tenant-goweyy");
    }

    @Test
    void result_has_verified_at_timestamp() {
        when(driverVerifier.verify(any())).thenReturn(passBlock("DRIVER"));
        when(vehicleVerifier.verify(any())).thenReturn(passBlock("VEHICLE"));
        when(missionVerifier.verify(any())).thenReturn(passBlock("MISSION"));

        ConvoyVerificationResult result = verifierAgent.verify(request);
        assertThat(result.getVerifiedAt()).isNotNull();
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private ConvoyVerificationBlock passBlock(String name) {
        return ConvoyVerificationBlock.builder()
                .blockName(name)
                .passed(true)
                .details("ok")
                .alerts(Collections.emptyList())
                .build();
    }

    private ConvoyVerificationBlock failBlock(String name) {
        return ConvoyVerificationBlock.builder()
                .blockName(name)
                .passed(false)
                .details("failed")
                .alerts(List.of(com.goweyy.convoyia.verifier.convoy.dto.ConvoyVerificationAlert.builder()
                        .code("BLOCKED")
                        .message("Blocked reason")
                        .severity(com.goweyy.convoyia.common.domain.enums.ConvoyAlertSeverity.WARNING)
                        .build()))
                .build();
    }
}
