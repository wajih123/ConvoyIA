package com.goweyy.convoyia.biller.convoy.agent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * ConvoyStripeConnectService — blocking Stripe Connect operations for ConvoyIA.
 * Uses WebClient only for external Stripe HTTP calls.
 */
@Slf4j
@Service
public class ConvoyStripeConnectService {

    public String capturePreAuth(String paymentIntentId, BigDecimal amount, String currency) {
        log.info("ConvoyStripeConnectService capturePreAuth intentId={} amount={} currency={}",
                paymentIntentId, amount, currency);
        // TODO: integrate with Stripe Java SDK
        return "ch_convoy_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    public String splitTransfer(String destinationAccountId, BigDecimal amount, String currency) {
        log.info("ConvoyStripeConnectService splitTransfer dest={} amount={} currency={}",
                destinationAccountId, amount, currency);
        // TODO: integrate with Stripe Connect Transfer API
        return "tr_convoy_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    public String cancelPreAuth(String paymentIntentId) {
        log.info("ConvoyStripeConnectService cancelPreAuth intentId={}", paymentIntentId);
        // TODO: PaymentIntent.cancel() via Stripe Java SDK
        return "cancelled_" + paymentIntentId;
    }

    public String createRefund(String paymentIntentId, BigDecimal amount, String currency, String reason) {
        log.info("ConvoyStripeConnectService createRefund intentId={} amount={} currency={} reason={}",
                paymentIntentId, amount, currency, reason);
        // TODO: Refund.create() via Stripe Java SDK
        return "re_convoy_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
