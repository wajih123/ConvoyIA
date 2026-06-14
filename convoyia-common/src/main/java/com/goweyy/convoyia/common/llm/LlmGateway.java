package com.goweyy.convoyia.common.llm;

import com.goweyy.convoyia.common.domain.records.LlmRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
@RequiredArgsConstructor
public class LlmGateway {

    private final OllamaAdapter ollamaAdapter;
    private final FallbackLlmAdapter fallbackLlmAdapter;

    private static final Duration OLLAMA_TIMEOUT = Duration.ofSeconds(8);

    public Mono<String> invoke(LlmRequest request) {
        return ollamaAdapter.invoke(request)
                .timeout(OLLAMA_TIMEOUT)
                .onErrorResume(ex -> {
                    if (ex instanceof TimeoutException) {
                        log.warn("Ollama timed out for model={}, falling back to Claude Sonnet", request.getModel());
                    } else {
                        log.warn("Ollama error for model={}: {}, falling back to Claude Sonnet", request.getModel(), ex.getMessage());
                    }
                    return fallbackLlmAdapter.invoke(request);
                });
    }
}
