package com.goweyy.convoyia.biller.service;

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

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceGeneratorService {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.of("Europe/Paris"));

    @Value("${minio.base-url:http://localhost:9000}")
    private String minioBaseUrl;

    @Value("${minio.bucket:convoyia-invoices}")
    private String bucket;

    /**
     * Generate client invoice PDF (white background — print-safe).
     * Contains: mission details, vehicle, origin→destination, total TTC, VAT breakdown, date.
     */
    public Mono<byte[]> generateClientInvoice(BillingRequest request, BillingResult result) {
        return Mono.fromCallable(() -> {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document doc = new Document(PageSize.A4);
            PdfWriter.getInstance(doc, baos);
            doc.open();

            // White background — print-safe (NO dark backgrounds in PDF)
            doc.add(new Paragraph("FACTURE CLIENT", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18)));
            doc.add(new Paragraph("Mission: " + request.getMissionId()));
            doc.add(new Paragraph("Date: " + DATE_FORMATTER.format(result.getBilledAt() != null ? result.getBilledAt() : Instant.now())));
            doc.add(new Paragraph(" "));
            doc.add(new Paragraph("CLIENT ID: " + request.getClientId()));
            doc.add(new Paragraph("VÉHICULE: " + request.getVehicleSegment()));
            doc.add(new Paragraph(" "));
            doc.add(new Paragraph("DÉTAIL DE FACTURATION", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));

            if (result.getTotalTtc() != null) {
                BigDecimal ttc = result.getTotalTtc();
                BigDecimal ht = ttc.divide(new BigDecimal("1.20"), 2, java.math.RoundingMode.HALF_UP);
                BigDecimal vat = ttc.subtract(ht);
                doc.add(new Paragraph("Montant HT: " + ht + " EUR"));
                doc.add(new Paragraph("TVA (20%): " + vat + " EUR"));
                doc.add(new Paragraph("TOTAL TTC: " + ttc + " EUR"));
            }

            doc.add(new Paragraph(" "));
            doc.add(new Paragraph("Goweyy — Plateforme de convoyage automobile"));
            doc.close();

            log.info("Generated client invoice for missionId={}", request.getMissionId());
            return baos.toByteArray();
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Generate conveyor receipt PDF.
     * Contains: mission details, gross fare, platform commission (25%), net payout (75%), date.
     */
    public Mono<byte[]> generateConveyorReceipt(BillingRequest request, BillingResult result) {
        return Mono.fromCallable(() -> {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document doc = new Document(PageSize.A4);
            PdfWriter.getInstance(doc, baos);
            doc.open();

            doc.add(new Paragraph("REÇU CONVOYEUR", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18)));
            doc.add(new Paragraph("Mission: " + request.getMissionId()));
            doc.add(new Paragraph("Date: " + DATE_FORMATTER.format(result.getBilledAt() != null ? result.getBilledAt() : Instant.now())));
            doc.add(new Paragraph(" "));
            doc.add(new Paragraph("CONVOYEUR ID: " + request.getConveyorId()));
            doc.add(new Paragraph(" "));
            doc.add(new Paragraph("DÉTAIL DE RÉMUNÉRATION", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));

            if (result.getTotalTtc() != null) {
                doc.add(new Paragraph("Montant brut mission: " + result.getTotalTtc() + " EUR TTC"));
                doc.add(new Paragraph("Commission plateforme (25%): " + result.getPlatformShare() + " EUR"));
                doc.add(new Paragraph("VOTRE PAIEMENT NET (75%): " + result.getConveyorShare() + " EUR"));
            }

            doc.add(new Paragraph(" "));
            doc.add(new Paragraph("Goweyy — Plateforme de convoyage automobile"));
            doc.close();

            log.info("Generated conveyor receipt for missionId={}", request.getMissionId());
            return baos.toByteArray();
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
