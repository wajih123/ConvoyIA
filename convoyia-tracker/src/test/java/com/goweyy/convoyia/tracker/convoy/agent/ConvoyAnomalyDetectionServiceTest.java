package com.goweyy.convoyia.tracker.convoy.agent;

import com.goweyy.convoyia.common.domain.enums.ConvoyAlertSeverity;
import com.goweyy.convoyia.tracker.convoy.dto.ConvoyAnomalyAlert;
import com.goweyy.convoyia.tracker.convoy.dto.ConvoyGpsPosition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ConvoyAnomalyDetectionServiceTest {

    private ConvoyAnomalyDetectionService service;

    @BeforeEach
    void setUp() {
        service = new ConvoyAnomalyDetectionService();
    }

    // ── Normal speeds — no anomaly ────────────────────────────────────────────

    @ParameterizedTest
    @ValueSource(doubles = {0.0, 50.0, 90.0, 110.0, 130.0})
    void normal_speed_returns_empty(double speedKmh) {
        ConvoyGpsPosition pos = buildPosition("m1", "t1", speedKmh);
        assertThat(service.detectAnomaly(pos)).isEmpty();
    }

    // ── Speeds above limit — anomaly detected ─────────────────────────────────

    @ParameterizedTest
    @ValueSource(doubles = {130.1, 150.0, 200.0, 300.0})
    void over_limit_speed_returns_anomaly(double speedKmh) {
        ConvoyGpsPosition pos = buildPosition("m2", "t1", speedKmh);
        Optional<ConvoyAnomalyAlert> result = service.detectAnomaly(pos);
        assertThat(result).isPresent();
    }

    @Test
    void anomaly_type_is_speed_exceeded() {
        ConvoyGpsPosition pos = buildPosition("m3", "t1", 160.0);
        ConvoyAnomalyAlert alert = service.detectAnomaly(pos).orElseThrow();
        assertThat(alert.getAnomalyType()).isEqualTo("SPEED_EXCEEDED");
    }

    @Test
    void anomaly_severity_is_warning() {
        ConvoyGpsPosition pos = buildPosition("m4", "t1", 160.0);
        ConvoyAnomalyAlert alert = service.detectAnomaly(pos).orElseThrow();
        assertThat(alert.getSeverity()).isEqualTo(ConvoyAlertSeverity.WARNING);
    }

    @Test
    void anomaly_contains_mission_and_tenant_ids() {
        ConvoyGpsPosition pos = buildPosition("mission-xyz", "tenant-abc", 180.0);
        ConvoyAnomalyAlert alert = service.detectAnomaly(pos).orElseThrow();
        assertThat(alert.getMissionId()).isEqualTo("mission-xyz");
        assertThat(alert.getTenantId()).isEqualTo("tenant-abc");
    }

    @Test
    void anomaly_description_contains_actual_speed() {
        double speed = 155.5;
        ConvoyGpsPosition pos = buildPosition("m5", "t1", speed);
        ConvoyAnomalyAlert alert = service.detectAnomaly(pos).orElseThrow();
        assertThat(alert.getDescription()).contains("155.5");
    }

    @Test
    void anomaly_description_contains_limit() {
        ConvoyGpsPosition pos = buildPosition("m6", "t1", 200.0);
        ConvoyAnomalyAlert alert = service.detectAnomaly(pos).orElseThrow();
        assertThat(alert.getDescription()).contains("130.0");
    }

    @Test
    void anomaly_has_detected_at_timestamp() {
        Instant before = Instant.now();
        ConvoyGpsPosition pos = buildPosition("m7", "t1", 200.0);
        ConvoyAnomalyAlert alert = service.detectAnomaly(pos).orElseThrow();
        assertThat(alert.getDetectedAt()).isAfterOrEqualTo(before);
    }

    @Test
    void anomaly_carries_position_reference() {
        ConvoyGpsPosition pos = ConvoyGpsPosition.builder()
                .missionId("m8")
                .tenantId("t1")
                .driverId("driver-001")
                .latitude(48.8566)
                .longitude(2.3522)
                .speedKmh(200.0)
                .recordedAt(Instant.now())
                .build();
        ConvoyAnomalyAlert alert = service.detectAnomaly(pos).orElseThrow();
        assertThat(alert.getPosition().getLatitude()).isEqualTo(48.8566);
        assertThat(alert.getPosition().getLongitude()).isEqualTo(2.3522);
    }

    // ── Boundary at exactly 130 km/h ─────────────────────────────────────────

    @Test
    void exactly_130_kmh_returns_empty_no_anomaly() {
        ConvoyGpsPosition pos = buildPosition("m9", "t1", 130.0);
        assertThat(service.detectAnomaly(pos)).isEmpty();
    }

    @Test
    void just_above_130_kmh_returns_anomaly() {
        ConvoyGpsPosition pos = buildPosition("m10", "t1", 130.01);
        assertThat(service.detectAnomaly(pos)).isPresent();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private ConvoyGpsPosition buildPosition(String missionId, String tenantId, double speedKmh) {
        return ConvoyGpsPosition.builder()
                .missionId(missionId)
                .tenantId(tenantId)
                .driverId("driver-001")
                .latitude(48.8566)
                .longitude(2.3522)
                .speedKmh(speedKmh)
                .recordedAt(Instant.now())
                .build();
    }
}
