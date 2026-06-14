package com.goweyy.convoyia.biller.convoy.agent;

import com.goweyy.convoyia.biller.convoy.dto.ConvoyBillingRequest;
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;

/**
 * ConvoyInvoiceGeneratorService — PDF invoice generation for ConvoyIA.
 * Rule 11: WHITE background only.
 */
@Slf4j
@Service
public class ConvoyInvoiceGeneratorService {

    public String generateClientInvoice(ConvoyBillingRequest request) {
        log.info("Generating client invoice missionId={}", request.getMissionId());
        byte[] pdf = buildPdf("CLIENT INVOICE", request);
        // TODO: upload to MinIO/S3 and return URL
        return "invoices/" + request.getMissionId() + "/client.pdf";
    }

    public String generateConveyorReceipt(ConvoyBillingRequest request) {
        log.info("Generating conveyor receipt missionId={}", request.getMissionId());
        byte[] pdf = buildPdf("CONVEYOR RECEIPT", request);
        // TODO: upload to MinIO/S3 and return URL
        return "invoices/" + request.getMissionId() + "/conveyor.pdf";
    }

    private byte[] buildPdf(String title, ConvoyBillingRequest request) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document doc = new Document();
            PdfWriter.getInstance(doc, baos);
            doc.open();
            // Rule 11: WHITE background
            doc.add(new Paragraph(title, new Font(Font.HELVETICA, 16, Font.BOLD)));
            doc.add(new Paragraph("Mission ID: " + request.getMissionId()));
            doc.add(new Paragraph("Tenant: " + request.getTenantId()));
            if (request.getPricingResult() != null && request.getPricingResult().getPricingBreakdown() != null) {
                var b = request.getPricingResult().getPricingBreakdown();
                doc.add(new Paragraph("Total TTC: " + b.getTotalTtc() + " " + b.getCurrencyCode()));
            }
            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate PDF: {}", e.getMessage());
            return new byte[0];
        }
    }
}
