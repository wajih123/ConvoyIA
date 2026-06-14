package com.goweyy.convoyia.common.domain.records;

import com.goweyy.convoyia.common.domain.enums.VerificationStatus;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class VerificationBlock {
    String blockId;
    VerificationStatus status;
    List<String> passed;
    List<String> failed;
    List<VerificationAlert> alerts;
    Double confidenceScore;
    String llmReasoning;
}
