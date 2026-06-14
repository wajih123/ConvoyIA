package com.goweyy.convoyia.common.domain.records;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.util.List;

@Value
@Builder
public class ConveyorData {
    String conveyorId;
    LocalDate permisExpiry;
    List<String> permisCategories;
    LocalDate casierB3Date;
    boolean habilitationLuxe;
    double reputationScore;
}
