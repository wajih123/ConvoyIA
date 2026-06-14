package com.goweyy.convoyia.common.domain.records;

import com.goweyy.convoyia.common.domain.enums.VehicleSegment;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.time.LocalDateTime;

@Value
@Builder
@Jacksonized
public class VerificationRequest {
    String missionId;
    String tenantId;
    VehicleData vehicleData;
    ConveyorData conveyorData;
    String originAddress;
    String destinationAddress;
    LocalDateTime requestedAt;
    VehicleSegment vehicleSegment;
    /**
     * Maximum age in days for the background check document.
     * Defaults to 90 days (France / Casier B3) when not provided.
     * Override per tenant: 365 for UK DBS Check, 180 for UAE Police Clearance, etc.
     */
    @lombok.Builder.Default
    Integer backgroundCheckMaxAgeDays = 90;
}
