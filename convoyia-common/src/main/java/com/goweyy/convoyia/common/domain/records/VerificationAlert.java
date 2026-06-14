package com.goweyy.convoyia.common.domain.records;

import com.goweyy.convoyia.common.domain.enums.AlertSeverity;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class VerificationAlert {
    String code;
    String message;
    AlertSeverity severity;
    boolean blocking;
}
