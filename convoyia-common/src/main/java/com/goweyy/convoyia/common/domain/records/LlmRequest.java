package com.goweyy.convoyia.common.domain.records;

import com.goweyy.convoyia.common.domain.enums.LlmModel;
import com.goweyy.convoyia.common.domain.enums.ResponseFormat;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class LlmRequest {
    LlmModel model;
    String prompt;
    ResponseFormat expectedFormat;
    @Builder.Default
    int maxTokens = 1000;
}
