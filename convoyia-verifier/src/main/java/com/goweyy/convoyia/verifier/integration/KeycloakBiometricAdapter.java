package com.goweyy.convoyia.verifier.integration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class KeycloakBiometricAdapter {

    private final WebClient.Builder webClientBuilder;

    @Value("${keycloak.admin-url:http://keycloak:8080}")
    private String keycloakAdminUrl;

    @Value("${keycloak.realm:convoyia}")
    private String realm;

    @Value("${keycloak.admin-token:}")
    private String adminToken;

    public Mono<Boolean> verifyIdentity(String conveyorId) {
        return webClientBuilder.build()
                .get()
                .uri(keycloakAdminUrl + "/admin/realms/" + realm + "/users/" + conveyorId + "/sessions")
                .header("Authorization", "Bearer " + adminToken)
                .retrieve()
                .bodyToMono(Map.class)
                .map(attrs -> {
                    String authMethod = (String) attrs.get("auth_method");
                    String biometricVerified = (String) attrs.get("biometric_verified");
                    return "biometric".equals(authMethod) && "true".equals(biometricVerified);
                })
                .onErrorReturn(false)
                .doOnError(e -> log.warn("Keycloak biometric check failed for conveyorId={}: {}", conveyorId, e.getMessage()));
    }
}
