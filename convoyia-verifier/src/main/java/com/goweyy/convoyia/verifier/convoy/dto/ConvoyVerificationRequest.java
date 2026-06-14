package com.goweyy.convoyia.verifier.convoy.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ConvoyVerificationRequest {
    private String missionId;
    private String tenantId;
    private String driverId;
    private String vehiclePlate;
    private String vehicleBrand;
    private String vehicleModel;
    private double vehicleDeclaredValue;
}
