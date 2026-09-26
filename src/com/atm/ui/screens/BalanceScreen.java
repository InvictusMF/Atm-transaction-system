package com.atm.ui.screens;

import com.atm.model.Transaction;
import com.atm.service.AtmSession;
import com.atm.ui.ModernButton;
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
    private JLabel cardNumLabel;

    public BalanceScreen(ScreenManager screenManager) {
        this.screenManager = screenManager;
        setLayout(new BorderLayout(14, 14));
        setBackground(UITheme.SCREEN_BG);
        setBorder(BorderFactory.createEmptyBorder(18, 35, 18, 35));

        initComponents();
    }

    private void initComponents() {
        // Header
        JPanel headerPanel = UITheme.createScreenHeader(
                "ACCOUNT STANDING  •  BALANCE INQUIRY",
                "Official Account Balance",
                "Real-time audited statement of funds and transaction allowances"
        );
        add(headerPanel, BorderLayout.NORTH);

        // Center Content Card
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setOpaque(false);

        JPanel balanceCard = UITheme.createCardPanel(20);
        balanceCard.setLayout(new BoxLayout(balanceCard, BoxLayout.Y_AXIS));
        balanceCard.setMaximumSize(new Dimension(560, 260));

        // Account Details Row
        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);

        JPanel userStack = new JPanel();
        userStack.setLayout(new BoxLayout(userStack, BoxLayout.Y_AXIS));
        userStack.setOpaque(false);

        nameLabel = new JLabel("Customer: --");
        nameLabel.setFont(UITheme.FONT_TITLE);
        nameLabel.setForeground(UITheme.TEXT_WHITE);

        accountNumLabel = new JLabel("Account Number: --");
        accountNumLabel.setFont(UITheme.FONT_MONO_BOLD);
        accountNumLabel.setForeground(UITheme.ACCENT_CYAN);

        typeLabel = new JLabel("Account Category: --");
        typeLabel.setFont(UITheme.FONT_SMALL);
        typeLabel.setForeground(UITheme.TEXT_MUTED);

        userStack.add(nameLabel);
        userStack.add(Box.createVerticalStrut(2));
        userStack.add(accountNumLabel);
        userStack.add(Box.createVerticalStrut(2));
        userStack.add(typeLabel);

        statusBadge = UITheme.createStatusBadge("ACTIVE ●", new Color(16, 185, 129, 40), UITheme.SUCCESS_GREEN);

        topRow.add(userStack, BorderLayout.WEST);
        topRow.add(statusBadge, BorderLayout.EAST);
        balanceCard.add(topRow);
        balanceCard.add(Box.createVerticalStrut(14));

        // Available Balance Highlight Box
        JPanel banner = new JPanel(new BorderLayout());
        banner.setBackground(new Color(11, 20, 38));
        banner.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.ACCENT_CYAN, 1, true),
                BorderFactory.createEmptyBorder(12, 18, 12, 18)
        ));

        JLabel availText = new JLabel("CURRENT AVAILABLE BALANCE");
        availText.setFont(UITheme.FONT_SMALL_BOLD);
        availText.setForeground(UITheme.TEXT_DIM);

        balanceAmountLabel = new JLabel("$0.00");
        balanceAmountLabel.setFont(new Font("Segoe UI", Font.BOLD, 36));
        balanceAmountLabel.setForeground(UITheme.SUCCESS_GREEN);

        banner.add(availText, BorderLayout.NORTH);
        banner.add(balanceAmountLabel, BorderLayout.CENTER);
        balanceCard.add(banner);
        balanceCard.add(Box.createVerticalStrut(12));

        // Meta details row (Daily Limit & Associated Card)
        JPanel metaRow = new JPanel(new BorderLayout());
        metaRow.setOpaque(false);
        metaRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));

        limitLabel = new JLabel("Daily Withdrawal Limit: $2,000.00");
        limitLabel.setFont(UITheme.FONT_SMALL);
        limitLabel.setForeground(UITheme.TEXT_MUTED);

        cardNumLabel = new JLabel("Card: **** 4455");
        cardNumLabel.setFont(UITheme.FONT_MONO);
        cardNumLabel.setForeground(UITheme.TEXT_MUTED);
        cardNumLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 8));

        metaRow.add(limitLabel, BorderLayout.WEST);
        metaRow.add(cardNumLabel, BorderLayout.EAST);
        balanceCard.add(metaRow);

        centerPanel.add(balanceCard);
        add(centerPanel, BorderLayout.CENTER);

        // Bottom Controls: Quick Action Shortcuts
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 10));
        bottomPanel.setOpaque(false);

        JButton backBtn = UITheme.createModernButton("Back to Menu", UITheme.CHASSIS_BG, UITheme.TEXT_WHITE);
        backBtn.setPreferredSize(new Dimension(170, 42));
        backBtn.addActionListener(e -> screenManager.showScreen("MAIN_MENU"));

        JButton printBtn = UITheme.createModernButton("Print Balance Slip", UITheme.ACCENT_BLUE, UITheme.TEXT_WHITE);
        printBtn.setPreferredSize(new Dimension(190, 42));
        printBtn.addActionListener(e -> printBalanceReceipt());

        JButton withdrawBtn = UITheme.createModernButton("Withdraw Cash", UITheme.SUCCESS_GREEN, Color.BLACK);
        withdrawBtn.setPreferredSize(new Dimension(170, 42));
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
            nameLabel.setText(session.getCustomer().getFullName());
            accountNumLabel.setText("Account Number: " + session.getAccount().getAccountNumber());
            typeLabel.setText("Category: " + session.getAccount().getAccountType() + "  |  Currency: " + session.getAccount().getCurrency());
            balanceAmountLabel.setText(UITheme.formatCurrency(session.getAccount().getBalance()));
            limitLabel.setText("Daily Withdrawal Limit: " + UITheme.formatCurrency(session.getAccount().getDailyWithdrawalLimit()));
            cardNumLabel.setText("Card: " + session.getCard().getMaskedCardNumber());
            statusBadge.setText(" " + session.getAccount().getStatus() + " ● ");
        }
    }
}
