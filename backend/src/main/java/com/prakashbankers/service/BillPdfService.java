package com.prakashbankers.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.prakashbankers.dto.ApiDtos.CustomerResponse;
import com.prakashbankers.dto.ApiDtos.LoanResponse;
import com.prakashbankers.dto.ApiDtos.TransactionResponse;
import com.prakashbankers.entity.TransactionType;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
public class BillPdfService {

    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("dd MMM yyyy");

    public byte[] generateBill(CustomerResponse customer, String loanId) {
        LoanResponse loan = customer.getLoans().stream()
                .filter(l -> l.getLoanId().equals(loanId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Loan not found"));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 36, 36, 36, 36);
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();

            Font title = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Font head = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
            Font body = FontFactory.getFont(FontFactory.HELVETICA, 10);

            doc.add(new Paragraph("PRAKASH BANKERS", title));
            doc.add(new Paragraph("Pledge & Repledge Ledger — Customer Bill", body));
            doc.add(Chunk.NEWLINE);

            doc.add(new Paragraph("Customer: " + customer.getName() + "  |  Phone: " + customer.getPhone(), body));
            doc.add(new Paragraph("Code: " + customer.getCode() + "  |  Address: " + nullSafe(customer.getAddress()), body));
            doc.add(new Paragraph("ID Proof: " + nullSafe(customer.getIdProof()), body));
            doc.add(Chunk.NEWLINE);

            doc.add(new Paragraph("Pledge Details — " + loan.getLoanId(), head));
            doc.add(new Paragraph("Item: " + loan.getItem() + " (" + loan.getMaterial() + ", " + loan.getPurity() + ")", body));
            doc.add(new Paragraph("Weight: Gross " + loan.getGrossWeight() + "g / Net " + loan.getNetWeight() + "g", body));
            doc.add(new Paragraph("Pledge Date: " + loan.getPledgeDate().format(DF) + "  |  Rate: " + loan.getInterestRate() + "% " + loan.getInterestType(), body));
            doc.add(new Paragraph("Status: " + loan.getStatus().name().toUpperCase(), body));
            doc.add(Chunk.NEWLINE);

            var fin = loan.getFinancials();
            PdfPTable summary = new PdfPTable(2);
            summary.setWidthPercentage(100);
            addRow(summary, "Original Pledge Amount", "Rs. " + loan.getPledgeAmount(), body);
            addRow(summary, "Current Principal", "Rs. " + fin.getCurrentPrincipal(), body);
            addRow(summary, "Principal Paid", "Rs. " + fin.getPrincipalPaid(), body);
            addRow(summary, "Interest Generated", "Rs. " + fin.getInterestGenerated(), body);
            addRow(summary, "Interest Paid", "Rs. " + fin.getInterestPaid(), body);
            addRow(summary, "Discount Given", "Rs. " + fin.getDiscountGiven(), body);
            addRow(summary, "Interest Balance", "Rs. " + fin.getInterestBalance(), body);
            addRow(summary, "One Period Interest", "Rs. " + fin.getCycleInterestAmount(), body);
            if (fin.getAdvanceInterestCredit() != null && fin.getAdvanceInterestCredit().signum() > 0) {
                addRow(summary, "Prepaid Interest Credit", "Rs. " + fin.getAdvanceInterestCredit(), body);
            }
            if (fin.getAsOfDate() != null) {
                addRow(summary, "Calculated As Of", fin.getAsOfDate().format(DF), body);
            }
            doc.add(summary);
            doc.add(Chunk.NEWLINE);

            doc.add(new Paragraph("Transaction Ledger", head));
            PdfPTable ledger = new PdfPTable(5);
            ledger.setWidthPercentage(100);
            addHeader(ledger, "Date", head);
            addHeader(ledger, "Type", head);
            addHeader(ledger, "Amount", head);
            addHeader(ledger, "Rate", head);
            addHeader(ledger, "Note", head);
            for (TransactionResponse t : loan.getTransactions()) {
                ledger.addCell(cell(t.getDate().format(DF), body));
                ledger.addCell(cell(t.getType().name(), body));
                ledger.addCell(cell("Rs. " + t.getAmount(), body));
                ledger.addCell(cell(t.getRateAtPayment() != null ? t.getRateAtPayment().toPlainString() : "-", body));
                ledger.addCell(cell(nullSafe(t.getNote()), body));
            }
            doc.add(ledger);

            doc.add(Chunk.NEWLINE);
            doc.add(new Paragraph("Generated on " + java.time.LocalDate.now().format(DF), body));
            doc.close();
        } catch (DocumentException e) {
            throw new RuntimeException("PDF generation failed", e);
        }
        return out.toByteArray();
    }

    private void addRow(PdfPTable t, String label, String value, Font f) {
        t.addCell(cell(label, f));
        t.addCell(cell(value, f));
    }

    private void addHeader(PdfPTable t, String text, Font f) {
        PdfPCell c = new PdfPCell(new Phrase(text, f));
        c.setBackgroundColor(new java.awt.Color(240, 240, 240));
        t.addCell(c);
    }

    private PdfPCell cell(String text, Font f) {
        return new PdfPCell(new Phrase(text != null ? text : "", f));
    }

    private String nullSafe(String s) {
        return s != null ? s : "-";
    }
}
