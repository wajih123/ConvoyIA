package com.goweyy.convoyia.inspector.convoy.agent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConvoySignatureService {

    @Value("${convoyia.signature.yousign-api-key:}")
    private String apiKey;

    @Value("${convoyia.signature.yousign-base-url:https://api.yousign.app/v3}")
    private String baseUrl;

    @Value("${convoyia.signature.enabled:false}")
    private boolean enabled;

    private final WebClient.Builder webClientBuilder;

    public String requestSignature(String pdfPath, String signerPhone) {
        if (!enabled) {
            log.info("YouSign disabled — TODO: enable via SIGNATURE_ENABLED=true. pdfPath={}", pdfPath);
            return "sig_disabled_" + UUID.randomUUID();
        }
        // TODO: YouSign API integration
        log.warn("YouSign API not yet implemented. pdfPath={} phone={}", pdfPath, signerPhone);
        return "sig_todo_" + UUID.randomUUID();
    }

    public String checkStatus(String requestId) {
        if (!enabled) {
            return "PENDING";
        }
        // TODO: GET {baseUrl}/signature_requests/{requestId}
        return "PENDING";
    }
}
