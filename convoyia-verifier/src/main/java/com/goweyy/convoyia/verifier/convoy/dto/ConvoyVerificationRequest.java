package com.goweyy.convoyia.verifier.convoy.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

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
    private String licenseCategories;
    private LocalDate licenseExpiryDate;
    private LocalDate backgroundCheckDate;
    private boolean habilitationLuxe;
    private double driverReputationScore;
    private String keycloakSessionId;
    private String carteGriseUrl;
    private LocalDate assuranceExpiryDate;
    private LocalDate controleTechniqueDate;
    private LocalDateTime requestedAt;
    private String backgroundCheckDocName;
    private int backgroundCheckMaxAgeDays;
    private BigDecimal insuranceCeilingAmount;
    private String originAddress;
    private String destinationAddress;
}
