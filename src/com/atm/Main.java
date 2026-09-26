package com.atm;

import com.atm.pdf.PdfReportGenerator;
import com.atm.ui.AtmKioskFrame;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        boolean autoPdf = false;
        boolean autoBackendPdf = false;

        for (String arg : args) {
            if ("--generate-pdf".equalsIgnoreCase(arg) || "--pdf".equalsIgnoreCase(arg) || "--frontend-pdf".equalsIgnoreCase(arg)) {
                autoPdf = true;
            } else if ("--generate-backend-pdf".equalsIgnoreCase(arg) || "--backend-pdf".equalsIgnoreCase(arg)) {
                autoBackendPdf = true;
            } else if ("--all-pdfs".equalsIgnoreCase(arg)) {
                autoPdf = true;
                autoBackendPdf = true;
            }
        }

        if (autoPdf || autoBackendPdf) {
            System.out.println("=================================================");
            System.out.println("  APEX BANK ATM TRANSACTION SIMULATION SYSTEM   ");
            System.out.println("  Automated Output PDF & Screenshot Generator    ");
            System.out.println("=================================================");

            if (autoPdf) {
                System.out.println("• Generating Frontend & Database Design PDF Report...");
                String pdfPath = PdfReportGenerator.generateSystemReport(null);
                System.out.println("✔ Single PDF Report successfully generated at:");
                System.out.println("  " + pdfPath);
                System.out.println("✔ All individual screenshots saved in /screenshots directory.");
            }

            if (autoBackendPdf) {
                System.out.println("• Generating Backend Implementation & Database Integration PDF Report...");
                String backendPdfPath = com.atm.pdf.BackendDatabasePdfGenerator.generateReport(null);
                System.out.println("✔ Backend & Database PDF Report successfully generated at:");
                System.out.println("  " + backendPdfPath);
            }

            System.out.println("=================================================");
            return;
        }

        // Standard Interactive GUI Launch
        SwingUtilities.invokeLater(() -> {
            try {
                // Set unified cross-platform Look & Feel for dark ATM terminal consistency
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception ignored) {}

            AtmKioskFrame frame = new AtmKioskFrame();
            frame.setVisible(true);
        });
    }
}
