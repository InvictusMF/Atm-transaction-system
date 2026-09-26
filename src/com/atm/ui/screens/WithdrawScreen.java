package com.atm.ui.screens;

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
import java.util.Map;

public class WithdrawScreen extends JPanel implements ScreenManager.KeypadListener, ScreenManager.RefreshableScreen {
    private enum ScreenState {
        INPUT,
        CONFIRMATION,
        PROCESSING,
        SUCCESS,
        ERROR
    }

    private final ScreenManager screenManager;
    private final StringBuilder enteredAmount = new StringBuilder();
    private ScreenState currentState = ScreenState.INPUT;

    // UI Panels for CardLayout
    private final CardLayout stepLayout = new CardLayout();
    private final JPanel stepContainer = new JPanel(stepLayout);

    // Step 1: Input Components
    private JLabel balanceLabel;
    private JTextField amountField;
    private JLabel feedbackLabel;

    // Step 2: Confirmation Components
    private JLabel confAmountLabel;
    private JLabel confBalanceLabel;
    private JLabel confFeeLabel;
    private JLabel confRemainingLabel;

    // Step 4: Success Components
    private JLabel succAmountLabel;
    private JLabel succBalanceLabel;
    private JPanel succNotesPanel;
    private Map<Integer, Integer> lastDispensedNotes;

    // Step 5: Error Components
    private JLabel errTitleLabel;
    private JLabel errMessageLabel;

    public WithdrawScreen(ScreenManager screenManager) {
        this.screenManager = screenManager;
        setLayout(new BorderLayout());
        setBackground(UITheme.SCREEN_BG);

        initAllSteps();
    }

    private void initAllSteps() {
        stepContainer.setBackground(UITheme.SCREEN_BG);
        stepContainer.add(createInputStep(), ScreenState.INPUT.name());
        stepContainer.add(createConfirmationStep(), ScreenState.CONFIRMATION.name());
        stepContainer.add(createProcessingStep(), ScreenState.PROCESSING.name());
        stepContainer.add(createSuccessStep(), ScreenState.SUCCESS.name());
        stepContainer.add(createErrorStep(), ScreenState.ERROR.name());

        add(stepContainer, BorderLayout.CENTER);
    }

