package com.atm.ui.screens;

import com.atm.model.Transaction;
import com.atm.service.AtmSession;
import com.atm.ui.UITheme;

import javax.swing.*;
import java.awt.*;
import java.time.format.DateTimeFormatter;

public class ReceiptPopup extends JDialog {

    private static final Font FONT_RECEIPT_MONO = new Font("Consolas", Font.PLAIN, 11);
    private static final Font FONT_RECEIPT_BOLD = new Font("Consolas", Font.BOLD, 12);

    public ReceiptPopup(Frame parent, AtmSession session, Transaction tx, String title) {
        super(parent, title, true);
        setLayout(new BorderLayout());
        getContentPane().setBackground(new Color(15, 23, 42));

        JPanel backdrop = new JPanel(new GridBagLayout());
        backdrop.setBackground(new Color(11, 18, 36));
        backdrop.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        // Thermal Paper Panel
        JPanel receiptPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();

                // Drop shadow
                g2.setColor(new Color(0, 0, 0, 90));
                g2.fillRoundRect(4, 4, w - 8, h - 8, 8, 8);

                // Paper Background
                g2.setColor(new Color(253, 253, 249)); // Warm thermal paper
                g2.fillRoundRect(2, 2, w - 4, h - 4, 6, 6);

                // Top zigzag perforated edge
                g2.setColor(new Color(203, 213, 225));
                g2.setStroke(new BasicStroke(1.2f));
                for (int x = 6; x < w - 6; x += 10) {
                    g2.drawLine(x, 6, x + 5, 2);
                    g2.drawLine(x + 5, 2, x + 10, 6);
                }

                // Bottom barcode emulation
                int barY = h - 34;
                g2.setColor(new Color(30, 30, 30));
                int[] barWidths = {2, 1, 3, 1, 2, 4, 1, 2, 3, 2, 1, 4, 2, 1, 3, 2, 4, 1, 2, 3, 1, 2, 4, 2, 1, 3, 1, 2, 3, 2};
                int curX = (w - 180) / 2;
                for (int bw : barWidths) {
                    g2.fillRect(curX, barY, bw, 20);
                    curX += bw + 3;
                }

                g2.dispose();
            }
        };
        receiptPanel.setLayout(new BoxLayout(receiptPanel, BoxLayout.Y_AXIS));
        receiptPanel.setOpaque(false);
        receiptPanel.setBorder(BorderFactory.createEmptyBorder(16, 16, 36, 16));
        receiptPanel.setPreferredSize(new Dimension(360, 490));

        // Bank Branding Header
        receiptPanel.add(createCenteredLabel("====================================", FONT_RECEIPT_MONO));
        receiptPanel.add(createCenteredLabel("APEX NATIONAL BANK", new Font("Consolas", Font.BOLD, 15)));
        receiptPanel.add(createCenteredLabel("WORLDWIDE ATM SERVICES", FONT_RECEIPT_MONO));
        receiptPanel.add(createCenteredLabel("TERMINAL: #ATM-TERMINAL-01", FONT_RECEIPT_MONO));
        receiptPanel.add(createCenteredLabel("====================================", FONT_RECEIPT_MONO));
        receiptPanel.add(Box.createVerticalStrut(8));

        // Details Section
        receiptPanel.add(createRow("DATE/TIME:", tx.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
        receiptPanel.add(createRow("TX ID:", tx.getTransactionId()));
        receiptPanel.add(createRow("CARD NO:", session.getCard().getMaskedCardNumber()));
        receiptPanel.add(createRow("ACC NO:", session.getAccount().getAccountNumber()));
        receiptPanel.add(createRow("OPERATION:", tx.getTransactionType()));

        if (tx.getAmount().compareTo(java.math.BigDecimal.ZERO) > 0) {
            receiptPanel.add(createRow("AMOUNT:", "$" + String.format("%.2f", tx.getAmount())));
        }
        if (tx.getBeneficiaryAccount() != null) {
            receiptPanel.add(createRow("RECIPIENT:", tx.getBeneficiaryAccount()));
        }
        receiptPanel.add(createRow("AVAIL BAL:", "$" + String.format("%.2f", tx.getBalanceAfter())));
        receiptPanel.add(createRow("STATUS:", tx.getStatus() + " (APPROVED)"));

        receiptPanel.add(Box.createVerticalStrut(8));
        receiptPanel.add(createCenteredLabel("------------------------------------", FONT_RECEIPT_MONO));
        receiptPanel.add(createCenteredLabel("AUTH SECURITY HASH:", FONT_RECEIPT_MONO));
        receiptPanel.add(createCenteredLabel(Integer.toHexString(tx.hashCode()).toUpperCase() + "-APEX-01", FONT_RECEIPT_BOLD));
        receiptPanel.add(createCenteredLabel("THANK YOU FOR BANKING WITH APEX", new Font("Consolas", Font.BOLD, 11)));
        receiptPanel.add(createCenteredLabel("PLEASE RETAIN FOR AUDIT RECORDS", FONT_RECEIPT_MONO));
        receiptPanel.add(createCenteredLabel("====================================", FONT_RECEIPT_MONO));

        backdrop.add(receiptPanel);
        add(backdrop, BorderLayout.CENTER);

        // Dismiss Action Footer
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 10));
        btnPanel.setBackground(new Color(11, 18, 36));

        JButton closeBtn = UITheme.createModernButton("Dismiss Receipt Slip", UITheme.ACCENT_BLUE, UITheme.TEXT_WHITE);
        closeBtn.setPreferredSize(new Dimension(200, 38));
        closeBtn.addActionListener(e -> dispose());
        btnPanel.add(closeBtn);

        add(btnPanel, BorderLayout.SOUTH);

        setSize(420, 590);
        setLocationRelativeTo(parent);
    }

    private JLabel createCenteredLabel(String text, Font font) {
        JLabel lbl = new JLabel(text, SwingConstants.CENTER);
        lbl.setFont(font);
        lbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        lbl.setForeground(new Color(45, 55, 72));
        return lbl;
    }

    private JPanel createRow(String label, String val) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(310, 20));
        row.setPreferredSize(new Dimension(310, 20));
        row.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 20));
        row.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel l = new JLabel(label);
        l.setFont(FONT_RECEIPT_MONO);
        l.setForeground(new Color(100, 116, 139));

        JLabel v = new JLabel(val);
        v.setFont(FONT_RECEIPT_BOLD);
        v.setForeground(new Color(15, 23, 42));
        v.setHorizontalAlignment(SwingConstants.RIGHT);

        row.add(l, BorderLayout.WEST);
        row.add(v, BorderLayout.CENTER);
        return row;
    }
}
