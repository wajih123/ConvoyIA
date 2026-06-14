package com.goweyy.convoyia.common.llm;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeoutException;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConvoyLlmGateway {

    private final ConvoyOllamaAdapter ollamaAdapter;
    private final ConvoyFallbackLlmAdapter fallbackLlmAdapter;

    @Value("${convoyia.llm.confidence-threshold:0.72}")
    private double confidenceThreshold;

    @Value("${convoyia.llm.timeout-seconds:8}")
    private int timeoutSeconds;

    public String invoke(ConvoyLlmRequest request) {
        try {
            return ollamaAdapter.invoke(request);
        } catch (Exception ex) {
            log.warn("Ollama error for model={}: {}, falling back to Claude Sonnet",
                    request.getModel(), ex.getMessage());
            return fallbackLlmAdapter.invoke(request);
        }
    }
}
