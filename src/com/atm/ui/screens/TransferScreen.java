package com.atm.ui.screens;

import com.atm.db.DatabaseManager;
import com.atm.model.Account;
import com.atm.model.Customer;
import com.atm.model.Transaction;
import com.atm.service.AtmService;
import com.atm.service.AtmSession;
import com.atm.ui.ModernButton;
import com.atm.ui.ScreenManager;
import com.atm.ui.UITheme;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.util.List;

public class TransferScreen extends JPanel implements ScreenManager.RefreshableScreen {
    private enum ScreenState {
        DETAILS,
        CONFIRMATION,
        PROCESSING,
        SUCCESS,
        ERROR
    }

    private final ScreenManager screenManager;
    private ScreenState currentState = ScreenState.DETAILS;

    private final CardLayout stepLayout = new CardLayout();
    private final JPanel stepContainer = new JPanel(stepLayout);

    // Step 1: Details
    private JLabel sourceAccountLabel;
    private JLabel availableBalanceLabel;
    private JComboBox<String> beneficiaryCombo;
    private JTextField amountField;
    private JLabel feedbackLabel;

    // Step 2: Confirmation
    private JLabel confFromLabel;
    private JLabel confToLabel;
    private JLabel confAmountLabel;
    private JLabel confFeeLabel;
    private JLabel confRemainingLabel;

    // Step 4: Success
    private JLabel succAmountLabel;
    private JLabel succRecipientLabel;
    private JLabel succNewBalLabel;

    // Step 5: Error
    private JLabel errMessageLabel;

    public TransferScreen(ScreenManager screenManager) {
        this.screenManager = screenManager;
        setLayout(new BorderLayout());
        setBackground(UITheme.SCREEN_BG);

        initAllSteps();
    }

    private void initAllSteps() {
        stepContainer.setBackground(UITheme.SCREEN_BG);
        stepContainer.add(createDetailsStep(), ScreenState.DETAILS.name());
        stepContainer.add(createConfirmationStep(), ScreenState.CONFIRMATION.name());
        stepContainer.add(createProcessingStep(), ScreenState.PROCESSING.name());
        stepContainer.add(createSuccessStep(), ScreenState.SUCCESS.name());
        stepContainer.add(createErrorStep(), ScreenState.ERROR.name());

        add(stepContainer, BorderLayout.CENTER);
    }

    private JPanel createDetailsStep() {
        JPanel panel = new JPanel(new BorderLayout(14, 14));
        panel.setBackground(UITheme.SCREEN_BG);
        panel.setBorder(BorderFactory.createEmptyBorder(18, 35, 18, 35));

        JPanel header = UITheme.createScreenHeader(
                "FUND TRANSFER  •  STEP 1 OF 3",
                "Electronic Wire Transfer",
                "Instant zero-fee transfer to any registered account holder"
        );
        panel.add(header, BorderLayout.NORTH);

        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);

        JPanel formCard = UITheme.createCardPanel(18);
        formCard.setLayout(new GridLayout(4, 2, 14, 12));
        formCard.setMaximumSize(new Dimension(560, 200));

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

        // Row 3: Beneficiary
        JLabel destTitle = new JLabel("Select Beneficiary:");
        destTitle.setFont(UITheme.FONT_BODY_BOLD);
        destTitle.setForeground(UITheme.TEXT_WHITE);

        beneficiaryCombo = new JComboBox<>();
        beneficiaryCombo.setFont(UITheme.FONT_BODY);
        beneficiaryCombo.setBackground(new Color(26, 42, 74));
        beneficiaryCombo.setForeground(Color.WHITE);
        formCard.add(destTitle);
        formCard.add(beneficiaryCombo);

        // Row 4: Amount Field & Quick Pills
        JLabel amtTitle = new JLabel("Transfer Amount ($):");
        amtTitle.setFont(UITheme.FONT_BODY_BOLD);
        amtTitle.setForeground(UITheme.TEXT_WHITE);

        JPanel amtInputWrap = new JPanel(new BorderLayout(8, 0));
        amtInputWrap.setOpaque(false);

