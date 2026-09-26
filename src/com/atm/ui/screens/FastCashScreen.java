package com.atm.ui.screens;

import com.atm.model.Transaction;
import com.atm.service.AtmService;
import com.atm.service.AtmSession;
import com.atm.ui.ScreenManager;
import com.atm.ui.UITheme;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.util.Map;

public class FastCashScreen extends JPanel implements ScreenManager.KeypadListener, ScreenManager.RefreshableScreen {
    private final ScreenManager screenManager;
    private JLabel balanceLabel;
    private JLabel statusLabel;

    public FastCashScreen(ScreenManager screenManager) {
        this.screenManager = screenManager;
        setLayout(new BorderLayout(15, 15));
        setBackground(UITheme.SCREEN_BG);
        setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        initComponents();
    }

    private void initComponents() {
        // Header
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setBackground(UITheme.SCREEN_BG);

        JLabel title = new JLabel("FAST CASH DISPENSING", SwingConstants.CENTER);
        title.setFont(UITheme.FONT_TITLE_LARGE);
        title.setForeground(UITheme.ACCENT_CYAN);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        balanceLabel = new JLabel("Available Balance: $0.00", SwingConstants.CENTER);
        balanceLabel.setFont(UITheme.FONT_SUBTITLE);
        balanceLabel.setForeground(UITheme.TEXT_WHITE);
        balanceLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        headerPanel.add(title);
        headerPanel.add(Box.createVerticalStrut(4));
        headerPanel.add(balanceLabel);
        add(headerPanel, BorderLayout.NORTH);

        // Center 2x3 Preset Grid
        JPanel gridPanel = new JPanel(new GridLayout(3, 2, 20, 15));
        gridPanel.setBackground(UITheme.SCREEN_BG);

        int[] presets = {20, 40, 60, 100, 200, 500};
        for (int amt : presets) {
            JButton btn = UITheme.createModernButton("$" + amt, UITheme.SCREEN_CARD, UITheme.TEXT_WHITE);
            btn.setFont(new Font("Segoe UI", Font.BOLD, 22));
            btn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(UITheme.ACCENT_BLUE, 2, true),
                    BorderFactory.createEmptyBorder(12, 20, 12, 20)
            ));
            btn.addActionListener(e -> executeFastCash(amt));
            gridPanel.add(btn);
        }
        add(gridPanel, BorderLayout.CENTER);

        // Bottom Navigation & Status
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(UITheme.SCREEN_BG);

        statusLabel = new JLabel("Select an amount to dispense instantly", SwingConstants.CENTER);
        statusLabel.setFont(UITheme.FONT_BODY_BOLD);
        statusLabel.setForeground(UITheme.TEXT_MUTED);

        JButton backBtn = UITheme.createModernButton("⬅ Back to Menu", UITheme.CHASSIS_BG, UITheme.TEXT_WHITE);
        backBtn.addActionListener(e -> screenManager.showScreen("MAIN_MENU"));

        bottomPanel.add(statusLabel, BorderLayout.CENTER);
        bottomPanel.add(backBtn, BorderLayout.EAST);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void executeFastCash(int amount) {
        AtmSession session = screenManager.getSession();
        if (session == null) return;

        AtmService.OperationResult<Map<Integer, Integer>> result = screenManager.getAtmService().withdraw(session, amount);
        if (result.isSuccess()) {
            StringBuilder notes = new StringBuilder("Dispensed Bills:\n");
            result.getData().forEach((denom, count) -> notes.append(" • ").append(count).append(" x $").append(denom).append(" note(s)\n"));

            int printReceipt = JOptionPane.showConfirmDialog(
                    this,
                    "Successfully dispensed $" + amount + "!\n\n" + notes +
                            "\nNew Balance: $" + String.format("%.2f", session.getAccount().getBalance()) +
                            "\n\nWould you like a printed receipt?",
                    "CASH DISPENSED",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.INFORMATION_MESSAGE
            );

            if (printReceipt == JOptionPane.YES_OPTION) {
                // Find latest tx
                java.util.List<Transaction> txs = screenManager.getAtmService().getMiniStatement(session, 1);
                if (!txs.isEmpty()) {
                    new ReceiptPopup(screenManager.getFrame(), session, txs.get(0), "ATM Receipt - Fast Cash").setVisible(true);
                }
            }

            screenManager.showScreen("MAIN_MENU");
        } else {
            statusLabel.setText("✖ " + result.getMessage());
            statusLabel.setForeground(UITheme.DANGER_RED);
            JOptionPane.showMessageDialog(this, result.getMessage(), "TRANSACTION FAILED", JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    public void onKeyPressed(String key) {
        switch (key) {
            case "1": executeFastCash(20); break;
            case "2": executeFastCash(40); break;
            case "3": executeFastCash(60); break;
            case "4": executeFastCash(100); break;
            case "5": executeFastCash(200); break;
            case "6": executeFastCash(500); break;
        }
    }

    @Override public void onClear() {}
    @Override public void onCancel() { screenManager.showScreen("MAIN_MENU"); }
    @Override public void onEnter() {}

    @Override
    public void refreshScreen() {
        AtmSession session = screenManager.getSession();
        if (session != null) {
            balanceLabel.setText("Available Balance: $" + String.format("%.2f", session.getAccount().getBalance()));
        }
        statusLabel.setText("Select an amount to dispense instantly");
        statusLabel.setForeground(UITheme.TEXT_MUTED);
    }
}
