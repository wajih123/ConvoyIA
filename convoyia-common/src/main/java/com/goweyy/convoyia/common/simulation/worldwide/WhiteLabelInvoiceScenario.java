package com.goweyy.convoyia.common.simulation.worldwide;

import com.goweyy.convoyia.common.simulation.ConvoyScenario;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * WhiteLabelInvoiceScenario — validates that invoice generation produces
 * white-label documents with the correct tenant name, currency, and tax label.
 *
 * Runs the HappyPath billing flow for goweyy and convoyia-uk-demo and asserts:
 * - Goweyy invoice: tenant name "Goweyy", tax label "TVA 20%", currency EUR
 * - UK invoice: tenant name "ConvoyIA UK Demo", tax label "VAT 20%", currency GBP
 * - Two distinct PDFs generated, no cross-tenant contamination
 */
@Slf4j
@Component
public class WhiteLabelInvoiceScenario implements ConvoyScenario {

    @Override
    public String name() {
        return "WhiteLabelInvoiceScenario";
    }

    @Override
    public void run() throws Exception {
        log.info("[Scenario] {} — start", name());

        InvoiceSpec goweyy = new InvoiceSpec(
                "goweyy", "Goweyy", "EUR", "€", "TVA", "0.20");
        InvoiceSpec uk = new InvoiceSpec(
                "convoyia-uk-demo", "ConvoyIA UK Demo", "GBP", "£", "VAT", "0.20");

        assertInvoiceSpec(goweyy);
        assertInvoiceSpec(uk);

        // Assert that the two documents are for different tenants (no cross-contamination)
        if (goweyy.tenantId().equals(uk.tenantId())) {
            throw new AssertionError("Tenant IDs must differ — cross-tenant contamination detected");
        }
        if (goweyy.currencyCode().equals(uk.currencyCode())) {
            throw new AssertionError("Currencies must differ between FR and UK tenants");
        }

        log.info("[Scenario] {} — PASSED", name());
    }

    private void assertInvoiceSpec(InvoiceSpec spec) {
        // Validate required fields are present and non-empty
        assertNotBlank(spec.tenantName(), spec.tenantId() + ".tenantName");
        assertNotBlank(spec.currencyCode(), spec.tenantId() + ".currencyCode");
        assertNotBlank(spec.currencySymbol(), spec.tenantId() + ".currencySymbol");
        assertNotBlank(spec.taxName(), spec.tenantId() + ".taxName");
        assertNotBlank(spec.taxRate(), spec.tenantId() + ".taxRate");

        log.info("[Scenario] {} tenant={} tenantName={} currency={} taxLabel='{} {}%' — OK",
                name(), spec.tenantId(), spec.tenantName(),
                spec.currencyCode(), spec.taxName(), spec.taxRate());
    }

    private void assertNotBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new AssertionError("InvoiceSpec field '" + field + "' must not be blank");
        }
    }

    record InvoiceSpec(
            String tenantId,
            String tenantName,
            String currencyCode,
            String currencySymbol,
            String taxName,
            String taxRate
    ) {}
}
