package com.atm.ui.screens;

import com.atm.model.Transaction;
import com.atm.service.AtmSession;
import com.atm.ui.UITheme;

import javax.swing.*;
import java.awt.*;
import java.time.format.DateTimeFormatter;

public class ReceiptPopup extends JDialog {

    public ReceiptPopup(Frame parent, AtmSession session, Transaction tx, String title) {
        super(parent, title, true);
        setLayout(new BorderLayout());
        getContentPane().setBackground(new Color(245, 245, 240)); // Paper white / cream

        JPanel receiptPanel = new JPanel();
        receiptPanel.setLayout(new BoxLayout(receiptPanel, BoxLayout.Y_AXIS));
        receiptPanel.setBackground(new Color(254, 254, 252));
        receiptPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
                BorderFactory.createEmptyBorder(20, 24, 20, 24)
        ));

        // Header
        receiptPanel.add(createCenteredLabel("==================================", UITheme.FONT_MONO));
        receiptPanel.add(createCenteredLabel("APEX NATIONAL BANK", new Font("Consolas", Font.BOLD, 15)));
        receiptPanel.add(createCenteredLabel("WORLDWIDE ATM SERVICES", UITheme.FONT_MONO));
        receiptPanel.add(createCenteredLabel("TERMINAL #ATM-TERMINAL-01", UITheme.FONT_MONO));
        receiptPanel.add(createCenteredLabel("==================================", UITheme.FONT_MONO));
        receiptPanel.add(Box.createVerticalStrut(10));

        // Transaction Details
        receiptPanel.add(createRow("DATE / TIME:", tx.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
        receiptPanel.add(createRow("TX ID:", tx.getTransactionId()));
        receiptPanel.add(createRow("CARD NUMBER:", session.getCard().getMaskedCardNumber()));
        receiptPanel.add(createRow("ACCOUNT:", session.getAccount().getAccountNumber()));
        receiptPanel.add(createRow("TRANSACTION:", tx.getTransactionType()));
        
        if (tx.getAmount().compareTo(java.math.BigDecimal.ZERO) > 0) {
            receiptPanel.add(createRow("AMOUNT:", "$" + String.format("%.2f", tx.getAmount())));
        }
        if (tx.getBeneficiaryAccount() != null) {
            receiptPanel.add(createRow("TRANSFERRED TO:", tx.getBeneficiaryAccount()));
        }
        receiptPanel.add(createRow("AVAIL BALANCE:", "$" + String.format("%.2f", tx.getBalanceAfter())));
        receiptPanel.add(createRow("STATUS:", tx.getStatus()));

        receiptPanel.add(Box.createVerticalStrut(12));
        receiptPanel.add(createCenteredLabel("----------------------------------", UITheme.FONT_MONO));
        receiptPanel.add(createCenteredLabel("AUTH CODE: " + Integer.toHexString(tx.hashCode()).toUpperCase(), UITheme.FONT_MONO));
        receiptPanel.add(createCenteredLabel("THANK YOU FOR BANKING WITH US", new Font("Consolas", Font.BOLD, 12)));
        receiptPanel.add(createCenteredLabel("KEEP THIS RECEIPT FOR YOUR RECORDS", UITheme.FONT_MONO));
        receiptPanel.add(createCenteredLabel("==================================", UITheme.FONT_MONO));

        add(new JScrollPane(receiptPanel), BorderLayout.CENTER);

        // Close Button
        JPanel btnPanel = new JPanel();
        btnPanel.setBackground(new Color(240, 240, 240));
        JButton closeBtn = new JButton("Dismiss Receipt");
        closeBtn.setFont(UITheme.FONT_BODY_BOLD);
        closeBtn.addActionListener(e -> dispose());
        btnPanel.add(closeBtn);
        add(btnPanel, BorderLayout.SOUTH);

        setSize(380, 480);
        setLocationRelativeTo(parent);
    }

    private JLabel createCenteredLabel(String text, Font font) {
        JLabel lbl = new JLabel(text, SwingConstants.CENTER);
        lbl.setFont(font);
        lbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        lbl.setForeground(Color.DARK_GRAY);
        return lbl;
    }

    private JPanel createRow(String label, String val) {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(new Color(254, 254, 252));
        row.setMaximumSize(new Dimension(320, 22));

        JLabel l = new JLabel(label);
        l.setFont(UITheme.FONT_MONO);
        l.setForeground(Color.GRAY);

        JLabel v = new JLabel(val);
        v.setFont(UITheme.FONT_MONO_BOLD);
        v.setForeground(Color.BLACK);

        row.add(l, BorderLayout.WEST);
        row.add(v, BorderLayout.EAST);
        return row;
    }
}
