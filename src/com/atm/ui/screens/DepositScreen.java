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

public class DepositScreen extends JPanel implements ScreenManager.RefreshableScreen {
    private enum ScreenState {
        HOPPER,
        CONFIRMATION,
        PROCESSING,
        SUCCESS,
        ERROR
    }

    private final ScreenManager screenManager;
    private ScreenState currentState = ScreenState.HOPPER;

    private final CardLayout stepLayout = new CardLayout();
    private final JPanel stepContainer = new JPanel(stepLayout);

    // Step 1: Hopper State
    private JSpinner spin100;
    private JSpinner spin50;
    private JSpinner spin20;
    private JSpinner spin10;
    private JLabel subtotal100;
    private JLabel subtotal50;
    private JLabel subtotal20;
    private JLabel subtotal10;
    private JLabel totalLabel;
    private JLabel currentBalanceLabel;

    // Step 2: Confirmation State
    private JLabel confTotalLabel;
    private JLabel confAccountLabel;
    private JLabel confCurBalLabel;
    private JLabel confNewBalLabel;
    private JPanel confBreakdownPanel;

    // Step 4: Success State
    private JLabel succTotalLabel;
    private JLabel succNewBalLabel;
    private JPanel succBreakdownPanel;

    public DepositScreen(ScreenManager screenManager) {
        this.screenManager = screenManager;
        setLayout(new BorderLayout());
        setBackground(UITheme.SCREEN_BG);

        initAllSteps();
    }

    private void initAllSteps() {
        stepContainer.setBackground(UITheme.SCREEN_BG);
        stepContainer.add(createHopperStep(), ScreenState.HOPPER.name());
        stepContainer.add(createConfirmationStep(), ScreenState.CONFIRMATION.name());
        stepContainer.add(createProcessingStep(), ScreenState.PROCESSING.name());
        stepContainer.add(createSuccessStep(), ScreenState.SUCCESS.name());
        stepContainer.add(createErrorStep(), ScreenState.ERROR.name());

        add(stepContainer, BorderLayout.CENTER);
    }

    private JPanel createHopperStep() {
        JPanel panel = new JPanel(new BorderLayout(14, 14));
        panel.setBackground(UITheme.SCREEN_BG);
        panel.setBorder(BorderFactory.createEmptyBorder(18, 32, 18, 32));

        JPanel header = UITheme.createScreenHeader(
                "CASH DEPOSIT HOPPER  •  STEP 1 OF 3",
                "Insert Banknotes into Hopper",
                "Place notes flat into the slot. Accepted: $100, $50, $20, $10 bills."
        );
        panel.add(header, BorderLayout.NORTH);

        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);

        currentBalanceLabel = new JLabel("Current Balance: $0.00", SwingConstants.CENTER);
        currentBalanceLabel.setFont(UITheme.FONT_SUBTITLE);
        currentBalanceLabel.setForeground(UITheme.SUCCESS_GREEN);
        currentBalanceLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        center.add(currentBalanceLabel);
        center.add(Box.createVerticalStrut(10));

        // Hopper Card with 4 Denomination Rows
        JPanel hopperCard = UITheme.createCardPanel(14);
        hopperCard.setLayout(new GridLayout(4, 1, 0, 8));
        hopperCard.setMaximumSize(new Dimension(560, 200));

        spin100 = createSpinner();
        spin50 = createSpinner();
        spin20 = createSpinner();
        spin10 = createSpinner();

        subtotal100 = createSubtotalLabel();
        subtotal50 = createSubtotalLabel();
        subtotal20 = createSubtotalLabel();
        subtotal10 = createSubtotalLabel();

        hopperCard.add(createDenomRow("$100 Banknote", spin100, subtotal100, 100));
        hopperCard.add(createDenomRow("$50 Banknote",  spin50,  subtotal50,  50));
        hopperCard.add(createDenomRow("$20 Banknote",  spin20,  subtotal20,  20));
        hopperCard.add(createDenomRow("$10 Banknote",  spin10,  subtotal10,  10));

        center.add(hopperCard);
        center.add(Box.createVerticalStrut(12));