    private JPanel createInputStep() {
        JPanel panel = new JPanel(new BorderLayout(14, 14));
        panel.setBackground(UITheme.SCREEN_BG);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 35, 20, 35));

        // Header
        JPanel header = UITheme.createScreenHeader(
                "CASH WITHDRAWAL  •  STEP 1 OF 3",
                "Enter Withdrawal Amount",
                "Specify desired amount in multiples of $10 (Dispenses $100, $50, $20, $10)"
        );
        panel.add(header, BorderLayout.NORTH);

        // Center Input Card
        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);

        JPanel inputCard = UITheme.createCardPanel(18);
        inputCard.setLayout(new BoxLayout(inputCard, BoxLayout.Y_AXIS));
        inputCard.setMaximumSize(new Dimension(500, 240));

        balanceLabel = new JLabel("Available Balance: $0.00", SwingConstants.CENTER);
        balanceLabel.setFont(UITheme.FONT_SUBTITLE);
        balanceLabel.setForeground(UITheme.SUCCESS_GREEN);
        balanceLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        amountField = new JTextField("$0");
        amountField.setFont(new Font("Segoe UI", Font.BOLD, 36));
        amountField.setHorizontalAlignment(JTextField.CENTER);
        amountField.setBackground(new Color(11, 20, 38));
        amountField.setForeground(UITheme.ACCENT_CYAN);
        amountField.setEditable(false);
        amountField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.ACCENT_CYAN, 2, true),
                BorderFactory.createEmptyBorder(6, 20, 6, 20)
        ));
        amountField.setMaximumSize(new Dimension(320, 60));
        amountField.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Quick Increment Pills
        JPanel pills = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 8));
        pills.setOpaque(false);
        JButton p20 = UITheme.createPillButton("+$20", null);
        JButton p50 = UITheme.createPillButton("+$50", null);
        JButton p100 = UITheme.createPillButton("+$100", null);
        JButton p200 = UITheme.createPillButton("+$200", null);

        for (JButton b : new JButton[]{p20, p50, p100, p200}) {
            b.addActionListener(e -> {
                int add = Integer.parseInt(b.getText().replace("+$", ""));
                int current = enteredAmount.length() == 0 ? 0 : Integer.parseInt(enteredAmount.toString());
                enteredAmount.setLength(0);
                enteredAmount.append(current + add);
                updateAmountField();
            });
            pills.add(b);
        }

        JLabel denomNotice = new JLabel("ATM Dispenser denominations: $100, $50, $20, $10 notes");
        denomNotice.setFont(UITheme.FONT_SMALL);
        denomNotice.setForeground(UITheme.ACCENT_GOLD);
        denomNotice.setAlignmentX(Component.CENTER_ALIGNMENT);

        inputCard.add(balanceLabel);
        inputCard.add(Box.createVerticalStrut(10));
        inputCard.add(amountField);
        inputCard.add(Box.createVerticalStrut(8));
        inputCard.add(pills);
        inputCard.add(Box.createVerticalStrut(6));
        inputCard.add(denomNotice);

        center.add(inputCard);
        center.add(Box.createVerticalStrut(12));

        feedbackLabel = new JLabel("Enter amount with keypad or quick pills, then click Continue", SwingConstants.CENTER);
        feedbackLabel.setFont(UITheme.FONT_BODY);
        feedbackLabel.setForeground(UITheme.TEXT_MUTED);
        feedbackLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        center.add(feedbackLabel);

        panel.add(center, BorderLayout.CENTER);

        // Bottom Nav
        JPanel bottomNav = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        bottomNav.setOpaque(false);

        JButton backBtn = UITheme.createModernButton("⬅ Back to Menu", UITheme.CHASSIS_BG, UITheme.TEXT_WHITE);
        backBtn.setPreferredSize(new Dimension(170, 42));
        backBtn.addActionListener(e -> onCancel());

        JButton clearBtn = UITheme.createModernButton("⌫ Clear Amount", UITheme.WARNING_YELLOW, Color.BLACK);
        clearBtn.setPreferredSize(new Dimension(150, 42));
        clearBtn.addActionListener(e -> onClear());

        JButton submitBtn = UITheme.createModernButton("✔ Continue ➡", UITheme.SUCCESS_GREEN, Color.BLACK);
        submitBtn.setPreferredSize(new Dimension(160, 42));
        submitBtn.addActionListener(e -> onEnter());

        bottomNav.add(backBtn);
        bottomNav.add(clearBtn);
        bottomNav.add(submitBtn);

        panel.add(bottomNav, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createConfirmationStep() {
        JPanel panel = new JPanel(new BorderLayout(14, 14));
        panel.setBackground(UITheme.SCREEN_BG);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 35, 20, 35));

        JPanel header = UITheme.createScreenHeader(
                "CASH WITHDRAWAL  •  STEP 2 OF 3",
                "Confirm Withdrawal Details",
                "Please review the transaction summary carefully before dispensing cash"
        );
        panel.add(header, BorderLayout.NORTH);

        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);

        JPanel summaryCard = UITheme.createCardPanel(20);
        summaryCard.setLayout(new BoxLayout(summaryCard, BoxLayout.Y_AXIS));
        summaryCard.setMaximumSize(new Dimension(500, 220));

        confAmountLabel = new JLabel("$0.00");
        confAmountLabel.setFont(new Font("Segoe UI", Font.BOLD, 26));
        confAmountLabel.setForeground(UITheme.ACCENT_CYAN);

        confBalanceLabel = new JLabel("$0.00");
        confBalanceLabel.setFont(UITheme.FONT_BODY_BOLD);
        confBalanceLabel.setForeground(UITheme.TEXT_WHITE);

        confFeeLabel = new JLabel("$0.00 (Free)");
        confFeeLabel.setFont(UITheme.FONT_BODY_BOLD);
        confFeeLabel.setForeground(UITheme.SUCCESS_GREEN);

        confRemainingLabel = new JLabel("$0.00");
        confRemainingLabel.setFont(UITheme.FONT_TITLE);
        confRemainingLabel.setForeground(UITheme.SUCCESS_GREEN);

        summaryCard.add(createSummaryRow("Requested Withdrawal:", confAmountLabel));
        summaryCard.add(Box.createVerticalStrut(10));
        summaryCard.add(createSummaryRow("Current Available Balance:", confBalanceLabel));
        summaryCard.add(Box.createVerticalStrut(10));
        summaryCard.add(createSummaryRow("ATM Transaction Fee:", confFeeLabel));
        summaryCard.add(Box.createVerticalStrut(10));
        JSeparator sep = new JSeparator();
        sep.setForeground(UITheme.CARD_BORDER);
        summaryCard.add(sep);
        summaryCard.add(Box.createVerticalStrut(10));
        summaryCard.add(createSummaryRow("Estimated Remaining Balance:", confRemainingLabel));

        center.add(summaryCard);
        center.add(Box.createVerticalStrut(12));

        JLabel tip = new JLabel("● Cash will be dispensed from the shutter slot below", SwingConstants.CENTER);
        tip.setFont(UITheme.FONT_SMALL);
        tip.setForeground(UITheme.TEXT_MUTED);
        tip.setAlignmentX(Component.CENTER_ALIGNMENT);
        center.add(tip);

        panel.add(center, BorderLayout.CENTER);

        // Bottom Actions
        JPanel bottomNav = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        bottomNav.setOpaque(false);

        JButton backBtn = UITheme.createModernButton("⬅ Modify Amount", UITheme.CHASSIS_BG, UITheme.TEXT_WHITE);
        backBtn.setPreferredSize(new Dimension(170, 42));
        backBtn.addActionListener(e -> setStep(ScreenState.INPUT));

        JButton confirmBtn = UITheme.createModernButton("✔ Confirm & Dispense", UITheme.SUCCESS_GREEN, Color.BLACK);
        confirmBtn.setPreferredSize(new Dimension(200, 42));
        confirmBtn.addActionListener(e -> processWithdrawal());

        bottomNav.add(backBtn);
        bottomNav.add(confirmBtn);

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

        JLabel spinner = new JLabel("⏳", SwingConstants.CENTER);
        spinner.setFont(new Font("Segoe UI", Font.PLAIN, 48));
        spinner.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel title = new JLabel("Dispensing Banknotes...", SwingConstants.CENTER);
        title.setFont(UITheme.FONT_TITLE_LARGE);
        title.setForeground(UITheme.ACCENT_CYAN);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("Please wait while the ATM securely counts and dispenses your cash", SwingConstants.CENTER);
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
        panel.setBorder(BorderFactory.createEmptyBorder(20, 35, 20, 35));

        JPanel header = UITheme.createScreenHeader(
                "CASH WITHDRAWAL  •  COMPLETED",
                "Withdrawal Successful",
                "Please collect your cash from the dispenser slot below"
        );
        panel.add(header, BorderLayout.NORTH);

        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);

        JPanel successCard = UITheme.createCardPanel(18);
        successCard.setLayout(new BoxLayout(successCard, BoxLayout.Y_AXIS));
        successCard.setMaximumSize(new Dimension(500, 220));

        succAmountLabel = new JLabel("$0.00");
        succAmountLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        succAmountLabel.setForeground(UITheme.SUCCESS_GREEN);

        succBalanceLabel = new JLabel("$0.00");
        succBalanceLabel.setFont(UITheme.FONT_TITLE);
        succBalanceLabel.setForeground(UITheme.TEXT_WHITE);

        successCard.add(createSummaryRow("Amount Dispensed:", succAmountLabel));
        successCard.add(Box.createVerticalStrut(8));
        successCard.add(createSummaryRow("Updated Available Balance:", succBalanceLabel));
        successCard.add(Box.createVerticalStrut(10));

        JLabel notesTitle = new JLabel("Dispensed Banknote Breakdown:");
        notesTitle.setFont(UITheme.FONT_SMALL_BOLD);
        notesTitle.setForeground(UITheme.ACCENT_GOLD);
        successCard.add(notesTitle);
        successCard.add(Box.createVerticalStrut(6));

        succNotesPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        succNotesPanel.setOpaque(false);
        successCard.add(succNotesPanel);

        center.add(successCard);
        panel.add(center, BorderLayout.CENTER);

        // Bottom Actions
        JPanel bottomNav = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 10));
        bottomNav.setOpaque(false);

        JButton receiptBtn = UITheme.createModernButton("🖨 View / Print Receipt", UITheme.ACCENT_BLUE, UITheme.TEXT_WHITE);
        receiptBtn.setPreferredSize(new Dimension(190, 42));
        receiptBtn.addActionListener(e -> printReceipt());

        JButton anotherBtn = UITheme.createModernButton("↺ Another Transaction", UITheme.SCREEN_CARD, UITheme.TEXT_WHITE);
        anotherBtn.setPreferredSize(new Dimension(180, 42));
        anotherBtn.addActionListener(e -> {
            enteredAmount.setLength(0);
            updateAmountField();
            setStep(ScreenState.INPUT);
        });

        JButton menuBtn = UITheme.createModernButton("⬅ Back to Dashboard", UITheme.CHASSIS_BG, UITheme.TEXT_WHITE);
        menuBtn.setPreferredSize(new Dimension(170, 42));
        menuBtn.addActionListener(e -> screenManager.showScreen("MAIN_MENU"));

        bottomNav.add(receiptBtn);
        bottomNav.add(anotherBtn);
        bottomNav.add(menuBtn);

        panel.add(bottomNav, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createErrorStep() {
        JPanel panel = new JPanel(new BorderLayout(14, 14));
        panel.setBackground(UITheme.SCREEN_BG);
        panel.setBorder(BorderFactory.createEmptyBorder(30, 35, 30, 35));

        JPanel header = UITheme.createScreenHeader(
                "CASH WITHDRAWAL  •  TRANSACTION DECLINED",
                "Withdrawal Unsuccessful",
                "The requested amount could not be processed"
        );
        panel.add(header, BorderLayout.NORTH);

        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);

        JPanel errCard = UITheme.createCardPanel(24);
        errCard.setLayout(new BoxLayout(errCard, BoxLayout.Y_AXIS));
        errCard.setMaximumSize(new Dimension(500, 180));

        errTitleLabel = new JLabel("Reason for Decline", SwingConstants.CENTER);
        errTitleLabel.setFont(UITheme.FONT_TITLE);
        errTitleLabel.setForeground(UITheme.DANGER_RED);
        errTitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        errMessageLabel = new JLabel("Please review the details below.", SwingConstants.CENTER);
        errMessageLabel.setFont(UITheme.FONT_BODY);
        errMessageLabel.setForeground(UITheme.TEXT_MUTED);
        errMessageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        errCard.add(errTitleLabel);
        errCard.add(Box.createVerticalStrut(10));
        errCard.add(errMessageLabel);

        center.add(errCard);
        panel.add(center, BorderLayout.CENTER);

        // Bottom Actions
        JPanel bottomNav = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        bottomNav.setOpaque(false);

        JButton retryBtn = UITheme.createModernButton("↺ Try Another Amount", UITheme.ACCENT_BLUE, UITheme.TEXT_WHITE);
        retryBtn.setPreferredSize(new Dimension(190, 42));
        retryBtn.addActionListener(e -> setStep(ScreenState.INPUT));

        JButton menuBtn = UITheme.createModernButton("⬅ Return to Dashboard", UITheme.CHASSIS_BG, UITheme.TEXT_WHITE);
        menuBtn.setPreferredSize(new Dimension(190, 42));
        menuBtn.addActionListener(e -> screenManager.showScreen("MAIN_MENU"));

        bottomNav.add(retryBtn);
        bottomNav.add(menuBtn);

        panel.add(bottomNav, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createSummaryRow(String label, JComponent valueComp) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));

        JLabel l = new JLabel(label);
        l.setFont(UITheme.FONT_BODY);
        l.setForeground(UITheme.TEXT_MUTED);

        row.add(l, BorderLayout.WEST);
        row.add(valueComp, BorderLayout.EAST);
        return row;
    }

    private void setStep(ScreenState state) {
        this.currentState = state;
        stepLayout.show(stepContainer, state.name());
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
        if (currentState == ScreenState.INPUT && key.matches("\\d") && enteredAmount.length() < 6) {
            enteredAmount.append(key);
            updateAmountField();
        }
    }

    @Override
    public void onClear() {
        if (currentState == ScreenState.INPUT) {
            enteredAmount.setLength(0);
            updateAmountField();
            feedbackLabel.setText("Amount cleared. Please enter new amount.");
            feedbackLabel.setForeground(UITheme.TEXT_MUTED);
        }
    }

    @Override
    public void onCancel() {
        if (currentState == ScreenState.CONFIRMATION) {
            setStep(ScreenState.INPUT);
        } else {
            enteredAmount.setLength(0);
            updateAmountField();
            setStep(ScreenState.INPUT);
            screenManager.showScreen("MAIN_MENU");
        }
    }

    @Override
    public void onEnter() {
        if (currentState == ScreenState.INPUT) {
            proceedToConfirmation();
        } else if (currentState == ScreenState.CONFIRMATION) {
            processWithdrawal();
        }
    }

    private void proceedToConfirmation() {
        AtmSession session = screenManager.getSession();
        if (session == null) return;

        if (enteredAmount.length() == 0) {
            feedbackLabel.setText("⚠ Please enter an amount to withdraw.");
            feedbackLabel.setForeground(UITheme.WARNING_YELLOW);
            return;
        }

        int amount = Integer.parseInt(enteredAmount.toString());
        if (amount <= 0 || amount % 10 != 0) {
            feedbackLabel.setText("⚠ Amount must be a positive multiple of $10.");
            feedbackLabel.setForeground(UITheme.WARNING_YELLOW);
            return;
        }

        BigDecimal withdrawAmt = BigDecimal.valueOf(amount);
        if (withdrawAmt.compareTo(session.getAccount().getBalance()) > 0) {
            feedbackLabel.setText("⚠ Insufficient balance. Available: " + UITheme.formatCurrency(session.getAccount().getBalance()));
            feedbackLabel.setForeground(UITheme.DANGER_RED);
            return;
        }

        if (withdrawAmt.compareTo(session.getAccount().getDailyWithdrawalLimit()) > 0) {
            feedbackLabel.setText("⚠ Amount exceeds daily limit of " + UITheme.formatCurrency(session.getAccount().getDailyWithdrawalLimit()));
            feedbackLabel.setForeground(UITheme.DANGER_RED);
            return;
        }

        // Populate Confirmation Card
        confAmountLabel.setText(UITheme.formatCurrency(withdrawAmt));
        confBalanceLabel.setText(UITheme.formatCurrency(session.getAccount().getBalance()));
        BigDecimal remaining = session.getAccount().getBalance().subtract(withdrawAmt);
        confRemainingLabel.setText(UITheme.formatCurrency(remaining));

        setStep(ScreenState.CONFIRMATION);
    }

    private void processWithdrawal() {
        AtmSession session = screenManager.getSession();
        if (session == null) return;

        int amount = Integer.parseInt(enteredAmount.toString());
        setStep(ScreenState.PROCESSING);

        // Execute withdrawal via service
        Timer timer = new Timer(500, e -> {
            ((Timer) e.getSource()).stop();
            AtmService.OperationResult<Map<Integer, Integer>> result = screenManager.getAtmService().withdraw(session, amount);
            if (result.isSuccess()) {
                lastDispensedNotes = result.getData();
                succAmountLabel.setText(UITheme.formatCurrency(BigDecimal.valueOf(amount)));
                succBalanceLabel.setText(UITheme.formatCurrency(session.getAccount().getBalance()));

                succNotesPanel.removeAll();
                if (lastDispensedNotes != null) {
                    lastDispensedNotes.forEach((denom, count) -> {
                        JLabel badge = UITheme.createStatusBadge(count + " × $" + denom + " Note(s)", UITheme.SCREEN_CARD_LIGHT, UITheme.ACCENT_CYAN);
                        succNotesPanel.add(badge);
                    });
                }
                succNotesPanel.revalidate();
                succNotesPanel.repaint();

                setStep(ScreenState.SUCCESS);
            } else {
                errTitleLabel.setText("Transaction Declined");
                errMessageLabel.setText("<html><center>" + result.getMessage() + "<br><br>No funds were debited from your account.</center></html>");
                setStep(ScreenState.ERROR);
            }
        });
        timer.setRepeats(false);
        timer.start();
    }

    private void printReceipt() {
        AtmSession session = screenManager.getSession();
        if (session == null) return;
        List<Transaction> txs = screenManager.getAtmService().getMiniStatement(session, 1);
        if (!txs.isEmpty()) {
            new ReceiptPopup(screenManager.getFrame(), session, txs.get(0), "ATM Receipt - Cash Withdrawal").setVisible(true);
        }
    }

    @Override
    public void refreshScreen() {
        enteredAmount.setLength(0);
        updateAmountField();
        setStep(ScreenState.INPUT);

        AtmSession session = screenManager.getSession();
        if (session != null) {
            balanceLabel.setText("Available Balance: " + UITheme.formatCurrency(session.getAccount().getBalance()) +
                    "  |  Daily Limit: " + UITheme.formatCurrency(session.getAccount().getDailyWithdrawalLimit()));
        }
        feedbackLabel.setText("Enter amount with keypad or quick pills, then click Continue");
        feedbackLabel.setForeground(UITheme.TEXT_MUTED);
    }
}
