package com.goweyy.convoyia.biller.convoy.agent;

import com.goweyy.convoyia.common.domain.enums.ConvoyBillingStatus;
import com.goweyy.convoyia.common.domain.enums.ConvoyPricingStatus;
import com.goweyy.convoyia.biller.convoy.dto.ConvoyBillingRequest;
import com.goweyy.convoyia.biller.convoy.dto.ConvoyBillingResult;
import com.goweyy.convoyia.pricer.convoy.dto.ConvoyPricingBreakdown;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

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

    private final ConvoyStripeConnectService stripeConnectService;
    private final ConvoyInvoiceGeneratorService invoiceGeneratorService;

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
        // Stripe pre-auth = totalTtc × 1.20 — captured here (rule 6)
        BigDecimal stripePreAuth = totalTtc.multiply(STRIPE_PRE_AUTH_MULTIPLIER)
                .setScale(2, RoundingMode.CEILING);

        String currency = breakdown.getCurrencyCode() != null
                ? breakdown.getCurrencyCode().toLowerCase()
                : "eur";

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
}
