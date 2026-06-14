package com.goweyy.convoyia.biller.agent;

import com.goweyy.convoyia.biller.domain.BillingRequest;
import com.goweyy.convoyia.biller.domain.BillingResult;
import com.goweyy.convoyia.biller.service.InvoiceGeneratorService;
import com.goweyy.convoyia.biller.service.StripeConnectService;
import com.goweyy.convoyia.common.domain.enums.PricingStatus;
import com.goweyy.convoyia.common.domain.events.InspectionCompletedEvent;
import com.goweyy.convoyia.common.domain.events.MissionCompletedEvent;
import com.goweyy.convoyia.common.kafka.KafkaEventPublisher;
import com.goweyy.convoyia.common.kafka.KafkaTopicsConfig;
import com.goweyy.convoyia.pricer.domain.PricingBreakdown;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillerAgent {

    private final StripeConnectService stripeConnectService;
    private final InvoiceGeneratorService invoiceGeneratorService;
    private final KafkaEventPublisher kafkaEventPublisher;

    public Mono<BillingResult> bill(BillingRequest request) {
        log.info("Starting billing for missionId={}", request.getMissionId());

        // STEP 2: If damage detected → pause billing → manual review
        if (request.isDamageDetected()) {
            log.warn("Damage detected for missionId={} — billing paused for manual review", request.getMissionId());
            return Mono.just(BillingResult.builder()
                    .missionId(request.getMissionId())
                    .tenantId(request.getTenantId())
                    .status("PENDING_DAMAGE_REVIEW")
                    .billedAt(Instant.now())
                    .build());
        }

        if (request.getPricingResult() == null || request.getPricingResult().getStatus() != PricingStatus.PRICED) {
            return Mono.error(new IllegalStateException("Cannot bill mission without a PRICED PricingResult"));
        }

        PricingBreakdown breakdown = request.getPricingResult().getPricingBreakdown();
        BigDecimal totalTtc = breakdown.getTotalTtc();
        BigDecimal conveyorPayout = breakdown.getConveyorPayout();
        BigDecimal platformFee = breakdown.getPlatformFeeAmount();

        // STEP 3: Capture Stripe pre-auth
        return stripeConnectService.capturePreAuth(request.getPaymentIntentId(), totalTtc)
                .flatMap(chargeId -> {
                    // STEP 4: Split transfer to conveyor (75% — ALWAYS)
                    return stripeConnectService.splitTransfer(request.getConveyorStripeAccountId(), conveyorPayout)
                            .flatMap(transferId -> {
                                BillingResult partialResult = BillingResult.builder()
                                        .missionId(request.getMissionId())
                                        .tenantId(request.getTenantId())
                                        .status("BILLED")
                                        .chargeId(chargeId)
                                        .transferId(transferId)
                                        .conveyorShare(conveyorPayout)
                                        .platformShare(platformFee)
                                        .totalTtc(totalTtc)
                                        .billedAt(Instant.now())
                                        .build();

                                // STEP 5: Generate invoices
                                return Mono.zip(
                                        invoiceGeneratorService.generateClientInvoice(request, partialResult),
                                        invoiceGeneratorService.generateConveyorReceipt(request, partialResult)
                                ).flatMap(tuple -> {
                                    // In production, store PDFs in MinIO and return URL
                                    String clientUrl = "/invoices/" + request.getMissionId() + "/client.pdf";
                                    String conveyorUrl = "/invoices/" + request.getMissionId() + "/conveyor.pdf";

                                    BillingResult finalResult = BillingResult.builder()
                                            .missionId(request.getMissionId())
                                            .tenantId(request.getTenantId())
                                            .status("BILLED")
                                            .chargeId(chargeId)
                                            .transferId(transferId)
                                            .conveyorShare(conveyorPayout)
                                            .platformShare(platformFee)
                                            .totalTtc(totalTtc)
                                            .clientInvoiceUrl(clientUrl)
                                            .conveyorReceiptUrl(conveyorUrl)
                                            .billedAt(Instant.now())
                                            .build();

                                    // STEP 6: Publish MissionCompletedEvent
                                    return kafkaEventPublisher.publishEvent(
                                            MissionCompletedEvent.builder()
                                                    .missionId(request.getMissionId())
                                                    .tenantId(request.getTenantId())
                                                    .occurredAt(Instant.now())
                                                    .build(),
                                            KafkaTopicsConfig.TOPIC_MISSION_COMPLETED
                                    ).thenReturn(finalResult);
                                });
                            });
                });
    }

    @KafkaListener(topics = KafkaTopicsConfig.TOPIC_MISSION_INSPECTION_COMPLETED,
            groupId = "${spring.kafka.consumer.group-id:biller-agent}")
    public void onInspectionCompleted(InspectionCompletedEvent event) {
        // Only trigger billing for POST_MISSION phase — phase info needs to be in the event
        // In production, retrieve from DB and check phase
        log.info("Received InspectionCompletedEvent missionId={} damageDetected={}",
                event.getMissionId(), event.isDamageDetected());
    }
}
