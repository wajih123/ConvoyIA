package com.goweyy.convoyia.biller.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Service
public class StripeConnectService {

    @Value("${stripe.api-key:}")
    private String stripeApiKey;

    /**
     * Capture a Stripe pre-auth (payment intent).
     * All amounts in EUR cents (amount × 100, as Long).
     */
    public Mono<String> capturePreAuth(String paymentIntentId, BigDecimal amount) {
        return Mono.fromCallable(() -> {
            com.stripe.Stripe.apiKey = stripeApiKey;
            long amountCents = amount.multiply(BigDecimal.valueOf(100))
                    .setScale(0, RoundingMode.HALF_UP).longValue();
            log.info("Capturing pre-auth paymentIntentId={} amount={}c", paymentIntentId, amountCents);

            var params = com.stripe.param.PaymentIntentCaptureParams.builder()
                    .setAmountToCapture(amountCents)
                    .build();
            var intent = com.stripe.model.PaymentIntent.retrieve(paymentIntentId);
            var captured = intent.capture(params);
            return captured.getLatestCharge();
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Transfer conveyor share via Stripe Connect.
     * Platform retains the 25% (already deducted from application_fee_amount at intent creation).
     */
    public Mono<String> splitTransfer(String conveyorStripeAccountId, BigDecimal conveyorShare) {
        return Mono.fromCallable(() -> {
            com.stripe.Stripe.apiKey = stripeApiKey;
            long amountCents = conveyorShare.multiply(BigDecimal.valueOf(100))
                    .setScale(0, RoundingMode.HALF_UP).longValue();
            log.info("Transferring to conveyor account={} amount={}c", conveyorStripeAccountId, amountCents);

            var params = com.stripe.param.TransferCreateParams.builder()
                    .setAmount(amountCents)
                    .setCurrency("eur")
                    .setDestination(conveyorStripeAccountId)
                    .build();
            var transfer = com.stripe.model.Transfer.create(params);
            return transfer.getId();
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Create a refund on a charge (for damage disputes).
     */
    public Mono<String> createRefund(String chargeId, BigDecimal amount, String reason) {
        return Mono.fromCallable(() -> {
            com.stripe.Stripe.apiKey = stripeApiKey;
            long amountCents = amount.multiply(BigDecimal.valueOf(100))
                    .setScale(0, RoundingMode.HALF_UP).longValue();
            log.info("Creating refund chargeId={} amount={}c reason={}", chargeId, amountCents, reason);

            var params = com.stripe.param.RefundCreateParams.builder()
                    .setCharge(chargeId)
                    .setAmount(amountCents)
                    .setReason(com.stripe.param.RefundCreateParams.Reason.FRAUDULENT)
                    .build();
            var refund = com.stripe.model.Refund.create(params);
            return refund.getId();
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
