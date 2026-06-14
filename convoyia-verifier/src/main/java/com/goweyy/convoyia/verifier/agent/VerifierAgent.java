package com.goweyy.convoyia.verifier.agent;

import com.goweyy.convoyia.common.domain.enums.LlmModel;
import com.goweyy.convoyia.common.domain.enums.ResponseFormat;
import com.goweyy.convoyia.common.domain.enums.VerificationStatus;
import com.goweyy.convoyia.common.domain.events.VerificationCompletedEvent;
import com.goweyy.convoyia.common.domain.records.LlmRequest;
import com.goweyy.convoyia.common.domain.records.VerificationRequest;
import com.goweyy.convoyia.common.domain.records.VerificationResult;
import com.goweyy.convoyia.common.kafka.KafkaEventPublisher;
import com.goweyy.convoyia.common.kafka.KafkaTopicsConfig;
import com.goweyy.convoyia.common.llm.LlmGateway;
import com.goweyy.convoyia.verifier.llm.VerifierPromptBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerifierAgent {

    private final VehicleVerificationService vehicleService;
    private final ConveyorVerificationService conveyorService;
    private final MissionVerificationService missionService;
    private final LlmGateway llmGateway;
    private final VerifierPromptBuilder promptBuilder;
    private final KafkaEventPublisher kafkaEventPublisher;

    public Mono<VerificationResult> verify(VerificationRequest request) {
        log.info("Starting verification for missionId={} tenantId={}", request.getMissionId(), request.getTenantId());

        return Mono.zip(
                vehicleService.verify(request),
                conveyorService.verify(request),
                missionService.verify(request)
        ).flatMap(tuple -> {
            var vehicleBlock = tuple.getT1();
            var conveyorBlock = tuple.getT2();
            var missionBlock = tuple.getT3();

            VerificationStatus globalStatus = worstStatus(
                    vehicleBlock.getStatus(),
                    conveyorBlock.getStatus(),
                    missionBlock.getStatus()
            );

            List<String> blockingReasons = new ArrayList<>();
            blockingReasons.addAll(vehicleBlock.getFailed());
            blockingReasons.addAll(conveyorBlock.getFailed());
            blockingReasons.addAll(missionBlock.getFailed());

            List<com.goweyy.convoyia.common.domain.records.VerificationAlert> allAlerts = new ArrayList<>();
            allAlerts.addAll(vehicleBlock.getAlerts());
            allAlerts.addAll(conveyorBlock.getAlerts());
            allAlerts.addAll(missionBlock.getAlerts());

            boolean hiscoxConfirmed = missionBlock.getPassed() != null
                    && missionBlock.getPassed().contains("HISCOX_COVERAGE");

            Mono<VerificationResult> resultMono;
            if (globalStatus == VerificationStatus.ESCALATED) {
                String escalationPrompt = promptBuilder.buildEscalationPrompt(
                        request, vehicleBlock, conveyorBlock, missionBlock);
                LlmRequest llmRequest = LlmRequest.builder()
                        .model(LlmModel.MISTRAL_7B)
                        .prompt(escalationPrompt)
                        .expectedFormat(ResponseFormat.JSON)
                        .build();
                resultMono = llmGateway.invoke(llmRequest)
                        .map(reasoning -> buildResult(request, globalStatus, vehicleBlock, conveyorBlock,
                                missionBlock, blockingReasons, allAlerts, hiscoxConfirmed, "LLM: " + reasoning));
            } else {
                resultMono = Mono.just(buildResult(request, globalStatus, vehicleBlock, conveyorBlock,
                        missionBlock, blockingReasons, allAlerts, hiscoxConfirmed, "verifier-agent"));
            }

            return resultMono.flatMap(result -> kafkaEventPublisher.publishEvent(
                    VerificationCompletedEvent.builder()
                            .missionId(request.getMissionId())
                            .tenantId(request.getTenantId())
                            .status(globalStatus)
                            .occurredAt(Instant.now())
                            .build(),
                    KafkaTopicsConfig.TOPIC_MISSION_VERIFICATION_COMPLETED
            ).thenReturn(result));
        });
    }

    @KafkaListener(topics = KafkaTopicsConfig.TOPIC_MISSION_DISPATCH_COMPLETED,
            groupId = "${spring.kafka.consumer.group-id:verifier-agent}")
    public void onMissionDispatched(com.goweyy.convoyia.common.domain.events.MissionDispatchedEvent event) {
        log.info("Received MissionDispatchedEvent for missionId={}", event.getMissionId());
        // Trigger verification automatically - in a real scenario we'd retrieve full request from DB
    }

    private VerificationResult buildResult(VerificationRequest request, VerificationStatus globalStatus,
                                            com.goweyy.convoyia.common.domain.records.VerificationBlock vehicleBlock,
                                            com.goweyy.convoyia.common.domain.records.VerificationBlock conveyorBlock,
                                            com.goweyy.convoyia.common.domain.records.VerificationBlock missionBlock,
                                            List<String> blockingReasons,
                                            List<com.goweyy.convoyia.common.domain.records.VerificationAlert> alerts,
                                            boolean hiscoxConfirmed, String verifiedBy) {
        return VerificationResult.builder()
                .missionId(request.getMissionId())
                .tenantId(request.getTenantId())
                .globalStatus(globalStatus)
                .vehicleBlock(vehicleBlock)
                .conveyorBlock(conveyorBlock)
                .missionBlock(missionBlock)
                .blockingReasons(blockingReasons)
                .alerts(alerts)
                .hiscoxCoverageConfirmed(hiscoxConfirmed)
                .verifiedAt(Instant.now())
                .verifiedBy(verifiedBy)
                .build();
    }

    private VerificationStatus worstStatus(VerificationStatus... statuses) {
        if (Stream.of(statuses).anyMatch(s -> s == VerificationStatus.BLOCKED)) return VerificationStatus.BLOCKED;
        if (Stream.of(statuses).anyMatch(s -> s == VerificationStatus.ESCALATED)) return VerificationStatus.ESCALATED;
        if (Stream.of(statuses).anyMatch(s -> s == VerificationStatus.PARTIAL)) return VerificationStatus.PARTIAL;
        return VerificationStatus.VERIFIED;
    }
}
