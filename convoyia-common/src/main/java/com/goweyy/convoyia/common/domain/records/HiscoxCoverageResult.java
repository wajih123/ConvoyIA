package com.goweyy.convoyia.common.domain.records;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class HiscoxCoverageResult {
    boolean covered;
    String reason;
    boolean requiresManualQuote;
}
