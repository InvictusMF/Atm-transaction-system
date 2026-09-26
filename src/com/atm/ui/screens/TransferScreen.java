package com.atm.ui.screens;

import com.atm.db.DatabaseManager;
import com.atm.model.Account;
import com.atm.model.Customer;
import com.atm.model.Transaction;
import com.atm.service.AtmService;
import com.atm.service.AtmSession;
import com.atm.ui.ScreenManager;
import com.atm.ui.UITheme;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.util.List;

public class TransferScreen extends JPanel implements ScreenManager.RefreshableScreen {
    private final ScreenManager screenManager;

    private JLabel sourceAccountLabel;
    private JLabel availableBalanceLabel;
    private JComboBox<String> beneficiaryCombo;
    private JTextField customBeneficiaryField;
    private JTextField amountField;
    private JLabel feedbackLabel;

    public TransferScreen(ScreenManager screenManager) {
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

        JLabel title = new JLabel("ELECTRONIC FUND TRANSFER", SwingConstants.CENTER);
        title.setFont(UITheme.FONT_TITLE_LARGE);
        title.setForeground(UITheme.ACCENT_CYAN);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("Instant Intra-Bank & ACH Transfer", SwingConstants.CENTER);
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

        JPanel formCard = UITheme.createCardPanel();
        formCard.setLayout(new GridLayout(4, 2, 12, 14));
        formCard.setMaximumSize(new Dimension(520, 200));

        // Row 1: Source
        JLabel srcTitle = new JLabel("Source Account:");
        srcTitle.setFont(UITheme.FONT_BODY_BOLD);
        srcTitle.setForeground(UITheme.TEXT_WHITE);

        sourceAccountLabel = new JLabel("ACC-0000");
        sourceAccountLabel.setFont(UITheme.FONT_MONO_BOLD);
        sourceAccountLabel.setForeground(UITheme.TEXT_CYAN);

        formCard.add(srcTitle);
        formCard.add(sourceAccountLabel);

        // Row 2: Available Balance
        JLabel balTitle = new JLabel("Available Balance:");
        balTitle.setFont(UITheme.FONT_BODY_BOLD);
        balTitle.setForeground(UITheme.TEXT_WHITE);

        availableBalanceLabel = new JLabel("$0.00");
        availableBalanceLabel.setFont(UITheme.FONT_TITLE);
        availableBalanceLabel.setForeground(UITheme.SUCCESS_GREEN);

        formCard.add(balTitle);
        formCard.add(availableBalanceLabel);

        // Row 3: Beneficiary Selector
        JLabel destTitle = new JLabel("Select Beneficiary:");
        destTitle.setFont(UITheme.FONT_BODY_BOLD);
        destTitle.setForeground(UITheme.TEXT_WHITE);

        beneficiaryCombo = new JComboBox<>();
        beneficiaryCombo.setFont(UITheme.FONT_BODY);
        beneficiaryCombo.setBackground(new Color(30, 41, 59));
        beneficiaryCombo.setForeground(Color.WHITE);

        formCard.add(destTitle);
        formCard.add(beneficiaryCombo);

        // Row 4: Transfer Amount
        JLabel amtTitle = new JLabel("Transfer Amount ($):");
        amtTitle.setFont(UITheme.FONT_BODY_BOLD);
        amtTitle.setForeground(UITheme.TEXT_WHITE);

        amountField = new JTextField();
        amountField.setFont(UITheme.FONT_SUBTITLE);
        amountField.setBackground(new Color(11, 20, 38));
        amountField.setForeground(UITheme.ACCENT_CYAN);
        amountField.setCaretColor(Color.WHITE);
        amountField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.ACCENT_CYAN, 1),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));

        formCard.add(amtTitle);
        formCard.add(amountField);

        centerPanel.add(formCard);
        centerPanel.add(Box.createVerticalStrut(12));

        feedbackLabel = new JLabel("Select destination account and specify amount, then click Confirm", SwingConstants.CENTER);
        feedbackLabel.setFont(UITheme.FONT_BODY);
        feedbackLabel.setForeground(UITheme.TEXT_MUTED);
        feedbackLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        centerPanel.add(feedbackLabel);

        add(centerPanel, BorderLayout.CENTER);

        // Bottom Actions
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        bottomPanel.setBackground(UITheme.SCREEN_BG);

        JButton backBtn = UITheme.createModernButton("⬅ Back to Menu", UITheme.CHASSIS_BG, UITheme.TEXT_WHITE);
        backBtn.addActionListener(e -> screenManager.showScreen("MAIN_MENU"));

        JButton clearBtn = UITheme.createModernButton("⌫ Reset", UITheme.WARNING_YELLOW, Color.BLACK);
        clearBtn.addActionListener(e -> amountField.setText(""));

        JButton submitBtn = UITheme.createModernButton("✔ Confirm Transfer", UITheme.SUCCESS_GREEN, Color.BLACK);
        submitBtn.addActionListener(e -> executeTransfer());

        bottomPanel.add(backBtn);
        bottomPanel.add(clearBtn);
        bottomPanel.add(submitBtn);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void executeTransfer() {
        AtmSession session = screenManager.getSession();
        if (session == null) return;

        String selected = (String) beneficiaryCombo.getSelectedItem();
        if (selected == null || !selected.contains("[")) {
            feedbackLabel.setText("⚠ Please select a valid destination account.");
            feedbackLabel.setForeground(UITheme.WARNING_YELLOW);
            return;
        }

        // Extract account number from "[ACC-XXXX-YYYY] Customer Name"
        String targetAcc = selected.substring(selected.indexOf("[") + 1, selected.indexOf("]"));
        String amtText = amountField.getText().trim().replace("$", "");
        if (amtText.isEmpty()) {
            feedbackLabel.setText("⚠ Please enter an amount to transfer.");
            feedbackLabel.setForeground(UITheme.WARNING_YELLOW);
            return;
        }

        BigDecimal amount;
        try {
            amount = new BigDecimal(amtText);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) throw new NumberFormatException();
        } catch (Exception ex) {
            feedbackLabel.setText("⚠ Please enter a valid positive decimal number.");
            feedbackLabel.setForeground(UITheme.WARNING_YELLOW);
            return;
        }

        // Confirmation Dialog
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Authorize Transfer:\n\n" +
                        "• From: " + session.getAccount().getAccountNumber() + "\n" +
                        "• To Beneficiary: " + selected + "\n" +
                        "• Transfer Amount: $" + String.format("%.2f", amount) + "\n\n" +
                        "Do you wish to proceed?",
                "CONFIRM WIRE TRANSFER",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (confirm != JOptionPane.YES_OPTION) return;

        AtmService.OperationResult<BigDecimal> result = screenManager.getAtmService().transfer(session, targetAcc, amount);
        if (result.isSuccess()) {
            int printReceipt = JOptionPane.showConfirmDialog(
                    this,
                    result.getMessage() + "\n\n" +
                            "New Account Balance: $" + String.format("%.2f", result.getData()) +
                            "\n\nWould you like a printed transfer receipt?",
                    "TRANSFER COMPLETED",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.INFORMATION_MESSAGE
            );

            if (printReceipt == JOptionPane.YES_OPTION) {
                List<Transaction> txs = screenManager.getAtmService().getMiniStatement(session, 1);
                if (!txs.isEmpty()) {
                    new ReceiptPopup(screenManager.getFrame(), session, txs.get(0), "ATM Receipt - Fund Transfer").setVisible(true);
                }
            }

            amountField.setText("");
            screenManager.showScreen("MAIN_MENU");
        } else {
            feedbackLabel.setText("✖ " + result.getMessage());
            feedbackLabel.setForeground(UITheme.DANGER_RED);
            JOptionPane.showMessageDialog(this, result.getMessage(), "TRANSFER ERROR", JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    public void refreshScreen() {
        AtmSession session = screenManager.getSession();
        beneficiaryCombo.removeAllItems();
        amountField.setText("");
        feedbackLabel.setText("Select destination account and specify amount, then click Confirm");
        feedbackLabel.setForeground(UITheme.TEXT_MUTED);

        if (session != null) {
            sourceAccountLabel.setText(session.getAccount().getAccountNumber() + " (" + session.getAccount().getAccountType() + ")");
            availableBalanceLabel.setText("$" + String.format("%.2f", session.getAccount().getBalance()));

            // Populate all other accounts except user's own account
            for (Account acc : DatabaseManager.getInstance().getAllAccounts()) {
                if (!acc.getAccountNumber().equalsIgnoreCase(session.getAccount().getAccountNumber())) {
                    Customer cust = DatabaseManager.getInstance().getCustomer(acc.getCustomerId());
                    String name = cust != null ? cust.getFullName() : "Customer";
                    beneficiaryCombo.addItem("[" + acc.getAccountNumber() + "] " + name + " (" + acc.getAccountType() + ")");
                }
            }
        }
    }
}
