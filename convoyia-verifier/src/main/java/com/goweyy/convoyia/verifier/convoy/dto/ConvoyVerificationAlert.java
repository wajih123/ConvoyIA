package com.goweyy.convoyia.verifier.convoy.dto;

import com.goweyy.convoyia.common.domain.enums.ConvoyAlertSeverity;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ConvoyVerificationAlert {
    private String code;
    private String message;
    private ConvoyAlertSeverity severity;
}
