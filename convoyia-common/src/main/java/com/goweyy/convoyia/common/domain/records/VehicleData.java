package com.goweyy.convoyia.common.domain.records;

import com.goweyy.convoyia.common.domain.enums.VehicleSegment;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;

@Value
@Builder
public class VehicleData {
    String vehicleId;
    String make;
    String model;
    int year;
    double declaredValue;
    String carteGriseUrl;
    LocalDate assuranceExpiry;
    LocalDate controleTechniqueDate;
    VehicleSegment declaredSegment;
}
