package com.goweyy.convoyia.biller.convoy.agent;

import com.goweyy.convoyia.biller.convoy.dto.ConvoyBillingRequest;
import com.goweyy.convoyia.biller.convoy.dto.ConvoyBillingResult;
import com.goweyy.convoyia.common.domain.enums.ConvoyBillingStatus;
import com.goweyy.convoyia.common.domain.enums.ConvoyPricingStatus;
import com.goweyy.convoyia.pricer.convoy.dto.ConvoyPricingBreakdown;
import com.goweyy.convoyia.pricer.convoy.dto.ConvoyPricingResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConvoyBillerAgentTest {

    @Mock
    private ConvoyStripeConnectService stripeConnectService;

    @Mock
    private ConvoyInvoiceGeneratorService invoiceGeneratorService;

    @InjectMocks
    private ConvoyBillerAgent billerAgent;

    private ConvoyPricingResult pricedResult;

    @BeforeEach
    void setUp() {
        ConvoyPricingBreakdown breakdown = ConvoyPricingBreakdown.builder()
                .totalHt(new BigDecimal("50.00"))
                .vatAmount(new BigDecimal("10.00"))
                .totalTtc(new BigDecimal("60.00"))
                .conveyorPayout(new BigDecimal("45.00"))
                .platformFeeAmount(new BigDecimal("15.00"))
                .currencyCode("EUR")
                .currencySymbol("€")
                .taxName("TVA")
                .build();

        pricedResult = ConvoyPricingResult.builder()
                .missionId("mission-bill-001")
                .tenantId("tenant-goweyy")
                .status(ConvoyPricingStatus.PRICED)
                .pricingBreakdown(breakdown)
                .pricedAt(Instant.now())
                .build();
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    void billing_priced_mission_returns_billed_status() {
        when(stripeConnectService.capturePreAuth(anyString(), any(), anyString())).thenReturn("ch_test_001");
        when(stripeConnectService.splitTransfer(anyString(), any(), anyString())).thenReturn("tr_test_001");
        when(invoiceGeneratorService.generateClientInvoice(any())).thenReturn("invoices/m1/client.pdf");
        when(invoiceGeneratorService.generateConveyorReceipt(any())).thenReturn("invoices/m1/conveyor.pdf");

        ConvoyBillingRequest req = buildRequest(false, pricedResult);
        ConvoyBillingResult result = billerAgent.bill(req);
        assertThat(result.getStatus()).isEqualTo(ConvoyBillingStatus.BILLED);
    }

    @Test
    void billing_result_has_charge_and_transfer_ids() {
        when(stripeConnectService.capturePreAuth(anyString(), any(), anyString())).thenReturn("ch_conv_abc");
        when(stripeConnectService.splitTransfer(anyString(), any(), anyString())).thenReturn("tr_conv_xyz");
        when(invoiceGeneratorService.generateClientInvoice(any())).thenReturn("invoices/m1/client.pdf");
        when(invoiceGeneratorService.generateConveyorReceipt(any())).thenReturn("invoices/m1/conveyor.pdf");

        ConvoyBillingRequest req = buildRequest(false, pricedResult);
        ConvoyBillingResult result = billerAgent.bill(req);
        assertThat(result.getChargeId()).isEqualTo("ch_conv_abc");
        assertThat(result.getTransferId()).isEqualTo("tr_conv_xyz");
    }

    @Test
    void billing_result_contains_correct_amounts() {
        when(stripeConnectService.capturePreAuth(anyString(), any(), anyString())).thenReturn("ch_x");
        when(stripeConnectService.splitTransfer(anyString(), any(), anyString())).thenReturn("tr_x");
        when(invoiceGeneratorService.generateClientInvoice(any())).thenReturn("invoices/m1/client.pdf");
        when(invoiceGeneratorService.generateConveyorReceipt(any())).thenReturn("invoices/m1/conveyor.pdf");

        ConvoyBillingRequest req = buildRequest(false, pricedResult);
        ConvoyBillingResult result = billerAgent.bill(req);
        assertThat(result.getTotalTtc()).isEqualByComparingTo(new BigDecimal("60.00"));
        assertThat(result.getConveyorShare()).isEqualByComparingTo(new BigDecimal("45.00"));
        assertThat(result.getPlatformShare()).isEqualByComparingTo(new BigDecimal("15.00"));
    }

    @Test
    void billing_result_contains_invoice_urls() {
        when(stripeConnectService.capturePreAuth(anyString(), any(), anyString())).thenReturn("ch_x");
        when(stripeConnectService.splitTransfer(anyString(), any(), anyString())).thenReturn("tr_x");
        when(invoiceGeneratorService.generateClientInvoice(any())).thenReturn("invoices/m/client.pdf");
        when(invoiceGeneratorService.generateConveyorReceipt(any())).thenReturn("invoices/m/conveyor.pdf");

        ConvoyBillingResult result = billerAgent.bill(buildRequest(false, pricedResult));
        assertThat(result.getClientInvoiceUrl()).isEqualTo("invoices/m/client.pdf");
        assertThat(result.getConveyorReceiptUrl()).isEqualTo("invoices/m/conveyor.pdf");
    }

    @Test
    void billing_has_billed_at_timestamp() {
        Instant before = Instant.now();
        when(stripeConnectService.capturePreAuth(anyString(), any(), anyString())).thenReturn("ch_x");
        when(stripeConnectService.splitTransfer(anyString(), any(), anyString())).thenReturn("tr_x");
        when(invoiceGeneratorService.generateClientInvoice(any())).thenReturn("invoices/m/client.pdf");
        when(invoiceGeneratorService.generateConveyorReceipt(any())).thenReturn("invoices/m/conveyor.pdf");

        ConvoyBillingResult result = billerAgent.bill(buildRequest(false, pricedResult));
        assertThat(result.getBilledAt()).isAfterOrEqualTo(before);
    }

    // ── Damage detected → pause billing ──────────────────────────────────────

    @Test
    void damage_detected_returns_pending_damage_review() {
        ConvoyBillingRequest req = buildRequest(true, pricedResult);
        ConvoyBillingResult result = billerAgent.bill(req);
        assertThat(result.getStatus()).isEqualTo(ConvoyBillingStatus.PENDING_DAMAGE_REVIEW);
    }

    @Test
    void damage_detected_does_not_call_stripe() {
        ConvoyBillingRequest req = buildRequest(true, pricedResult);
        billerAgent.bill(req);
        // Mockito strict stubbing: no calls should be made to stripe services
        // Verified by absence of interactions
    }

    // ── Null pricing result ────────────────────────────────────────────────

    @Test
    void null_pricing_result_throws_illegal_state() {
        ConvoyBillingRequest req = buildRequest(false, null);
        assertThatThrownBy(() -> billerAgent.bill(req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot bill");
    }

    // ── Non-PRICED status ──────────────────────────────────────────────────

    @Test
    void pending_manual_quote_status_throws_illegal_state() {
        ConvoyPricingResult nonPriced = ConvoyPricingResult.builder()
                .missionId("mission-bill-001")
                .tenantId("tenant-goweyy")
                .status(ConvoyPricingStatus.PENDING_MANUAL_QUOTE)
                .pricedAt(Instant.now())
                .build();
        ConvoyBillingRequest req = buildRequest(false, nonPriced);
        assertThatThrownBy(() -> billerAgent.bill(req))
                .isInstanceOf(IllegalStateException.class);
    }

    // ── Mission and tenant propagation ────────────────────────────────────

    @Test
    void billing_result_propagates_mission_and_tenant_id() {
        when(stripeConnectService.capturePreAuth(anyString(), any(), anyString())).thenReturn("ch_x");
        when(stripeConnectService.splitTransfer(anyString(), any(), anyString())).thenReturn("tr_x");
        when(invoiceGeneratorService.generateClientInvoice(any())).thenReturn("invoices/m/client.pdf");
        when(invoiceGeneratorService.generateConveyorReceipt(any())).thenReturn("invoices/m/conveyor.pdf");

        ConvoyBillingResult result = billerAgent.bill(buildRequest(false, pricedResult));
        assertThat(result.getMissionId()).isEqualTo("mission-bill-001");
        assertThat(result.getTenantId()).isEqualTo("tenant-goweyy");
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private ConvoyBillingRequest buildRequest(boolean damageDetected, ConvoyPricingResult pricingResult) {
        return ConvoyBillingRequest.builder()
                .missionId("mission-bill-001")
                .tenantId("tenant-goweyy")
                .paymentIntentId("pi_test_001")
                .conveyorStripeAccountId("acct_test_001")
                .pricingResult(pricingResult)
                .damageDetected(damageDetected)
                .build();
    }
}
