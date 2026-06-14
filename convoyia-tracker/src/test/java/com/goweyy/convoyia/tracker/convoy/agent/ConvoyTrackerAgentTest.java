package com.goweyy.convoyia.tracker.convoy.agent;

import com.goweyy.convoyia.tracker.convoy.dto.ConvoyAnomalyAlert;
import com.goweyy.convoyia.tracker.convoy.dto.ConvoyGpsPosition;
import com.goweyy.convoyia.tracker.convoy.dto.ConvoyTrackingEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConvoyTrackerAgentTest {

    @Mock
    private ConvoyAnomalyDetectionService anomalyDetectionService;

    @InjectMocks
    private ConvoyTrackerAgent trackerAgent;

    private ConvoyGpsPosition normalPosition;

    @BeforeEach
    void setUp() {
        normalPosition = ConvoyGpsPosition.builder()
                .missionId("mission-track-001")
                .tenantId("tenant-goweyy")
                .driverId("driver-001")
                .latitude(48.8566)
                .longitude(2.3522)
                .speedKmh(80.0)
                .recordedAt(Instant.now())
                .build();
    }

    @Test
    void normal_position_creates_position_update_event() {
        when(anomalyDetectionService.detectAnomaly(any())).thenReturn(Optional.empty());
        ConvoyTrackingEvent event = trackerAgent.track(normalPosition);
        assertThat(event.getEventType()).isEqualTo("POSITION_UPDATE");
    }

    @Test
    void anomalous_position_creates_anomaly_event() {
        ConvoyAnomalyAlert mockAlert = ConvoyAnomalyAlert.builder()
                .missionId("mission-track-001")
                .tenantId("tenant-goweyy")
                .anomalyType("SPEED_EXCEEDED")
                .build();
        when(anomalyDetectionService.detectAnomaly(any())).thenReturn(Optional.of(mockAlert));
        ConvoyTrackingEvent event = trackerAgent.track(normalPosition);
        assertThat(event.getEventType()).isEqualTo("ANOMALY");
    }

    @Test
    void tracking_event_contains_mission_and_tenant_id() {
        when(anomalyDetectionService.detectAnomaly(any())).thenReturn(Optional.empty());
        ConvoyTrackingEvent event = trackerAgent.track(normalPosition);
        assertThat(event.getMissionId()).isEqualTo("mission-track-001");
        assertThat(event.getTenantId()).isEqualTo("tenant-goweyy");
    }

    @Test
    void tracking_event_contains_position() {
        when(anomalyDetectionService.detectAnomaly(any())).thenReturn(Optional.empty());
        ConvoyTrackingEvent event = trackerAgent.track(normalPosition);
        assertThat(event.getPosition()).isEqualTo(normalPosition);
    }

    @Test
    void tracking_event_has_occurred_at_timestamp() {
        Instant before = Instant.now();
        when(anomalyDetectionService.detectAnomaly(any())).thenReturn(Optional.empty());
        ConvoyTrackingEvent event = trackerAgent.track(normalPosition);
        assertThat(event.getOccurredAt()).isAfterOrEqualTo(before);
    }
}
