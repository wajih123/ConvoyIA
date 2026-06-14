package com.goweyy.convoyia.common.llm;

import com.goweyy.convoyia.common.domain.enums.ConvoyLlmModel;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ConvoyLlmRequest {
    ConvoyLlmModel model;
    String prompt;
    @Builder.Default
    int maxTokens = 1000;
}
