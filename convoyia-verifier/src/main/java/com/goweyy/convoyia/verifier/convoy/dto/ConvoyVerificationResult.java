package com.goweyy.convoyia.verifier.convoy.dto;

import com.goweyy.convoyia.common.domain.enums.ConvoyVerificationStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
public class ConvoyVerificationResult {
    private String missionId;
    private String tenantId;
    private ConvoyVerificationStatus status;
    private List<ConvoyVerificationBlock> blocks;
    private List<ConvoyVerificationAlert> alerts;
    private Instant verifiedAt;
}
