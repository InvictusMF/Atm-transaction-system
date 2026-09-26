package com.atm;

import com.atm.pdf.PdfReportGenerator;
import com.atm.ui.AtmKioskFrame;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        boolean autoPdf = false;

        for (String arg : args) {
            if ("--generate-pdf".equalsIgnoreCase(arg) || "--pdf".equalsIgnoreCase(arg)) {
                autoPdf = true;
            }
        }

        if (autoPdf) {
            System.out.println("=================================================");
            System.out.println("  APEX BANK ATM TRANSACTION SIMULATION SYSTEM   ");
            System.out.println("  Automated Output PDF & Screenshot Generator    ");
            System.out.println("=================================================");
            System.out.println("• Initializing Relational Database Schema...");
            System.out.println("• Initializing Hardware Kiosk UI & Screens...");
            System.out.println("• Capturing 11 Screen States & Database Tables...");

            String pdfPath = PdfReportGenerator.generateSystemReport(null);
            System.out.println("✔ Single PDF Report successfully generated at:");
            System.out.println("  " + pdfPath);
            System.out.println("✔ All individual screenshots saved in /screenshots directory.");
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
