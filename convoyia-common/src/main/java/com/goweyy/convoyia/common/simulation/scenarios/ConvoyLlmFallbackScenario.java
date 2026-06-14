package com.goweyy.convoyia.common.simulation.scenarios;

import com.goweyy.convoyia.common.domain.enums.ConvoyLlmModel;
import com.goweyy.convoyia.common.llm.ConvoyLlmRequest;
import com.goweyy.convoyia.common.simulation.ConvoyScenario;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Validates LLM fallback request construction:
 * - ConvoyLlmRequest is built correctly
 * - maxTokens defaults to 1000
 * - model field is accepted
 */
@Slf4j
@Component
public class ConvoyLlmFallbackScenario implements ConvoyScenario {

    @Override
    public String name() { return "ConvoyLlmFallbackScenario"; }

    @Override
    public void run() {
        log.info("[Scenario] {} — starting", name());

        // Validate ConvoyLlmRequest builder with default tokens
        ConvoyLlmRequest defaultRequest = ConvoyLlmRequest.builder()
                .model(ConvoyLlmModel.CLAUDE_SONNET)
                .prompt("Verify mission feasibility for Paris to Lyon.")
                .build();

        assertThat(defaultRequest.getPrompt() != null && !defaultRequest.getPrompt().isEmpty(),
                "Request prompt must not be null or empty");
        assertThat(defaultRequest.getMaxTokens() == 1000,
                "Default maxTokens should be 1000, got " + defaultRequest.getMaxTokens());

        // Validate custom tokens
        ConvoyLlmRequest customRequest = ConvoyLlmRequest.builder()
                .model(ConvoyLlmModel.CLAUDE_SONNET)
                .prompt("Evaluate driver background check.")
                .maxTokens(512)
                .build();
        assertThat(customRequest.getMaxTokens() == 512,
                "Custom maxTokens should be 512, got " + customRequest.getMaxTokens());

        // Validate all LLM models are accessible
        assertThat(ConvoyLlmModel.values().length > 0,
                "At least one LLM model must be defined");

        // Use the correct model constant
        ConvoyLlmRequest claudeRequest = ConvoyLlmRequest.builder()
                .model(ConvoyLlmModel.CLAUDE_SONNET)
                .prompt("Fallback test prompt.")
                .build();

        log.info("[Scenario] {} — PASSED (request builder validated, models={})",
                name(), ConvoyLlmModel.values().length);
    }

    private void assertThat(boolean condition, String message) {
        if (!condition) throw new AssertionError("[" + name() + "] " + message);
    }
}
