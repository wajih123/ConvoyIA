package com.goweyy.convoyia.common.repository;

import com.goweyy.convoyia.common.domain.enums.ConvoyMissionState;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "convoy_mission_contexts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConvoyMissionContext {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "mission_id", nullable = false, unique = true)
    private UUID missionId;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_state")
    private ConvoyMissionState currentState;

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_segment")
    private com.goweyy.convoyia.common.domain.enums.ConvoyVehicleSegment vehicleSegment;

    @Enumerated(EnumType.STRING)
    @Column(name = "urgency")
    private com.goweyy.convoyia.common.domain.enums.ConvoyUrgency urgency;

    @Column(name = "confidence_score")
    private Double confidenceScore;

    @Column(name = "assigned_driver_id")
    private String assignedDriverId;

    @Column(name = "estimated_duration_min")
    private Integer estimatedDurationMin;

    @Column(name = "origin_address")
    private String originAddress;

    @Column(name = "destination_address")
    private String destinationAddress;

    @Column(name = "client_aboard")
    private boolean clientAboard;

    @Lob
    @Column(name = "agent_trace", columnDefinition = "TEXT")
    private String agentTrace;

    @Lob
    @Column(name = "enriched_data", columnDefinition = "TEXT")
    private String enrichedData;

    @Column(name = "last_updated")
    private Instant lastUpdated;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        lastUpdated = now;
    }

    @PreUpdate
    public void preUpdate() {
        lastUpdated = Instant.now();
    }
}
