package com.goweyy.convoyia.verifier.integration;

import com.goweyy.convoyia.common.domain.records.HiscoxCoverageResult;
import com.goweyy.convoyia.common.domain.records.VehicleData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class HiscoxAdapter {

    private static final double MAX_COVERAGE_VALUE = 120_000.0;

    @Qualifier("hiscoxWebClient")
    private final WebClient hiscoxWebClient;

    public Mono<HiscoxCoverageResult> checkCoverage(VehicleData vehicle, String missionId) {
        if (vehicle.getDeclaredValue() > MAX_COVERAGE_VALUE) {
            log.info("Vehicle value {} exceeds Hiscox max coverage for missionId={}", vehicle.getDeclaredValue(), missionId);
            return Mono.just(HiscoxCoverageResult.builder()
                    .covered(false)
                    .reason("Valeur déclarée dépasse le plafond de couverture Hiscox (" + MAX_COVERAGE_VALUE + " EUR)")
                    .requiresManualQuote(true)
                    .build());
        }

        Map<String, Object> body = Map.of(
                "vehicleValue", vehicle.getDeclaredValue(),
                "segment", vehicle.getDeclaredSegment().name(),
                "missionId", missionId
        );

        return hiscoxWebClient.post()
                .uri("/coverage/check")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> HiscoxCoverageResult.builder()
                        .covered(Boolean.TRUE.equals(response.get("covered")))
                        .reason((String) response.get("reason"))
                        .requiresManualQuote(Boolean.TRUE.equals(response.get("requiresManualQuote")))
                        .build())
                .onErrorReturn(HiscoxCoverageResult.builder()
                        .covered(false)
                        .reason("Erreur de communication avec Hiscox")
                        .requiresManualQuote(true)
                        .build());
    }
}
