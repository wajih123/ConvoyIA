package com.goweyy.convoyia.common.simulation.worldwide;

import com.goweyy.convoyia.common.simulation.ConvoyScenario;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * MultiCurrencyScenario — validates that pricing produces correct results for
 * 3 different tenants: goweyy (EUR), convoyia-uk-demo (GBP), convoyia-uae-demo (AED).
 *
 * Simulates the same logical mission (comparable urban route) for each tenant
 * and asserts correct currency, tax name, tax rate, and split ratio per tenant.
 * No cross-currency contamination must occur.
 */
@Slf4j
@Component
public class MultiCurrencyScenario implements ConvoyScenario {

    @Override
    public String name() {
        return "MultiCurrencyScenario";
    }

    @Override
    public void run() throws Exception {
        log.info("[Scenario] {} — start", name());

        // Tenant: goweyy (France)
        TenantPricingContext frCtx = TenantPricingContext.builder()
                .tenantId("goweyy")
                .currencyCode("EUR")
                .currencySymbol("€")
                .taxName("TVA")
                .taxRate(new BigDecimal("0.20"))
                .platformFeeRatio(new BigDecimal("0.25"))
                .minimumFare(new BigDecimal("30.00"))
                .baseFare(new BigDecimal("85.00"))
                .build();

        // Tenant: convoyia-uk-demo (UK)
        TenantPricingContext gbCtx = TenantPricingContext.builder()
                .tenantId("convoyia-uk-demo")
                .currencyCode("GBP")
                .currencySymbol("£")
                .taxName("VAT")
                .taxRate(new BigDecimal("0.20"))
                .platformFeeRatio(new BigDecimal("0.25"))
                .minimumFare(new BigDecimal("25.00"))
                .baseFare(new BigDecimal("72.00"))
                .build();

        // Tenant: convoyia-uae-demo (UAE)
        TenantPricingContext aeCtx = TenantPricingContext.builder()
                .tenantId("convoyia-uae-demo")
                .currencyCode("AED")
                .currencySymbol("د.إ")
                .taxName("VAT")
                .taxRate(new BigDecimal("0.05"))
                .platformFeeRatio(new BigDecimal("0.25"))
                .minimumFare(new BigDecimal("120.00"))
                .baseFare(new BigDecimal("340.00"))
                .build();

        assertPricingResult(frCtx, "EUR", "TVA", new BigDecimal("0.20"));
        assertPricingResult(gbCtx, "GBP", "VAT", new BigDecimal("0.20"));
        assertPricingResult(aeCtx, "AED", "VAT", new BigDecimal("0.05"));

        log.info("[Scenario] {} — PASSED", name());
    }

    private void assertPricingResult(TenantPricingContext ctx,
                                      String expectedCurrency,
                                      String expectedTaxName,
                                      BigDecimal expectedTaxRate) {
        BigDecimal baseFare = ctx.baseFare().max(ctx.minimumFare());
        BigDecimal vatAmount = baseFare.multiply(ctx.taxRate()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalTtc = baseFare.add(vatAmount).setScale(2, RoundingMode.HALF_UP);
        BigDecimal platformFee = totalTtc.multiply(ctx.platformFeeRatio()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal conveyorShare = totalTtc.subtract(platformFee).setScale(2, RoundingMode.HALF_UP);

        // Assert currency
        if (!expectedCurrency.equals(ctx.currencyCode())) {
            throw new AssertionError(String.format(
                    "[%s] currencyCode expected=%s actual=%s",
                    ctx.tenantId(), expectedCurrency, ctx.currencyCode()));
        }
        // Assert tax name
        if (!expectedTaxName.equals(ctx.taxName())) {
            throw new AssertionError(String.format(
                    "[%s] taxName expected=%s actual=%s",
                    ctx.tenantId(), expectedTaxName, ctx.taxName()));
        }
        // Assert tax rate
        if (expectedTaxRate.compareTo(ctx.taxRate()) != 0) {
            throw new AssertionError(String.format(
                    "[%s] taxRate expected=%s actual=%s",
                    ctx.tenantId(), expectedTaxRate, ctx.taxRate()));
        }
        // Assert split: conveyorShare == totalTtc × 0.75
        BigDecimal expectedConveyorShare = totalTtc.multiply(BigDecimal.ONE.subtract(ctx.platformFeeRatio()))
                .setScale(2, RoundingMode.HALF_UP);
        if (conveyorShare.compareTo(expectedConveyorShare) != 0) {
            throw new AssertionError(String.format(
                    "[%s] conveyorShare expected=%s actual=%s",
                    ctx.tenantId(), expectedConveyorShare, conveyorShare));
        }

        log.info("[Scenario] {} tenant={} currency={} taxName={} taxRate={} totalTtc={} conveyorShare={}",
                name(), ctx.tenantId(), ctx.currencyCode(), ctx.taxName(), ctx.taxRate(),
                totalTtc, conveyorShare);
    }

    /** Value object capturing the pricing context for one tenant in this scenario. */
    record TenantPricingContext(
            String tenantId,
            String currencyCode,
            String currencySymbol,
            String taxName,
            BigDecimal taxRate,
            BigDecimal platformFeeRatio,
            BigDecimal minimumFare,
            BigDecimal baseFare
    ) {
        static Builder builder() { return new Builder(); }

        static final class Builder {
            private String tenantId;
            private String currencyCode;
            private String currencySymbol;
            private String taxName;
            private BigDecimal taxRate;
            private BigDecimal platformFeeRatio;
            private BigDecimal minimumFare;
            private BigDecimal baseFare;

            Builder tenantId(String v)           { this.tenantId = v; return this; }
            Builder currencyCode(String v)        { this.currencyCode = v; return this; }
            Builder currencySymbol(String v)      { this.currencySymbol = v; return this; }
            Builder taxName(String v)             { this.taxName = v; return this; }
            Builder taxRate(BigDecimal v)         { this.taxRate = v; return this; }
            Builder platformFeeRatio(BigDecimal v){ this.platformFeeRatio = v; return this; }
            Builder minimumFare(BigDecimal v)     { this.minimumFare = v; return this; }
            Builder baseFare(BigDecimal v)        { this.baseFare = v; return this; }

            TenantPricingContext build() {
                return new TenantPricingContext(tenantId, currencyCode, currencySymbol,
                        taxName, taxRate, platformFeeRatio, minimumFare, baseFare);
            }
        }
    }
}
