package com.goweyy.convoyia.common.domain.records;

import com.goweyy.convoyia.common.domain.enums.VerificationStatus;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;

@Value
@Builder
public class VerificationResult {
    String missionId;
    String tenantId;
    VerificationStatus globalStatus;
    VerificationBlock vehicleBlock;
    VerificationBlock conveyorBlock;
    VerificationBlock missionBlock;
    List<String> blockingReasons;
    List<VerificationAlert> alerts;
    boolean hiscoxCoverageConfirmed;
    Instant verifiedAt;
    String verifiedBy;
}
