package com.atm.ui.screens;

import com.atm.model.Transaction;
import com.atm.service.AtmSession;
import com.atm.ui.ScreenManager;
import com.atm.ui.UITheme;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class BalanceScreen extends JPanel implements ScreenManager.RefreshableScreen {
    private final ScreenManager screenManager;

    private JLabel nameLabel;
    private JLabel accountNumLabel;
    private JLabel typeLabel;
    private JLabel balanceAmountLabel;
    private JLabel limitLabel;
    private JLabel statusBadge;

    public BalanceScreen(ScreenManager screenManager) {
        this.screenManager = screenManager;
        setLayout(new BorderLayout(15, 15));
        setBackground(UITheme.SCREEN_BG);
        setBorder(BorderFactory.createEmptyBorder(20, 35, 20, 35));

        initComponents();
    }

    private void initComponents() {
        // Header
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setBackground(UITheme.SCREEN_BG);

        JLabel title = new JLabel("REAL-TIME BALANCE INQUIRY", SwingConstants.CENTER);
        title.setFont(UITheme.FONT_TITLE_LARGE);
        title.setForeground(UITheme.ACCENT_CYAN);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("Official Statement of Account Standing", SwingConstants.CENTER);
        subtitle.setFont(UITheme.FONT_SUBTITLE);
        subtitle.setForeground(UITheme.TEXT_MUTED);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        headerPanel.add(title);
        headerPanel.add(Box.createVerticalStrut(4));
        headerPanel.add(subtitle);
        add(headerPanel, BorderLayout.NORTH);

        // Center Content Card
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBackground(UITheme.SCREEN_BG);

        JPanel balanceCard = UITheme.createCardPanel();
        balanceCard.setLayout(new BoxLayout(balanceCard, BoxLayout.Y_AXIS));
        balanceCard.setMaximumSize(new Dimension(500, 240));

        nameLabel = new JLabel("Customer: --");
        nameLabel.setFont(UITheme.FONT_BODY_BOLD);
        nameLabel.setForeground(UITheme.TEXT_WHITE);

        accountNumLabel = new JLabel("Account Number: --");
        accountNumLabel.setFont(UITheme.FONT_MONO_BOLD);
        accountNumLabel.setForeground(UITheme.TEXT_CYAN);

        typeLabel = new JLabel("Account Category: --");
        typeLabel.setFont(UITheme.FONT_BODY);
        typeLabel.setForeground(UITheme.TEXT_MUTED);

        balanceCard.add(nameLabel);
        balanceCard.add(Box.createVerticalStrut(4));
        balanceCard.add(accountNumLabel);
        balanceCard.add(Box.createVerticalStrut(4));
        balanceCard.add(typeLabel);
        balanceCard.add(Box.createVerticalStrut(14));

        // Big Balance Highlight Banner
        JPanel banner = new JPanel(new BorderLayout());
        banner.setBackground(new Color(15, 23, 42));
        banner.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.ACCENT_CYAN, 1, true),
                BorderFactory.createEmptyBorder(12, 18, 12, 18)
        ));

        JLabel availText = new JLabel("AVAILABLE BALANCE");
        availText.setFont(new Font("Segoe UI", Font.BOLD, 12));
        availText.setForeground(UITheme.TEXT_MUTED);

        balanceAmountLabel = new JLabel("$0.00");
        balanceAmountLabel.setFont(new Font("Segoe UI", Font.BOLD, 32));
        balanceAmountLabel.setForeground(UITheme.SUCCESS_GREEN);

        banner.add(availText, BorderLayout.NORTH);
        banner.add(balanceAmountLabel, BorderLayout.CENTER);
        balanceCard.add(banner);
        balanceCard.add(Box.createVerticalStrut(12));

        // Limit & Status
        JPanel metaRow = new JPanel(new BorderLayout());
        metaRow.setBackground(UITheme.SCREEN_CARD);

        limitLabel = new JLabel("Daily Withdrawal Limit: $1,000.00");
        limitLabel.setFont(UITheme.FONT_BODY);
        limitLabel.setForeground(UITheme.TEXT_MUTED);

        statusBadge = new JLabel("Status: ACTIVE ●");
        statusBadge.setFont(UITheme.FONT_BODY_BOLD);
        statusBadge.setForeground(UITheme.SUCCESS_GREEN);

        metaRow.add(limitLabel, BorderLayout.WEST);
        metaRow.add(statusBadge, BorderLayout.EAST);
        balanceCard.add(metaRow);

        centerPanel.add(balanceCard);
        add(centerPanel, BorderLayout.CENTER);

        // Bottom Controls
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        bottomPanel.setBackground(UITheme.SCREEN_BG);

        JButton backBtn = UITheme.createModernButton("⬅ Back to Menu", UITheme.CHASSIS_BG, UITheme.TEXT_WHITE);
        backBtn.addActionListener(e -> screenManager.showScreen("MAIN_MENU"));

        JButton printBtn = UITheme.createModernButton("🖨 Print Balance Slip", UITheme.ACCENT_BLUE, UITheme.TEXT_WHITE);
        printBtn.addActionListener(e -> printBalanceReceipt());

        JButton withdrawBtn = UITheme.createModernButton("💵 Withdraw Cash Now", UITheme.SUCCESS_GREEN, Color.BLACK);
        withdrawBtn.addActionListener(e -> screenManager.showScreen("WITHDRAW"));

        bottomPanel.add(backBtn);
        bottomPanel.add(printBtn);
        bottomPanel.add(withdrawBtn);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void printBalanceReceipt() {
        AtmSession session = screenManager.getSession();
        if (session == null) return;

        Transaction inquiryTx = new Transaction(
                "INQ-" + System.currentTimeMillis() % 1000000,
                session.getAccount().getAccountNumber(),
                session.getCard().getCardNumber(),
                "BALANCE_INQUIRY",
                BigDecimal.ZERO,
                session.getAccount().getBalance(),
                null,
                "ATM-TERMINAL-01",
                "SUCCESS",
                "Inquiry Receipt Printed",
                LocalDateTime.now()
        );

        new ReceiptPopup(screenManager.getFrame(), session, inquiryTx, "Official Balance Slip").setVisible(true);
    }

    @Override
    public void refreshScreen() {
        AtmSession session = screenManager.getSession();
        if (session != null) {
            nameLabel.setText("Customer: " + session.getCustomer().getFullName());
            accountNumLabel.setText("Account Number: " + session.getAccount().getAccountNumber());
            typeLabel.setText("Account Category: " + session.getAccount().getAccountType() + " (" + session.getAccount().getCurrency() + ")");
            balanceAmountLabel.setText("$" + String.format("%.2f", session.getAccount().getBalance()));
            limitLabel.setText("Daily Withdrawal Limit: $" + String.format("%.2f", session.getAccount().getDailyWithdrawalLimit()));
            statusBadge.setText("Status: " + session.getAccount().getStatus() + " ●");
        }
    }
}
