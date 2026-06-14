package com.goweyy.convoyia.verifier.convoy.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ConvoyVerificationBlock {
    private String blockName;
    private boolean passed;
    private String details;
    private List<ConvoyVerificationAlert> alerts;
}
