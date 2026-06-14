package com.goweyy.convoyia.biller.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Set;

@Slf4j
@Service
public class StripeConnectService {

    /**
     * Currencies that have no decimal places (amount is passed as-is, not multiplied by 100).
     * See: https://stripe.com/docs/currencies#zero-decimal
     */
    private static final Set<String> ZERO_DECIMAL_CURRENCIES =
            Set.of("JPY", "KRW", "VND", "IDR", "BIF", "CLP", "GNF", "MGA",
                   "PYG", "RWF", "UGX", "XAF", "XOF", "XPF");

    @Value("${stripe.api-key:}")
    private String stripeApiKey;

    /**
     * Capture a Stripe pre-auth (payment intent).
     * Uses EUR as default currency — prefer the overload that accepts currency for multi-tenant setups.
     */
    public Mono<String> capturePreAuth(String paymentIntentId, BigDecimal amount) {
        return capturePreAuth(paymentIntentId, amount, "eur");
    }

    /**
     * Capture a Stripe pre-auth using the tenant's Stripe currency (lowercase ISO 4217).
     */
    public Mono<String> capturePreAuth(String paymentIntentId, BigDecimal amount, String stripeCurrency) {
        return Mono.fromCallable(() -> {
            com.stripe.Stripe.apiKey = stripeApiKey;
            long stripeAmount = toStripeAmount(amount, stripeCurrency);
            log.info("Capturing pre-auth paymentIntentId={} amount={} currency={}",
                    paymentIntentId, stripeAmount, stripeCurrency);

            var params = com.stripe.param.PaymentIntentCaptureParams.builder()
                    .setAmountToCapture(stripeAmount)
                    .build();
            var intent = com.stripe.model.PaymentIntent.retrieve(paymentIntentId);
            var captured = intent.capture(params);
            return captured.getLatestCharge();
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Transfer conveyor share via Stripe Connect.
     * Uses EUR as default currency — prefer the overload that accepts currency for multi-tenant setups.
     */
    public Mono<String> splitTransfer(String conveyorStripeAccountId, BigDecimal conveyorShare) {
        return splitTransfer(conveyorStripeAccountId, conveyorShare, "eur");
    }

    /**
     * Transfer conveyor share via Stripe Connect using the tenant's Stripe currency.
     * Platform retains its fee (already deducted from application_fee_amount at intent creation).
     */
    public Mono<String> splitTransfer(String conveyorStripeAccountId, BigDecimal conveyorShare,
                                       String stripeCurrency) {
        return Mono.fromCallable(() -> {
            com.stripe.Stripe.apiKey = stripeApiKey;
            long stripeAmount = toStripeAmount(conveyorShare, stripeCurrency);
            log.info("Transferring to conveyor account={} amount={} currency={}",
                    conveyorStripeAccountId, stripeAmount, stripeCurrency);

            var params = com.stripe.param.TransferCreateParams.builder()
                    .setAmount(stripeAmount)
                    .setCurrency(stripeCurrency.toLowerCase())
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
        return createRefund(chargeId, amount, reason, "eur");
    }

    /**
     * Create a refund on a charge using the tenant's Stripe currency.
     */
    public Mono<String> createRefund(String chargeId, BigDecimal amount, String reason,
                                      String stripeCurrency) {
        return Mono.fromCallable(() -> {
            com.stripe.Stripe.apiKey = stripeApiKey;
            long stripeAmount = toStripeAmount(amount, stripeCurrency);
            log.info("Creating refund chargeId={} amount={} currency={} reason={}",
                    chargeId, stripeAmount, stripeCurrency, reason);

            var params = com.stripe.param.RefundCreateParams.builder()
                    .setCharge(chargeId)
                    .setAmount(stripeAmount)
                    .setReason(com.stripe.param.RefundCreateParams.Reason.FRAUDULENT)
                    .build();
            var refund = com.stripe.model.Refund.create(params);
            return refund.getId();
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Convert a BigDecimal amount to the smallest currency unit for Stripe.
     * Most currencies: multiply by 100 (cents).
     * Zero-decimal currencies (JPY, KRW, etc.): pass as-is.
     */
    long toStripeAmount(BigDecimal amount, String currency) {
        if (ZERO_DECIMAL_CURRENCIES.contains(currency.toUpperCase())) {
            return amount.setScale(0, RoundingMode.HALF_UP).longValue();
        }
        return amount.multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValue();
    }
}