        amountField = new JTextField();
        amountField.setFont(new Font("Segoe UI", Font.BOLD, 18));
        amountField.setBackground(new Color(11, 20, 38));
        amountField.setForeground(UITheme.ACCENT_CYAN);
        amountField.setCaretColor(Color.WHITE);
        amountField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.ACCENT_CYAN, 1),
                BorderFactory.createEmptyBorder(4, 10, 4, 10)
        ));
        amtInputWrap.add(amountField, BorderLayout.CENTER);

        formCard.add(amtTitle);
        formCard.add(amtInputWrap);

        center.add(formCard);
        center.add(Box.createVerticalStrut(10));

        // Quick amount pills
        JPanel pills = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 4));
        pills.setOpaque(false);
        JButton p50 = UITheme.createPillButton("$50", null);
        JButton p100 = UITheme.createPillButton("$100", null);
        JButton p250 = UITheme.createPillButton("$250", null);
        JButton p500 = UITheme.createPillButton("$500", null);
        for (JButton b : new JButton[]{p50, p100, p250, p500}) {
            b.addActionListener(e -> amountField.setText(b.getText().replace("$", "")));
            pills.add(b);
        }
        center.add(pills);
        center.add(Box.createVerticalStrut(6));

        feedbackLabel = new JLabel("Select destination account and specify amount, then click Review", SwingConstants.CENTER);
        feedbackLabel.setFont(UITheme.FONT_BODY);
        feedbackLabel.setForeground(UITheme.TEXT_MUTED);
        feedbackLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        center.add(feedbackLabel);

        panel.add(center, BorderLayout.CENTER);

        // Bottom Controls
        JPanel bottomNav = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        bottomNav.setOpaque(false);

        JButton backBtn = UITheme.createModernButton("⬅ Back to Menu", UITheme.CHASSIS_BG, UITheme.TEXT_WHITE);
        backBtn.setPreferredSize(new Dimension(170, 42));
        backBtn.addActionListener(e -> screenManager.showScreen("MAIN_MENU"));

        JButton clearBtn = UITheme.createModernButton("⌫ Clear Amount", UITheme.WARNING_YELLOW, Color.BLACK);
        clearBtn.setPreferredSize(new Dimension(150, 42));
        clearBtn.addActionListener(e -> amountField.setText(""));

        JButton reviewBtn = UITheme.createModernButton("✔ Review Transfer ➡", UITheme.SUCCESS_GREEN, Color.BLACK);
        reviewBtn.setPreferredSize(new Dimension(190, 42));
        reviewBtn.addActionListener(e -> proceedToConfirmation());

        bottomNav.add(backBtn);
        bottomNav.add(clearBtn);
        bottomNav.add(reviewBtn);

        panel.add(bottomNav, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createConfirmationStep() {
        JPanel panel = new JPanel(new BorderLayout(14, 14));
        panel.setBackground(UITheme.SCREEN_BG);
        panel.setBorder(BorderFactory.createEmptyBorder(18, 35, 18, 35));

        JPanel header = UITheme.createScreenHeader(
                "FUND TRANSFER  •  STEP 2 OF 3",
                "Authorize Electronic Wire",
                "Confirm beneficiary information and transfer amount"
        );
        panel.add(header, BorderLayout.NORTH);

        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);

        JPanel summaryCard = UITheme.createCardPanel(20);
        summaryCard.setLayout(new BoxLayout(summaryCard, BoxLayout.Y_AXIS));
        summaryCard.setMaximumSize(new Dimension(540, 240));

        confFromLabel = new JLabel("ACC-1001-8842");
        confFromLabel.setFont(UITheme.FONT_BODY_BOLD);
        confFromLabel.setForeground(UITheme.TEXT_WHITE);

        confToLabel = new JLabel("ACC-2002-4419 (Sophia Williams)");
        confToLabel.setFont(UITheme.FONT_BODY_BOLD);
        confToLabel.setForeground(UITheme.TEXT_CYAN);

        confAmountLabel = new JLabel("$0.00");
        confAmountLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        confAmountLabel.setForeground(UITheme.ACCENT_CYAN);

        confFeeLabel = new JLabel("$0.00 (Instant No-Fee ACH)");
        confFeeLabel.setFont(UITheme.FONT_BODY_BOLD);
        confFeeLabel.setForeground(UITheme.SUCCESS_GREEN);

        confRemainingLabel = new JLabel("$0.00");
        confRemainingLabel.setFont(UITheme.FONT_TITLE);
        confRemainingLabel.setForeground(UITheme.SUCCESS_GREEN);

        summaryCard.add(createRow("Originating Account:", confFromLabel));
        summaryCard.add(Box.createVerticalStrut(8));
        summaryCard.add(createRow("Target Beneficiary:", confToLabel));
        summaryCard.add(Box.createVerticalStrut(8));
        summaryCard.add(createRow("Transfer Amount:", confAmountLabel));
        summaryCard.add(Box.createVerticalStrut(8));
        summaryCard.add(createRow("Transfer Service Fee:", confFeeLabel));
        summaryCard.add(Box.createVerticalStrut(8));
        JSeparator sep = new JSeparator();
        sep.setForeground(UITheme.CARD_BORDER);
        summaryCard.add(sep);
        summaryCard.add(Box.createVerticalStrut(8));
        summaryCard.add(createRow("Remaining Sender Balance:", confRemainingLabel));

        center.add(summaryCard);
        panel.add(center, BorderLayout.CENTER);

        // Bottom Controls
        JPanel bottomNav = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        bottomNav.setOpaque(false);

        JButton backBtn = UITheme.createModernButton("⬅ Modify Details", UITheme.CHASSIS_BG, UITheme.TEXT_WHITE);
        backBtn.setPreferredSize(new Dimension(170, 42));
        backBtn.addActionListener(e -> setStep(ScreenState.DETAILS));

        JButton authorizeBtn = UITheme.createModernButton("✔ Authorize & Send Funds", UITheme.SUCCESS_GREEN, Color.BLACK);
        authorizeBtn.setPreferredSize(new Dimension(220, 42));
        authorizeBtn.addActionListener(e -> executeTransfer());

        bottomNav.add(backBtn);
        bottomNav.add(authorizeBtn);

        panel.add(bottomNav, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createProcessingStep() {
        JPanel panel = new JPanel(new BorderLayout(14, 14));
        panel.setBackground(UITheme.SCREEN_BG);
        panel.setBorder(BorderFactory.createEmptyBorder(60, 35, 60, 35));

        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);

        JLabel spinner = new JLabel("↗", SwingConstants.CENTER);
        spinner.setFont(new Font("Segoe UI", Font.PLAIN, 48));
        spinner.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel title = new JLabel("Routing Wire Transfer...", SwingConstants.CENTER);
        title.setFont(UITheme.FONT_TITLE_LARGE);
        title.setForeground(UITheme.ACCENT_CYAN);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("Contacting inter-bank clearing switch. Debiting account...", SwingConstants.CENTER);
        subtitle.setFont(UITheme.FONT_BODY);
        subtitle.setForeground(UITheme.TEXT_MUTED);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        center.add(spinner);
        center.add(Box.createVerticalStrut(16));
        center.add(title);
        center.add(Box.createVerticalStrut(8));
        center.add(subtitle);

        panel.add(center, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createSuccessStep() {
        JPanel panel = new JPanel(new BorderLayout(14, 14));
        panel.setBackground(UITheme.SCREEN_BG);
        panel.setBorder(BorderFactory.createEmptyBorder(18, 35, 18, 35));

        JPanel header = UITheme.createScreenHeader(
                "FUND TRANSFER  •  TRANSACTION COMPLETE",
                "Wire Transfer Authorized!",
                "Funds have been routed and deposited to the beneficiary immediately"
        );
        panel.add(header, BorderLayout.NORTH);

        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);

        JPanel succCard = UITheme.createCardPanel(20);
        succCard.setLayout(new BoxLayout(succCard, BoxLayout.Y_AXIS));
        succCard.setMaximumSize(new Dimension(520, 200));

        succAmountLabel = new JLabel("$0.00");
        succAmountLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        succAmountLabel.setForeground(UITheme.SUCCESS_GREEN);

        succRecipientLabel = new JLabel("Recipient");
        succRecipientLabel.setFont(UITheme.FONT_BODY_BOLD);
        succRecipientLabel.setForeground(UITheme.TEXT_WHITE);

        succNewBalLabel = new JLabel("$0.00");
        succNewBalLabel.setFont(UITheme.FONT_TITLE);
        succNewBalLabel.setForeground(UITheme.ACCENT_CYAN);

        succCard.add(createRow("Amount Transferred:", succAmountLabel));
        succCard.add(Box.createVerticalStrut(8));
        succCard.add(createRow("Beneficiary Account:", succRecipientLabel));
        succCard.add(Box.createVerticalStrut(8));
        succCard.add(createRow("Remaining Available Balance:", succNewBalLabel));
        succCard.add(Box.createVerticalStrut(8));
        succCard.add(createRow("Settlement Status:", UITheme.createStatusBadge("COMPLETED ●", UITheme.SCREEN_CARD_LIGHT, UITheme.SUCCESS_GREEN)));

        center.add(succCard);
        panel.add(center, BorderLayout.CENTER);

        // Bottom Controls
        JPanel bottomNav = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 10));
        bottomNav.setOpaque(false);

        JButton receiptBtn = UITheme.createModernButton("🖨 Print Transfer Slip", UITheme.ACCENT_BLUE, UITheme.TEXT_WHITE);
        receiptBtn.setPreferredSize(new Dimension(190, 42));
        receiptBtn.addActionListener(e -> printTransferReceipt());

        JButton menuBtn = UITheme.createModernButton("⬅ Return to Dashboard", UITheme.CHASSIS_BG, UITheme.TEXT_WHITE);
        menuBtn.setPreferredSize(new Dimension(190, 42));
        menuBtn.addActionListener(e -> {
            amountField.setText("");
            screenManager.showScreen("MAIN_MENU");
        });

        bottomNav.add(receiptBtn);
        bottomNav.add(menuBtn);

        panel.add(bottomNav, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createErrorStep() {
        JPanel panel = new JPanel(new BorderLayout(14, 14));
        panel.setBackground(UITheme.SCREEN_BG);
        panel.setBorder(BorderFactory.createEmptyBorder(30, 35, 30, 35));

        JPanel header = UITheme.createScreenHeader(
                "FUND TRANSFER  •  TRANSACTION DECLINED",
                "Transfer Failed",
                "The requested wire transfer could not be settled"
        );
        panel.add(header, BorderLayout.NORTH);

        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);

        JPanel errCard = UITheme.createCardPanel(24);
        errCard.setLayout(new BoxLayout(errCard, BoxLayout.Y_AXIS));
        errCard.setMaximumSize(new Dimension(520, 160));

        JLabel errTitle = new JLabel("Reason for Decline", SwingConstants.CENTER);
        errTitle.setFont(UITheme.FONT_TITLE);
        errTitle.setForeground(UITheme.DANGER_RED);
        errTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        errMessageLabel = new JLabel("Please review the transfer instructions.", SwingConstants.CENTER);
        errMessageLabel.setFont(UITheme.FONT_BODY);
        errMessageLabel.setForeground(UITheme.TEXT_MUTED);
        errMessageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        errCard.add(errTitle);
        errCard.add(Box.createVerticalStrut(10));
        errCard.add(errMessageLabel);

        center.add(errCard);
        panel.add(center, BorderLayout.CENTER);

        JPanel bottomNav = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        bottomNav.setOpaque(false);

        JButton retryBtn = UITheme.createModernButton("↺ Modify Transfer", UITheme.ACCENT_BLUE, UITheme.TEXT_WHITE);
        retryBtn.setPreferredSize(new Dimension(180, 42));
        retryBtn.addActionListener(e -> setStep(ScreenState.DETAILS));

        JButton menuBtn = UITheme.createModernButton("⬅ Return to Dashboard", UITheme.CHASSIS_BG, UITheme.TEXT_WHITE);
        menuBtn.setPreferredSize(new Dimension(190, 42));
        menuBtn.addActionListener(e -> screenManager.showScreen("MAIN_MENU"));

        bottomNav.add(retryBtn);
        bottomNav.add(menuBtn);

        panel.add(bottomNav, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createRow(String label, JComponent comp) {
        JPanel r = new JPanel(new BorderLayout(8, 0));
        r.setOpaque(false);
        r.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));

        JLabel l = new JLabel(label);
        l.setFont(UITheme.FONT_BODY);
        l.setForeground(UITheme.TEXT_MUTED);

        r.add(l, BorderLayout.WEST);
        r.add(comp, BorderLayout.EAST);
        return r;
    }

    private void setStep(ScreenState state) {
        this.currentState = state;
        stepLayout.show(stepContainer, state.name());
    }

    private void proceedToConfirmation() {
        AtmSession session = screenManager.getSession();
        if (session == null) return;

        String selected = (String) beneficiaryCombo.getSelectedItem();
        if (selected == null || !selected.contains("[")) {
            feedbackLabel.setText("⚠ Please select a valid destination beneficiary.");
            feedbackLabel.setForeground(UITheme.WARNING_YELLOW);
            return;
        }

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
            feedbackLabel.setText("⚠ Please enter a valid positive decimal number (e.g. 150.00).");
            feedbackLabel.setForeground(UITheme.WARNING_YELLOW);
            return;
        }

        if (amount.compareTo(session.getAccount().getBalance()) > 0) {
            feedbackLabel.setText("⚠ Insufficient balance. Available: " + UITheme.formatCurrency(session.getAccount().getBalance()));
            feedbackLabel.setForeground(UITheme.DANGER_RED);
            return;
        }

        // Set Confirmation Data
        confFromLabel.setText(session.getAccount().getAccountNumber() + " (" + session.getCustomer().getFullName() + ")");
        confToLabel.setText(selected);
        confAmountLabel.setText(UITheme.formatCurrency(amount));
        BigDecimal rem = session.getAccount().getBalance().subtract(amount);
        confRemainingLabel.setText(UITheme.formatCurrency(rem));

        setStep(ScreenState.CONFIRMATION);
    }

    private void executeTransfer() {
        AtmSession session = screenManager.getSession();
        if (session == null) return;

        String selected = (String) beneficiaryCombo.getSelectedItem();
        if (selected == null) return;
        String targetAcc = selected.substring(selected.indexOf("[") + 1, selected.indexOf("]"));
        BigDecimal amount = new BigDecimal(amountField.getText().trim().replace("$", ""));

        setStep(ScreenState.PROCESSING);

        Timer timer = new Timer(500, e -> {
            ((Timer) e.getSource()).stop();
            AtmService.OperationResult<BigDecimal> result = screenManager.getAtmService().transfer(session, targetAcc, amount);
            if (result.isSuccess()) {
                succAmountLabel.setText(UITheme.formatCurrency(amount));
                succRecipientLabel.setText(selected);
                succNewBalLabel.setText(UITheme.formatCurrency(result.getData()));
                setStep(ScreenState.SUCCESS);
            } else {
                errMessageLabel.setText("<html><center>" + result.getMessage() + "</center></html>");
                setStep(ScreenState.ERROR);
            }
        });
        timer.setRepeats(false);
        timer.start();
    }

    private void printTransferReceipt() {
        AtmSession session = screenManager.getSession();
        if (session == null) return;
        List<Transaction> txs = screenManager.getAtmService().getMiniStatement(session, 1);
        if (!txs.isEmpty()) {
            new ReceiptPopup(screenManager.getFrame(), session, txs.get(0), "ATM Receipt - Fund Transfer").setVisible(true);
        }
    }

    @Override
    public void refreshScreen() {
        AtmSession session = screenManager.getSession();
        beneficiaryCombo.removeAllItems();
        amountField.setText("");
        feedbackLabel.setText("Select destination account and specify amount, then click Review");
        feedbackLabel.setForeground(UITheme.TEXT_MUTED);
        setStep(ScreenState.DETAILS);

        if (session != null) {
            sourceAccountLabel.setText(session.getAccount().getAccountNumber() + " (" + session.getAccount().getAccountType() + ")");
            availableBalanceLabel.setText(UITheme.formatCurrency(session.getAccount().getBalance()));

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
