package com.goweyy.convoyia.pricer.agent;

import com.goweyy.convoyia.common.domain.enums.VehicleSegment;
import com.goweyy.convoyia.common.domain.enums.VerificationStatus;
import com.goweyy.convoyia.common.domain.events.PricingCompletedEvent;
import com.goweyy.convoyia.common.domain.events.VerificationCompletedEvent;
import com.goweyy.convoyia.common.kafka.KafkaEventPublisher;
import com.goweyy.convoyia.common.kafka.KafkaTopicsConfig;
import com.goweyy.convoyia.pricer.domain.PricingRequest;
import com.goweyy.convoyia.pricer.domain.PricingResult;
import com.goweyy.convoyia.pricer.service.PricingCalculationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class PricerAgent {

    private final PricingCalculationService calculationService;
    private final KafkaEventPublisher kafkaEventPublisher;

    public Mono<PricingResult> price(PricingRequest request) {
        log.info("Pricing missionId={} segment={}", request.getMissionId(), request.getVehicleSegment());

        if (request.getVehicleSegment() == VehicleSegment.LUXE_PLATEAU) {
            log.info("LUXE_PLATEAU segment - returning PENDING_MANUAL_QUOTE for missionId={}", request.getMissionId());
            PricingResult result = PricingResult.builder()
                    .missionId(request.getMissionId())
                    .tenantId(request.getTenantId())
                    .status("PENDING_MANUAL_QUOTE")
                    .currency("EUR")
                    .pricedAt(Instant.now())
                    .build();
            return Mono.just(result);
        }

        return calculationService.calculate(request)
                .flatMap(result -> kafkaEventPublisher.publishEvent(
                        PricingCompletedEvent.builder()
                                .missionId(result.getMissionId())
                                .tenantId(result.getTenantId())
                                .totalTtc(result.getTotalTtc())
                                .conveyorShare(result.getConveyorShare())
                                .platformShare(result.getPlatformShare())
                                .occurredAt(Instant.now())
                                .build(),
                        KafkaTopicsConfig.TOPIC_MISSION_PRICING_COMPLETED
                ).thenReturn(result));
    }

    @KafkaListener(topics = KafkaTopicsConfig.TOPIC_MISSION_VERIFICATION_COMPLETED,
            groupId = "${spring.kafka.consumer.group-id:pricer-agent}")
    public void onVerificationCompleted(VerificationCompletedEvent event) {
        if (event.getStatus() == VerificationStatus.VERIFIED || event.getStatus() == VerificationStatus.PARTIAL) {
            log.info("Received verified mission for pricing: missionId={}", event.getMissionId());
            // In a real scenario, retrieve full mission data from DB and trigger pricing
        }
    }
}