        // Total Cash Inserted Banner
        JPanel totalBox = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 8));
        totalBox.setBackground(new Color(15, 23, 42));
        totalBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.SUCCESS_GREEN, 1, true),
                BorderFactory.createEmptyBorder(4, 20, 4, 20)
        ));
        totalBox.setMaximumSize(new Dimension(460, 44));

        totalLabel = new JLabel("Total Cash Inserted: $0.00");
        totalLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        totalLabel.setForeground(UITheme.SUCCESS_GREEN);
        totalBox.add(totalLabel);
        center.add(totalBox);

        panel.add(center, BorderLayout.CENTER);

        // Bottom Controls
        JPanel bottomNav = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        bottomNav.setOpaque(false);

        JButton backBtn = UITheme.createModernButton("⬅ Cancel & Return Notes", UITheme.CHASSIS_BG, UITheme.TEXT_WHITE);
        backBtn.setPreferredSize(new Dimension(190, 42));
        backBtn.addActionListener(e -> screenManager.showScreen("MAIN_MENU"));

        JButton resetBtn = UITheme.createModernButton("↺ Reset Counts", UITheme.WARNING_YELLOW, Color.BLACK);
        resetBtn.setPreferredSize(new Dimension(150, 42));
        resetBtn.addActionListener(e -> resetSpinners());

        JButton confirmBtn = UITheme.createModernButton("✔ Review Deposit ➡", UITheme.SUCCESS_GREEN, Color.BLACK);
        confirmBtn.setPreferredSize(new Dimension(180, 42));
        confirmBtn.addActionListener(e -> proceedToConfirmation());

        bottomNav.add(backBtn);
        bottomNav.add(resetBtn);
        bottomNav.add(confirmBtn);

        panel.add(bottomNav, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createConfirmationStep() {
        JPanel panel = new JPanel(new BorderLayout(14, 14));
        panel.setBackground(UITheme.SCREEN_BG);
        panel.setBorder(BorderFactory.createEmptyBorder(18, 35, 18, 35));

        JPanel header = UITheme.createScreenHeader(
                "CASH DEPOSIT  •  STEP 2 OF 3",
                "Confirm Cash Deposit",
                "Verify the counted banknotes and credit amount below"
        );
        panel.add(header, BorderLayout.NORTH);

        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);

        JPanel summaryCard = UITheme.createCardPanel(18);
        summaryCard.setLayout(new BoxLayout(summaryCard, BoxLayout.Y_AXIS));
        summaryCard.setMaximumSize(new Dimension(520, 240));

        confTotalLabel = new JLabel("$0.00");
        confTotalLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        confTotalLabel.setForeground(UITheme.SUCCESS_GREEN);

        confAccountLabel = new JLabel("ACC-1001-8842");
        confAccountLabel.setFont(UITheme.FONT_BODY_BOLD);
        confAccountLabel.setForeground(UITheme.TEXT_WHITE);

        confCurBalLabel = new JLabel("$0.00");
        confCurBalLabel.setFont(UITheme.FONT_BODY_BOLD);
        confCurBalLabel.setForeground(UITheme.TEXT_WHITE);

        confNewBalLabel = new JLabel("$0.00");
        confNewBalLabel.setFont(UITheme.FONT_TITLE);
        confNewBalLabel.setForeground(UITheme.ACCENT_CYAN);

        summaryCard.add(createRow("Total Cash Verified:", confTotalLabel));
        summaryCard.add(Box.createVerticalStrut(8));
        summaryCard.add(createRow("Credited Account:", confAccountLabel));
        summaryCard.add(Box.createVerticalStrut(8));
        summaryCard.add(createRow("Current Balance:", confCurBalLabel));
        summaryCard.add(Box.createVerticalStrut(8));
        JSeparator sep = new JSeparator();
        sep.setForeground(UITheme.CARD_BORDER);
        summaryCard.add(sep);
        summaryCard.add(Box.createVerticalStrut(8));
        summaryCard.add(createRow("Projected Balance After Deposit:", confNewBalLabel));
        summaryCard.add(Box.createVerticalStrut(8));

        confBreakdownPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        confBreakdownPanel.setOpaque(false);
        summaryCard.add(confBreakdownPanel);

        center.add(summaryCard);
        panel.add(center, BorderLayout.CENTER);

        // Bottom Controls
        JPanel bottomNav = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        bottomNav.setOpaque(false);

        JButton backBtn = UITheme.createModernButton("⬅ Add More Notes", UITheme.CHASSIS_BG, UITheme.TEXT_WHITE);
        backBtn.setPreferredSize(new Dimension(170, 42));
        backBtn.addActionListener(e -> setStep(ScreenState.HOPPER));

        JButton confirmBtn = UITheme.createModernButton("✔ Accept & Deposit Cash", UITheme.SUCCESS_GREEN, Color.BLACK);
        confirmBtn.setPreferredSize(new Dimension(210, 42));
        confirmBtn.addActionListener(e -> executeDeposit());

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

        JLabel spinner = new JLabel("📥", SwingConstants.CENTER);
        spinner.setFont(new Font("Segoe UI", Font.PLAIN, 48));
        spinner.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel title = new JLabel("Validating & Storing Banknotes...", SwingConstants.CENTER);
        title.setFont(UITheme.FONT_TITLE_LARGE);
        title.setForeground(UITheme.ACCENT_CYAN);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("Currency validation scanner active. Crediting your account...", SwingConstants.CENTER);
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
                "CASH DEPOSIT  •  TRANSACTION COMPLETE",
                "Deposit Successful!",
                "Your funds have been credited to your account immediately"
        );
        panel.add(header, BorderLayout.NORTH);

        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);

        JPanel succCard = UITheme.createCardPanel(18);
        succCard.setLayout(new BoxLayout(succCard, BoxLayout.Y_AXIS));
        succCard.setMaximumSize(new Dimension(500, 210));

        succTotalLabel = new JLabel("$0.00");
        succTotalLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        succTotalLabel.setForeground(UITheme.SUCCESS_GREEN);

        succNewBalLabel = new JLabel("$0.00");
        succNewBalLabel.setFont(UITheme.FONT_TITLE);
        succNewBalLabel.setForeground(UITheme.ACCENT_CYAN);

        succCard.add(createRow("Total Amount Deposited:", succTotalLabel));
        succCard.add(Box.createVerticalStrut(8));
        succCard.add(createRow("Updated Account Balance:", succNewBalLabel));
        succCard.add(Box.createVerticalStrut(10));

        JLabel breakTitle = new JLabel("Banknotes Stored in Safe:");
        breakTitle.setFont(UITheme.FONT_SMALL_BOLD);
        breakTitle.setForeground(UITheme.ACCENT_GOLD);
        succCard.add(breakTitle);
        succCard.add(Box.createVerticalStrut(6));

        succBreakdownPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        succBreakdownPanel.setOpaque(false);
        succCard.add(succBreakdownPanel);

        center.add(succCard);
        panel.add(center, BorderLayout.CENTER);

        // Bottom Controls
        JPanel bottomNav = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 10));
        bottomNav.setOpaque(false);

        JButton receiptBtn = UITheme.createModernButton("🖨 Print Deposit Slip", UITheme.ACCENT_BLUE, UITheme.TEXT_WHITE);
        receiptBtn.setPreferredSize(new Dimension(190, 42));
        receiptBtn.addActionListener(e -> printDepositReceipt());

        JButton menuBtn = UITheme.createModernButton("⬅ Return to Dashboard", UITheme.CHASSIS_BG, UITheme.TEXT_WHITE);
        menuBtn.setPreferredSize(new Dimension(190, 42));
        menuBtn.addActionListener(e -> {
            resetSpinners();
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
                "CASH DEPOSIT  •  ERROR",
                "Deposit Failed",
                "No currency was detected or the notes could not be authenticated"
        );
        panel.add(header, BorderLayout.NORTH);

        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);

        JPanel errCard = UITheme.createCardPanel(24);
        errCard.setLayout(new BoxLayout(errCard, BoxLayout.Y_AXIS));
        errCard.setMaximumSize(new Dimension(500, 160));

        JLabel errTitle = new JLabel("No Banknotes Detected", SwingConstants.CENTER);
        errTitle.setFont(UITheme.FONT_TITLE);
        errTitle.setForeground(UITheme.DANGER_RED);
        errTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel errSub = new JLabel("Please insert at least one genuine banknote into the hopper slot.", SwingConstants.CENTER);
        errSub.setFont(UITheme.FONT_BODY);
        errSub.setForeground(UITheme.TEXT_MUTED);
        errSub.setAlignmentX(Component.CENTER_ALIGNMENT);

        errCard.add(errTitle);
        errCard.add(Box.createVerticalStrut(10));
        errCard.add(errSub);

        center.add(errCard);
        panel.add(center, BorderLayout.CENTER);

        JPanel bottomNav = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        bottomNav.setOpaque(false);

        JButton retryBtn = UITheme.createModernButton("↺ Try Again", UITheme.ACCENT_BLUE, UITheme.TEXT_WHITE);
        retryBtn.setPreferredSize(new Dimension(160, 42));
        retryBtn.addActionListener(e -> setStep(ScreenState.HOPPER));

        JButton menuBtn = UITheme.createModernButton("⬅ Return to Dashboard", UITheme.CHASSIS_BG, UITheme.TEXT_WHITE);
        menuBtn.setPreferredSize(new Dimension(190, 42));
        menuBtn.addActionListener(e -> screenManager.showScreen("MAIN_MENU"));

        bottomNav.add(retryBtn);
        bottomNav.add(menuBtn);

        panel.add(bottomNav, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createDenomRow(String label, JSpinner spinner, JLabel subtotal, int multiplier) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(false);

        JLabel l = new JLabel(label);
        l.setFont(UITheme.FONT_BODY_BOLD);
        l.setForeground(UITheme.TEXT_WHITE);
        l.setPreferredSize(new Dimension(140, 28));

        JPanel spinnerWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        spinnerWrap.setOpaque(false);
        spinnerWrap.add(spinner);

        row.add(l, BorderLayout.WEST);
        row.add(spinnerWrap, BorderLayout.CENTER);
        row.add(subtotal, BorderLayout.EAST);
        return row;
    }

    private JSpinner createSpinner() {
        SpinnerNumberModel model = new SpinnerNumberModel(0, 0, 100, 1);
        JSpinner spinner = new JSpinner(model);
        spinner.setFont(new Font("Segoe UI", Font.BOLD, 15));
        spinner.setPreferredSize(new Dimension(85, 30));
        spinner.addChangeListener(e -> updateTotal());
        return spinner;
    }

    private JLabel createSubtotalLabel() {
        JLabel l = new JLabel("$0.00", SwingConstants.RIGHT);
        l.setFont(UITheme.FONT_MONO_BOLD);
        l.setForeground(UITheme.TEXT_MUTED);
        l.setPreferredSize(new Dimension(90, 28));
        return l;
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

    private void updateTotal() {
        int n100 = (Integer) spin100.getValue();
        int n50  = (Integer) spin50.getValue();
        int n20  = (Integer) spin20.getValue();
        int n10  = (Integer) spin10.getValue();

        subtotal100.setText("$" + (n100 * 100) + ".00");
        subtotal50.setText("$" + (n50 * 50) + ".00");
        subtotal20.setText("$" + (n20 * 20) + ".00");
        subtotal10.setText("$" + (n10 * 10) + ".00");

        int total = (n100 * 100) + (n50 * 50) + (n20 * 20) + (n10 * 10);
        totalLabel.setText("Total Cash Inserted: " + UITheme.formatCurrency(total));
    }

    private void resetSpinners() {
        spin100.setValue(0);
        spin50.setValue(0);
        spin20.setValue(0);
        spin10.setValue(0);
        updateTotal();
    }

    private void proceedToConfirmation() {
        AtmSession session = screenManager.getSession();
        if (session == null) return;

        int n100 = (Integer) spin100.getValue();
        int n50  = (Integer) spin50.getValue();
        int n20  = (Integer) spin20.getValue();
        int n10  = (Integer) spin10.getValue();
        int total = (n100 * 100) + (n50 * 50) + (n20 * 20) + (n10 * 10);

        if (total <= 0) {
            setStep(ScreenState.ERROR);
            return;
        }

        confTotalLabel.setText(UITheme.formatCurrency(total));
        confAccountLabel.setText(session.getAccount().getAccountNumber() + " (" + session.getAccount().getAccountType() + ")");
        confCurBalLabel.setText(UITheme.formatCurrency(session.getAccount().getBalance()));
        BigDecimal newBal = session.getAccount().getBalance().add(BigDecimal.valueOf(total));
        confNewBalLabel.setText(UITheme.formatCurrency(newBal));

        confBreakdownPanel.removeAll();
        if (n100 > 0) confBreakdownPanel.add(UITheme.createStatusBadge(n100 + " × $100", UITheme.SCREEN_CARD_LIGHT, UITheme.ACCENT_CYAN));
        if (n50 > 0)  confBreakdownPanel.add(UITheme.createStatusBadge(n50 + " × $50",   UITheme.SCREEN_CARD_LIGHT, UITheme.ACCENT_CYAN));
        if (n20 > 0)  confBreakdownPanel.add(UITheme.createStatusBadge(n20 + " × $20",   UITheme.SCREEN_CARD_LIGHT, UITheme.ACCENT_CYAN));
        if (n10 > 0)  confBreakdownPanel.add(UITheme.createStatusBadge(n10 + " × $10",   UITheme.SCREEN_CARD_LIGHT, UITheme.ACCENT_CYAN));
        confBreakdownPanel.revalidate();
        confBreakdownPanel.repaint();

        setStep(ScreenState.CONFIRMATION);
    }

    private void executeDeposit() {
        AtmSession session = screenManager.getSession();
        if (session == null) return;

        int n100 = (Integer) spin100.getValue();
        int n50  = (Integer) spin50.getValue();
        int n20  = (Integer) spin20.getValue();
        int n10  = (Integer) spin10.getValue();
        int total = (n100 * 100) + (n50 * 50) + (n20 * 20) + (n10 * 10);

        setStep(ScreenState.PROCESSING);

        Timer timer = new Timer(500, e -> {
            ((Timer) e.getSource()).stop();
            AtmService.OperationResult<BigDecimal> result = screenManager.getAtmService().deposit(session, n100, n50, n20, n10);
            if (result.isSuccess()) {
                succTotalLabel.setText(UITheme.formatCurrency(total));
                succNewBalLabel.setText(UITheme.formatCurrency(result.getData()));

                succBreakdownPanel.removeAll();
                if (n100 > 0) succBreakdownPanel.add(UITheme.createStatusBadge(n100 + " × $100", UITheme.SCREEN_CARD_LIGHT, UITheme.SUCCESS_GREEN));
                if (n50 > 0)  succBreakdownPanel.add(UITheme.createStatusBadge(n50 + " × $50",   UITheme.SCREEN_CARD_LIGHT, UITheme.SUCCESS_GREEN));
                if (n20 > 0)  succBreakdownPanel.add(UITheme.createStatusBadge(n20 + " × $20",   UITheme.SCREEN_CARD_LIGHT, UITheme.SUCCESS_GREEN));
                if (n10 > 0)  succBreakdownPanel.add(UITheme.createStatusBadge(n10 + " × $10",   UITheme.SCREEN_CARD_LIGHT, UITheme.SUCCESS_GREEN));
                succBreakdownPanel.revalidate();
                succBreakdownPanel.repaint();

                setStep(ScreenState.SUCCESS);
            } else {
                setStep(ScreenState.ERROR);
            }
        });
        timer.setRepeats(false);
        timer.start();
    }

    private void printDepositReceipt() {
        AtmSession session = screenManager.getSession();
        if (session == null) return;
        List<Transaction> txs = screenManager.getAtmService().getMiniStatement(session, 1);
        if (!txs.isEmpty()) {
            new ReceiptPopup(screenManager.getFrame(), session, txs.get(0), "ATM Receipt - Cash Deposit").setVisible(true);
        }
    }

    @Override
    public void refreshScreen() {
        resetSpinners();
        setStep(ScreenState.HOPPER);
        AtmSession session = screenManager.getSession();
        if (session != null) {
            currentBalanceLabel.setText("Current Account Balance: " + UITheme.formatCurrency(session.getAccount().getBalance()));
        }
    }
}
