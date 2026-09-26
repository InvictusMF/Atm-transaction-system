package com.atm.ui.screens;

import com.atm.model.Transaction;
import com.atm.service.AtmService;
import com.atm.service.AtmSession;
import com.atm.ui.ScreenManager;
import com.atm.ui.UITheme;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Map;

public class WithdrawScreen extends JPanel implements ScreenManager.KeypadListener, ScreenManager.RefreshableScreen {
    private final ScreenManager screenManager;
    private final StringBuilder enteredAmount = new StringBuilder();

    private JLabel balanceLabel;
    private JTextField amountField;
    private JLabel feedbackLabel;

    public WithdrawScreen(ScreenManager screenManager) {
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

        JLabel title = new JLabel("CASH WITHDRAWAL", SwingConstants.CENTER);
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

        // Center Panel: Amount Input & Denomination Info
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBackground(UITheme.SCREEN_BG);

        JPanel inputCard = UITheme.createCardPanel();
        inputCard.setLayout(new BoxLayout(inputCard, BoxLayout.Y_AXIS));
        inputCard.setMaximumSize(new Dimension(460, 220));

        JLabel prompt = new JLabel("Enter Desired Withdrawal Amount:");
        prompt.setFont(UITheme.FONT_BODY_BOLD);
        prompt.setForeground(UITheme.TEXT_WHITE);
        prompt.setAlignmentX(Component.CENTER_ALIGNMENT);

        amountField = new JTextField("$0");
        amountField.setFont(new Font("Segoe UI", Font.BOLD, 30));
        amountField.setHorizontalAlignment(JTextField.CENTER);
        amountField.setBackground(new Color(11, 20, 38));
        amountField.setForeground(UITheme.ACCENT_CYAN);
        amountField.setEditable(false);
        amountField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.ACCENT_CYAN, 2, true),
                BorderFactory.createEmptyBorder(8, 20, 8, 20)
        ));
        amountField.setMaximumSize(new Dimension(300, 55));
        amountField.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel denomNotice = new JLabel("ATM Dispenses: $100, $50, $20, $10 notes (Multiples of $10)");
        denomNotice.setFont(UITheme.FONT_BODY);
        denomNotice.setForeground(UITheme.ACCENT_GOLD);
        denomNotice.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Quick increment pills
        JPanel pills = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 6));
        pills.setBackground(UITheme.SCREEN_CARD);
        JButton p50 = UITheme.createPillButton("+$50", null);
        JButton p100 = UITheme.createPillButton("+$100", null);
        JButton p200 = UITheme.createPillButton("+$200", null);
        for (JButton b : new JButton[]{p50, p100, p200}) {
            b.addActionListener(e -> {
                int add = Integer.parseInt(b.getText().replace("+$", ""));
                int current = enteredAmount.length() == 0 ? 0 : Integer.parseInt(enteredAmount.toString());
                enteredAmount.setLength(0);
                enteredAmount.append(current + add);
                updateAmountField();
            });
            pills.add(b);
        }

        inputCard.add(prompt);
        inputCard.add(Box.createVerticalStrut(12));
        inputCard.add(amountField);
        inputCard.add(Box.createVerticalStrut(8));
        inputCard.add(pills);
        inputCard.add(Box.createVerticalStrut(8));
        inputCard.add(denomNotice);

        centerPanel.add(inputCard);
        centerPanel.add(Box.createVerticalStrut(15));

        feedbackLabel = new JLabel("Use keypad or click quick pills, then press Submit", SwingConstants.CENTER);
        feedbackLabel.setFont(UITheme.FONT_BODY);
        feedbackLabel.setForeground(UITheme.TEXT_MUTED);
        feedbackLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        centerPanel.add(feedbackLabel);

        add(centerPanel, BorderLayout.CENTER);

        // Bottom Navigation
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        bottomPanel.setBackground(UITheme.SCREEN_BG);

        JButton backBtn = UITheme.createModernButton("⬅ Back to Menu", UITheme.CHASSIS_BG, UITheme.TEXT_WHITE);
        backBtn.addActionListener(e -> onCancel());

        JButton clearBtn = UITheme.createModernButton("⌫ Clear Amount", UITheme.WARNING_YELLOW, Color.BLACK);
        clearBtn.addActionListener(e -> onClear());

        JButton submitBtn = UITheme.createModernButton("✔ Withdraw Cash", UITheme.SUCCESS_GREEN, Color.BLACK);
        submitBtn.addActionListener(e -> onEnter());

        bottomPanel.add(backBtn);
        bottomPanel.add(clearBtn);
        bottomPanel.add(submitBtn);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void updateAmountField() {
        if (enteredAmount.length() == 0) {
            amountField.setText("$0");
        } else {
            amountField.setText("$" + enteredAmount.toString());
        }
    }

    @Override
    public void onKeyPressed(String key) {
        if (key.matches("\\d") && enteredAmount.length() < 6) {
            enteredAmount.append(key);
            updateAmountField();
        }
    }

    @Override
    public void onClear() {
        enteredAmount.setLength(0);
        updateAmountField();
        feedbackLabel.setText("Amount cleared. Please enter new amount.");
        feedbackLabel.setForeground(UITheme.TEXT_MUTED);
    }

    @Override
    public void onCancel() {
        enteredAmount.setLength(0);
        updateAmountField();
        screenManager.showScreen("MAIN_MENU");
    }

    @Override
    public void onEnter() {
        AtmSession session = screenManager.getSession();
        if (session == null) return;

        if (enteredAmount.length() == 0) {
            feedbackLabel.setText("⚠ Please enter an amount.");
            feedbackLabel.setForeground(UITheme.WARNING_YELLOW);
            return;
        }

        int amount = Integer.parseInt(enteredAmount.toString());
        if (amount <= 0 || amount % 10 != 0) {
            feedbackLabel.setText("⚠ Amount must be a positive multiple of $10.");
            feedbackLabel.setForeground(UITheme.WARNING_YELLOW);
            return;
        }

        AtmService.OperationResult<Map<Integer, Integer>> result = screenManager.getAtmService().withdraw(session, amount);
        if (result.isSuccess()) {
            StringBuilder notes = new StringBuilder("Dispensed Banknotes:\n");
            result.getData().forEach((denom, count) -> notes.append(" • ").append(count).append(" x $").append(denom).append(" note(s)\n"));

            int printReceipt = JOptionPane.showConfirmDialog(
                    this,
                    "Cash withdrawal successful!\n\n" + notes +
                            "\nNew Account Balance: $" + String.format("%.2f", session.getAccount().getBalance()) +
                            "\n\nWould you like an official printed receipt?",
                    "CASH DISPENSED",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.INFORMATION_MESSAGE
            );

            if (printReceipt == JOptionPane.YES_OPTION) {
                List<Transaction> txs = screenManager.getAtmService().getMiniStatement(session, 1);
                if (!txs.isEmpty()) {
                    new ReceiptPopup(screenManager.getFrame(), session, txs.get(0), "ATM Receipt - Cash Withdrawal").setVisible(true);
                }
            }

            screenManager.showScreen("MAIN_MENU");
        } else {
            feedbackLabel.setText("✖ " + result.getMessage());
            feedbackLabel.setForeground(UITheme.DANGER_RED);
            JOptionPane.showMessageDialog(this, result.getMessage(), "WITHDRAWAL ERROR", JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    public void refreshScreen() {
        enteredAmount.setLength(0);
        updateAmountField();
        AtmSession session = screenManager.getSession();
        if (session != null) {
            balanceLabel.setText("Available Balance: $" + String.format("%.2f", session.getAccount().getBalance()) +
                    "  |  Daily Limit: $" + String.format("%.2f", session.getAccount().getDailyWithdrawalLimit()));
        }
        feedbackLabel.setText("Use keypad or click quick pills, then press Submit");
        feedbackLabel.setForeground(UITheme.TEXT_MUTED);
    }
}
