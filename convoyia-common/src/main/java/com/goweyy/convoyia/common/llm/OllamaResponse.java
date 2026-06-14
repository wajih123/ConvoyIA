package com.goweyy.convoyia.common.llm;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class OllamaResponse {
    private String model;
    private String response;
    @JsonProperty("done")
    private boolean done;
}
