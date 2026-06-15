package com.goweyy.convoyia.biller.service;

import com.goweyy.convoyia.common.domain.enums.ConvoyMarket;
import com.goweyy.convoyia.biller.domain.BillingRequest;
import com.goweyy.convoyia.biller.domain.BillingResult;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceGeneratorService {

    @Value("${minio.base-url:http://localhost:9000}")
    private String minioBaseUrl;

    @Value("${minio.bucket:convoyia-invoices}")
    private String bucket;

    /**
     * Generate client invoice PDF (white background — print-safe).
     * Uses tenant currency, tax name and timezone from the billing request context.
     */
    public Mono<byte[]> generateClientInvoice(BillingRequest request, BillingResult result) {
        return Mono.fromCallable(() -> {
            String currencyCode   = request.getCurrencyCode() != null ? request.getCurrencyCode() : ConvoyMarket.FRANCE.getCurrencyCode();
            String currencySymbol = request.getCurrencySymbol() != null ? request.getCurrencySymbol() : "€";
            String taxName        = request.getTaxName() != null ? request.getTaxName() : "TVA";
            BigDecimal taxRate    = request.getTaxRate() != null ? request.getTaxRate() : new BigDecimal("0.20");
            String tenantName     = request.getTenantName() != null ? request.getTenantName() : "Goweyy";
            String timezone       = request.getTimezone() != null ? request.getTimezone() : "Europe/Paris";

            DateTimeFormatter formatter = DateTimeFormatter
                    .ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.of(timezone));

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document doc = new Document(PageSize.A4);
            PdfWriter.getInstance(doc, baos);
            doc.open();

            // White background — print-safe (NO dark backgrounds in PDF)
            doc.add(new Paragraph("FACTURE CLIENT", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18)));
            doc.add(new Paragraph("Mission: " + request.getMissionId()));
            doc.add(new Paragraph("Date: " + formatter.format(
                    result.getBilledAt() != null ? result.getBilledAt() : Instant.now())));
            doc.add(new Paragraph(" "));
            doc.add(new Paragraph("CLIENT ID: " + request.getClientId()));
            doc.add(new Paragraph("VÉHICULE: " + request.getVehicleSegment()));
            doc.add(new Paragraph(" "));
            doc.add(new Paragraph("DÉTAIL DE FACTURATION",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));

            if (result.getTotalTtc() != null && taxRate != null && taxRate.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal divisor = BigDecimal.ONE.add(taxRate);
                BigDecimal ttc = result.getTotalTtc();
                BigDecimal ht = ttc.divide(divisor, 2, java.math.RoundingMode.HALF_UP);
                BigDecimal vat = ttc.subtract(ht);
                String pct = taxRate.multiply(BigDecimal.valueOf(100)).stripTrailingZeros().toPlainString();
                doc.add(new Paragraph("Montant HT: " + ht + " " + currencyCode));
                doc.add(new Paragraph(taxName + " (" + pct + "%): " + vat + " " + currencyCode));
                doc.add(new Paragraph("TOTAL: " + ttc + " " + currencySymbol));
            } else if (result.getTotalTtc() != null) {
                doc.add(new Paragraph("TOTAL: " + result.getTotalTtc() + " " + currencySymbol));
            }

            doc.add(new Paragraph(" "));
            doc.add(new Paragraph(tenantName + " — Plateforme de convoyage automobile"));
            doc.close();

            log.info("Generated client invoice for missionId={} tenant={} currency={}",
                    request.getMissionId(), tenantName, currencyCode);
            return baos.toByteArray();
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Generate conveyor receipt PDF.
     * Uses tenant currency and split ratios from the billing request context.
     */
    public Mono<byte[]> generateConveyorReceipt(BillingRequest request, BillingResult result) {
        return Mono.fromCallable(() -> {
            String currencyCode   = request.getCurrencyCode() != null ? request.getCurrencyCode() : ConvoyMarket.FRANCE.getCurrencyCode();
            String currencySymbol = request.getCurrencySymbol() != null ? request.getCurrencySymbol() : "€";
            String tenantName     = request.getTenantName() != null ? request.getTenantName() : "Goweyy";
            String timezone       = request.getTimezone() != null ? request.getTimezone() : "Europe/Paris";
            BigDecimal platformFeeRatio = request.getPlatformFeeRatio() != null
                    ? request.getPlatformFeeRatio() : new BigDecimal("0.25");

            DateTimeFormatter formatter = DateTimeFormatter
                    .ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.of(timezone));

            int platformPct = platformFeeRatio != null
                    ? platformFeeRatio.multiply(BigDecimal.valueOf(100)).intValue()
                    : 25;
            int conveyorPct = 100 - platformPct;

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document doc = new Document(PageSize.A4);
            PdfWriter.getInstance(doc, baos);
            doc.open();

            doc.add(new Paragraph("REÇU CONVOYEUR", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18)));
            doc.add(new Paragraph("Mission: " + request.getMissionId()));
            doc.add(new Paragraph("Date: " + formatter.format(
                    result.getBilledAt() != null ? result.getBilledAt() : Instant.now())));
            doc.add(new Paragraph(" "));
            doc.add(new Paragraph("CONVOYEUR ID: " + request.getConveyorId()));
            doc.add(new Paragraph(" "));
            doc.add(new Paragraph("DÉTAIL DE RÉMUNÉRATION",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));

            if (result.getTotalTtc() != null) {
                doc.add(new Paragraph("Montant brut mission: " + result.getTotalTtc()
                        + " " + currencySymbol));
                doc.add(new Paragraph("Commission plateforme (" + platformPct + "%): "
                        + result.getPlatformShare() + " " + currencyCode));
                doc.add(new Paragraph("VOTRE PAIEMENT NET (" + conveyorPct + "%): "
                        + result.getConveyorShare() + " " + currencySymbol));
            }

            doc.add(new Paragraph(" "));
            doc.add(new Paragraph(tenantName + " — Plateforme de convoyage automobile"));
            doc.close();

            log.info("Generated conveyor receipt for missionId={} tenant={} currency={}",
                    request.getMissionId(), tenantName, currencyCode);
            return baos.toByteArray();
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
