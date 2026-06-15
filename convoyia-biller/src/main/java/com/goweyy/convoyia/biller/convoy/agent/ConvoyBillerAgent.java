package com.goweyy.convoyia.biller.convoy.agent;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.goweyy.convoyia.biller.convoy.dto.ConvoyBillingRequest;
import com.goweyy.convoyia.biller.convoy.dto.ConvoyBillingResult;
import com.goweyy.convoyia.common.domain.enums.ConvoyBillingStatus;
import com.goweyy.convoyia.common.domain.enums.ConvoyInspectionPhase;
import com.goweyy.convoyia.common.domain.enums.ConvoyMarket;
import com.goweyy.convoyia.common.domain.enums.ConvoyMissionState;
import com.goweyy.convoyia.common.domain.enums.ConvoyPricingStatus;
import com.goweyy.convoyia.common.kafka.ConvoyKafkaEventPublisher;
import com.goweyy.convoyia.common.kafka.ConvoyKafkaTopicsConfig;
import com.goweyy.convoyia.common.kafka.events.ConvoyInspectionCompletedEvent;
import com.goweyy.convoyia.common.kafka.events.ConvoyMissionCompletedEvent;
import com.goweyy.convoyia.common.repository.ConvoyMissionContext;
import com.goweyy.convoyia.common.repository.ConvoyMissionContextRepository;
import com.goweyy.convoyia.pricer.convoy.dto.ConvoyPricingBreakdown;
import com.goweyy.convoyia.pricer.convoy.dto.ConvoyPricingResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * ConvoyBillerAgent — blocking billing agent for ConvoyIA.
 *
 * Stripe pre-auth totalTtc × 1.20 is captured here (rule 6).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConvoyBillerAgent {

    private static final BigDecimal STRIPE_PRE_AUTH_MULTIPLIER = new BigDecimal("1.20");
    private static final BigDecimal CONVEYOR_SHARE_RATIO = new BigDecimal("0.75");
    private static final BigDecimal PLATFORM_SHARE_RATIO = new BigDecimal("0.25");

    private final ConvoyStripeConnectService stripeConnectService;
    private final ConvoyInvoiceGeneratorService invoiceGeneratorService;
    private final ConvoyMissionContextRepository missionContextRepository;
    private final ConvoyKafkaEventPublisher kafkaEventPublisher;
    private final ObjectMapper objectMapper;

    public ConvoyBillingResult bill(ConvoyBillingRequest request) {
        log.info("ConvoyBillerAgent billing missionId={}", request.getMissionId());

        if (request.isDamageDetected()) {
            log.warn("Damage detected missionId={} — billing paused", request.getMissionId());
            return ConvoyBillingResult.builder()
                    .missionId(request.getMissionId())
                    .tenantId(request.getTenantId())
                    .status(ConvoyBillingStatus.PENDING_DAMAGE_REVIEW)
                    .billedAt(Instant.now())
                    .build();
        }

        if (request.getPricingResult() == null
                || request.getPricingResult().getStatus() != ConvoyPricingStatus.PRICED) {
            throw new IllegalStateException("Cannot bill mission without a PRICED ConvoyPricingResult");
        }

        ConvoyPricingBreakdown breakdown = request.getPricingResult().getPricingBreakdown();
        BigDecimal totalTtc = breakdown.getTotalTtc();
        BigDecimal conveyorPayout = breakdown.getConveyorPayout();
        BigDecimal platformFee = breakdown.getPlatformFeeAmount();
        BigDecimal stripePreAuth = totalTtc.multiply(STRIPE_PRE_AUTH_MULTIPLIER)
                .setScale(2, RoundingMode.CEILING);

        String currency = breakdown.getCurrencyCode() != null
                ? breakdown.getCurrencyCode().toLowerCase()
                : ConvoyMarket.FRANCE.getCurrencyCode().toLowerCase();

        String chargeId = stripeConnectService.capturePreAuth(
                request.getPaymentIntentId(), stripePreAuth, currency);
        String transferId = stripeConnectService.splitTransfer(
                request.getConveyorStripeAccountId(), conveyorPayout, currency);

        String clientInvoiceUrl = invoiceGeneratorService.generateClientInvoice(request);
        String conveyorReceiptUrl = invoiceGeneratorService.generateConveyorReceipt(request);

        log.info("ConvoyBillerAgent missionId={} billed totalTtc={} chargeId={}",
                request.getMissionId(), totalTtc, chargeId);

        return ConvoyBillingResult.builder()
                .missionId(request.getMissionId())
                .tenantId(request.getTenantId())
                .status(ConvoyBillingStatus.BILLED)
                .chargeId(chargeId)
                .transferId(transferId)
                .conveyorShare(conveyorPayout)
                .platformShare(platformFee)
                .totalTtc(totalTtc)
                .clientInvoiceUrl(clientInvoiceUrl)
                .conveyorReceiptUrl(conveyorReceiptUrl)
                .billedAt(Instant.now())
                .build();
    }

    @KafkaListener(topics = ConvoyKafkaTopicsConfig.TOPIC_CONVOY_INSPECTION_COMPLETED,
            groupId = "${spring.kafka.consumer.group-id:convoy-biller}")
    public void onInspectionCompleted(ConvoyInspectionCompletedEvent event) {
        if (event.getPhase() != ConvoyInspectionPhase.POST_MISSION) {
            return;
        }
        missionContextRepository.findByMissionId(UUID.fromString(event.getMissionId())).ifPresentOrElse(context -> {
            if (event.isDamageDetected()) {
                log.warn("Damage detected for missionId={} — escalating to human review", event.getMissionId());
                context.setCurrentState(ConvoyMissionState.ESCALATED_HUMAN);
                missionContextRepository.save(context);
                return;
            }
            ConvoyPricingResult pricingResult = buildPricingResult(context);
            ConvoyBillingResult billingResult = bill(ConvoyBillingRequest.builder()
                    .missionId(event.getMissionId())
                    .tenantId(event.getTenantId())
                    .paymentIntentId(valueAsString(parseEnrichedData(context.getEnrichedData()), "paymentIntentId"))
                    .conveyorStripeAccountId(valueAsString(parseEnrichedData(context.getEnrichedData()), "conveyorStripeAccountId"))
                    .pricingResult(pricingResult)
                    .damageDetected(false)
                    .build());
            kafkaEventPublisher.publishEvent(ConvoyMissionCompletedEvent.builder()
                            .missionId(billingResult.getMissionId())
                            .tenantId(billingResult.getTenantId())
                            .totalTtc(billingResult.getTotalTtc())
                            .currencyCode(pricingResult.getPricingBreakdown().getCurrencyCode())
                            .clientInvoiceUrl(billingResult.getClientInvoiceUrl())
                            .conveyorReceiptUrl(billingResult.getConveyorReceiptUrl())
                            .occurredAt(Instant.now())
                            .build(),
                    ConvoyKafkaTopicsConfig.TOPIC_CONVOY_MISSION_COMPLETED);
            context.setCurrentState(ConvoyMissionState.COMPLETED);
            missionContextRepository.save(context);
        }, () -> log.warn("Mission context not found for inspection event missionId={}", event.getMissionId()));
    }

    private ConvoyPricingResult buildPricingResult(ConvoyMissionContext context) {
        Map<String, Object> enriched = parseEnrichedData(context.getEnrichedData());
        BigDecimal totalTtc = valueAsBigDecimal(enriched.get("pricingTotalTtc"));
        BigDecimal totalHt = valueAsBigDecimal(enriched.get("pricingTotalHt"));
        if (totalTtc.compareTo(BigDecimal.ZERO) == 0 && totalHt.compareTo(BigDecimal.ZERO) == 0) {
            totalHt = BigDecimal.ZERO;
            totalTtc = BigDecimal.ZERO;
        }
        String currencyCode = valueAsString(enriched, "currencyCode");
        if (currencyCode == null || currencyCode.isBlank()) {
            currencyCode = ConvoyMarket.FRANCE.getCurrencyCode();
        }
        ConvoyPricingBreakdown breakdown = ConvoyPricingBreakdown.builder()
                .transportCost(valueAsBigDecimal(enriched.get("transportCost")))
                .segmentSurcharge(valueAsBigDecimal(enriched.get("segmentSurcharge")))
                .vehicleInsuranceCost(BigDecimal.ZERO)
                .rcProPlatformCost(BigDecimal.ZERO)
                .subtotalBeforeBonus(valueAsBigDecimal(enriched.get("subtotalBeforeBonus")))
                .urgencyBonusAmount(valueAsBigDecimal(enriched.get("urgencyBonusAmount")))
                .weekendBonusAmount(valueAsBigDecimal(enriched.get("weekendBonusAmount")))
                .nightBonusAmount(valueAsBigDecimal(enriched.get("nightBonusAmount")))
                .totalHt(totalHt)
                .vatAmount(valueAsBigDecimal(enriched.get("vatAmount")))
                .totalTtc(totalTtc)
                .conveyorPayout(totalTtc.multiply(CONVEYOR_SHARE_RATIO).setScale(2, RoundingMode.HALF_UP))
                .platformFeeAmount(totalTtc.multiply(PLATFORM_SHARE_RATIO).setScale(2, RoundingMode.HALF_UP))
                .minimumFareApplied(Boolean.parseBoolean(String.valueOf(enriched.getOrDefault("minimumFareApplied", false))))
                .currencyCode(currencyCode)
                .currencySymbol(valueAsString(enriched, "currencySymbol"))
                .taxName(valueAsString(enriched, "taxName"))
                .build();
        return ConvoyPricingResult.builder()
                .missionId(context.getMissionId().toString())
                .tenantId(context.getTenantId())
                .status(ConvoyPricingStatus.PRICED)
                .pricingBreakdown(breakdown)
                .pricedAt(Instant.now())
                .build();
    }

    private Map<String, Object> parseEnrichedData(String enrichedData) {
        if (enrichedData == null || enrichedData.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(enrichedData, new TypeReference<>() {});
        } catch (Exception exception) {
            log.warn("Failed to parse mission enrichedData for billing: {}", exception.getMessage());
            return Map.of();
        }
    }

    private String valueAsString(Map<String, Object> source, String key) {
        Object value = source.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private BigDecimal valueAsBigDecimal(Object value) {
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue()).setScale(2, RoundingMode.HALF_UP);
        }
        if (value instanceof String text && !text.isBlank()) {
            return new BigDecimal(text).setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }
}
