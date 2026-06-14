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
}
