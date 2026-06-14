package com.goweyy.convoyia.biller.agent;

import com.goweyy.convoyia.biller.domain.BillingRequest;
import com.goweyy.convoyia.biller.domain.BillingResult;
import com.goweyy.convoyia.biller.service.DocumentStorageService;
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
    private final DocumentStorageService documentStorageService;
    private final KafkaEventPublisher kafkaEventPublisher;

    public Mono<BillingResult> bill(BillingRequest request) {
        log.info("Starting billing for missionId={}", request.getMissionId());

        // STEP 1: If damage detected → pause billing → manual review required
        if (request.isDamageDetected()) {
            log.warn("Damage detected for missionId={} — billing paused for manual review",
                    request.getMissionId());
            return Mono.just(BillingResult.builder()
                    .missionId(request.getMissionId())
                    .tenantId(request.getTenantId())
                    .status("PENDING_DAMAGE_REVIEW")
                    .billedAt(Instant.now())
                    .build());
        }

        if (request.getPricingResult() == null
                || request.getPricingResult().getStatus() != PricingStatus.PRICED) {
            return Mono.error(new IllegalStateException(
                    "Cannot bill mission without a PRICED PricingResult"));
        }

        PricingBreakdown breakdown = request.getPricingResult().getPricingBreakdown();
        BigDecimal totalTtc = breakdown.getTotalTtc();
        BigDecimal conveyorPayout = breakdown.getConveyorPayout();
        BigDecimal platformFee = breakdown.getPlatformFeeAmount();
        // Use tenant's stripe currency from breakdown (falls back to "eur" if not set)
        String stripeCurrency = breakdown.getCurrencyCode() != null
                ? breakdown.getCurrencyCode().toLowerCase()
                : "eur";

        // STEP 2: Capture Stripe pre-auth (was authorized at totalTtc × 1.20)
        return stripeConnectService.capturePreAuth(request.getPaymentIntentId(), totalTtc, stripeCurrency)
                .flatMap(chargeId ->
                        // STEP 3: Split transfer — conveyor share (ALWAYS)
                        stripeConnectService.splitTransfer(
                                request.getConveyorStripeAccountId(), conveyorPayout, stripeCurrency)
                                .flatMap(transferId -> buildBilledResult(
                                        request, chargeId, transferId,
                                        totalTtc, conveyorPayout, platformFee)));
    }

    private Mono<BillingResult> buildBilledResult(
            BillingRequest request,
            String chargeId,
            String transferId,
            BigDecimal totalTtc,
            BigDecimal conveyorPayout,
            BigDecimal platformFee) {

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

        // STEP 4: Generate invoices
        return Mono.zip(
                invoiceGeneratorService.generateClientInvoice(request, partialResult),
                invoiceGeneratorService.generateConveyorReceipt(request, partialResult)
        ).flatMap(pdfTuple -> {
            String clientKey = "invoices/" + request.getMissionId() + "/client.pdf";
            String conveyorKey = "invoices/" + request.getMissionId() + "/conveyor.pdf";
            // STEP 5: Upload PDFs to MinIO
            return Mono.zip(
                    documentStorageService.store(clientKey, pdfTuple.getT1()),
                    documentStorageService.store(conveyorKey, pdfTuple.getT2())
            ).flatMap(urls -> {
                BillingResult finalResult = BillingResult.builder()
                        .missionId(request.getMissionId())
                        .tenantId(request.getTenantId())
                        .status("BILLED")
                        .chargeId(chargeId)
                        .transferId(transferId)
                        .conveyorShare(conveyorPayout)
                        .platformShare(platformFee)
                        .totalTtc(totalTtc)
                        .clientInvoiceUrl(urls.getT1())
                        .conveyorReceiptUrl(urls.getT2())
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
    }

    @KafkaListener(topics = KafkaTopicsConfig.TOPIC_MISSION_INSPECTION_COMPLETED,
            groupId = "${spring.kafka.consumer.group-id:biller-agent}")
    public void onInspectionCompleted(InspectionCompletedEvent event) {
        // Trigger billing for POST_MISSION phase (retrieve from DB to check phase in production)
        log.info("Received InspectionCompletedEvent missionId={} damageDetected={}",
                event.getMissionId(), event.isDamageDetected());
    }
}
