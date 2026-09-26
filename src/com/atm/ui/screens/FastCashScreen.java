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

public class FastCashScreen extends JPanel implements ScreenManager.KeypadListener, ScreenManager.RefreshableScreen {
    private enum ScreenState {
        SELECTION,
        CONFIRMATION,
        PROCESSING,
        SUCCESS,
        ERROR
    }

    private final ScreenManager screenManager;
    private int selectedAmount = 0;
    private ScreenState currentState = ScreenState.SELECTION;

    private final CardLayout stepLayout = new CardLayout();
    private final JPanel stepContainer = new JPanel(stepLayout);

    // Step 1: Selection
    private JLabel balanceLabel;

    // Step 2: Confirmation
    private JLabel confAmountLabel;
    private JLabel confBalanceLabel;
    private JLabel confRemainingLabel;

    // Step 4: Success
    private JLabel succAmountLabel;
    private JLabel succBalanceLabel;
    private JPanel succNotesPanel;

    // Step 5: Error
    private JLabel errMessageLabel;

    public FastCashScreen(ScreenManager screenManager) {
        this.screenManager = screenManager;
        setLayout(new BorderLayout());
        setBackground(UITheme.SCREEN_BG);

        initAllSteps();
    }

    private void initAllSteps() {
        stepContainer.setBackground(UITheme.SCREEN_BG);
        stepContainer.add(createSelectionStep(), ScreenState.SELECTION.name());
        stepContainer.add(createConfirmationStep(), ScreenState.CONFIRMATION.name());
        stepContainer.add(createProcessingStep(), ScreenState.PROCESSING.name());
        stepContainer.add(createSuccessStep(), ScreenState.SUCCESS.name());
        stepContainer.add(createErrorStep(), ScreenState.ERROR.name());

        add(stepContainer, BorderLayout.CENTER);
    }

    private JPanel createSelectionStep() {
        JPanel panel = new JPanel(new BorderLayout(14, 14));
        panel.setBackground(UITheme.SCREEN_BG);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 35, 20, 35));

        JPanel header = UITheme.createScreenHeader(
                "FAST CASH DISPENSING  •  EXPRESS CHECKOUT",
                "Select Fast Cash Amount",
                "Instant one-touch cash dispensing presets"
        );
        panel.add(header, BorderLayout.NORTH);

        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);

        balanceLabel = new JLabel("Available Balance: $0.00", SwingConstants.CENTER);
        balanceLabel.setFont(UITheme.FONT_SUBTITLE);
        balanceLabel.setForeground(UITheme.SUCCESS_GREEN);
        balanceLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        center.add(balanceLabel);
        center.add(Box.createVerticalStrut(14));

        // 2x3 Grid of Preset Buttons
        JPanel gridPanel = new JPanel(new GridLayout(3, 2, 16, 12));
        gridPanel.setOpaque(false);
        gridPanel.setMaximumSize(new Dimension(540, 220));

        int[] presets = {20, 40, 60, 100, 200, 500};
        String[] descriptions = {"1 × $20 bill", "2 × $20 bills", "1 × $50 + 1 × $10", "1 × $100 bill", "2 × $100 bills", "5 × $100 bills"};

        for (int i = 0; i < presets.length; i++) {
            int amt = presets[i];
            String desc = descriptions[i];
            JButton btn = UITheme.createCardButton("$" + amt, desc, UITheme.ACCENT_BLUE, e -> initiateFastCash(amt));
            gridPanel.add(btn);
        }
        center.add(gridPanel);

        panel.add(center, BorderLayout.CENTER);

        // Bottom
        JPanel bottomNav = new JPanel(new BorderLayout());
        bottomNav.setOpaque(false);

        JLabel tip = new JLabel("● Touch an amount or press Keypad [1–6] to select", SwingConstants.CENTER);
        tip.setFont(UITheme.FONT_SMALL);
        tip.setForeground(UITheme.TEXT_MUTED);
        bottomNav.add(tip, BorderLayout.CENTER);

        JButton backBtn = UITheme.createModernButton("⬅ Back to Menu", UITheme.CHASSIS_BG, UITheme.TEXT_WHITE);
        backBtn.setPreferredSize(new Dimension(160, 40));
        backBtn.addActionListener(e -> onCancel());
        bottomNav.add(backBtn, BorderLayout.EAST);

        panel.add(bottomNav, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createConfirmationStep() {
        JPanel panel = new JPanel(new BorderLayout(14, 14));
        panel.setBackground(UITheme.SCREEN_BG);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 35, 20, 35));

        JPanel header = UITheme.createScreenHeader(
                "FAST CASH  •  CONFIRM DISPENSE",
                "Authorize Fast Cash Dispense",
                "Please confirm the instant withdrawal amount"
        );
        panel.add(header, BorderLayout.NORTH);

        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);

        JPanel summaryCard = UITheme.createCardPanel(20);
        summaryCard.setLayout(new BoxLayout(summaryCard, BoxLayout.Y_AXIS));
        summaryCard.setMaximumSize(new Dimension(500, 200));

        confAmountLabel = new JLabel("$0.00");
        confAmountLabel.setFont(new Font("Segoe UI", Font.BOLD, 26));
        confAmountLabel.setForeground(UITheme.ACCENT_CYAN);

        confBalanceLabel = new JLabel("$0.00");
        confBalanceLabel.setFont(UITheme.FONT_BODY_BOLD);
        confBalanceLabel.setForeground(UITheme.TEXT_WHITE);

        confRemainingLabel = new JLabel("$0.00");
        confRemainingLabel.setFont(UITheme.FONT_TITLE);
        confRemainingLabel.setForeground(UITheme.SUCCESS_GREEN);

        summaryCard.add(createRow("Fast Cash Dispense Amount:", confAmountLabel));
        summaryCard.add(Box.createVerticalStrut(10));
        summaryCard.add(createRow("Available Account Balance:", confBalanceLabel));
        summaryCard.add(Box.createVerticalStrut(10));
        JSeparator sep = new JSeparator();
        sep.setForeground(UITheme.CARD_BORDER);
        summaryCard.add(sep);
        summaryCard.add(Box.createVerticalStrut(10));
        summaryCard.add(createRow("Projected Remaining Balance:", confRemainingLabel));

        center.add(summaryCard);
        panel.add(center, BorderLayout.CENTER);

        // Bottom Actions
        JPanel bottomNav = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        bottomNav.setOpaque(false);

        JButton backBtn = UITheme.createModernButton("⬅ Cancel / Change", UITheme.CHASSIS_BG, UITheme.TEXT_WHITE);
        backBtn.setPreferredSize(new Dimension(170, 42));
        backBtn.addActionListener(e -> setStep(ScreenState.SELECTION));

        JButton confirmBtn = UITheme.createModernButton("✔ Dispense Now", UITheme.SUCCESS_GREEN, Color.BLACK);
        confirmBtn.setPreferredSize(new Dimension(180, 42));
        confirmBtn.addActionListener(e -> executeFastCash());

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

        JLabel title = new JLabel("Counting Banknotes...", SwingConstants.CENTER);
        title.setFont(UITheme.FONT_TITLE_LARGE);
        title.setForeground(UITheme.ACCENT_CYAN);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("Please collect your cash from the dispenser slot momentarily", SwingConstants.CENTER);
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
                "FAST CASH  •  DISPENSE COMPLETE",
                "Cash Dispensed Successfully",
                "Please collect your banknotes and card from the kiosk slots"
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

        successCard.add(createRow("Total Dispensed:", succAmountLabel));
        successCard.add(Box.createVerticalStrut(8));
        successCard.add(createRow("Updated Available Balance:", succBalanceLabel));
        successCard.add(Box.createVerticalStrut(10));

        JLabel notesTitle = new JLabel("Dispensed Banknotes:");
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

        JButton receiptBtn = UITheme.createModernButton("🖨 Print Receipt", UITheme.ACCENT_BLUE, UITheme.TEXT_WHITE);
        receiptBtn.setPreferredSize(new Dimension(170, 42));
        receiptBtn.addActionListener(e -> printReceipt());

        JButton menuBtn = UITheme.createModernButton("⬅ Return to Dashboard", UITheme.CHASSIS_BG, UITheme.TEXT_WHITE);
        menuBtn.setPreferredSize(new Dimension(190, 42));
        menuBtn.addActionListener(e -> screenManager.showScreen("MAIN_MENU"));

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
                "FAST CASH  •  DECLINED",
                "Transaction Could Not Be Completed",
                "The requested Fast Cash amount was not dispensed"
        );
        panel.add(header, BorderLayout.NORTH);

        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);

        JPanel errCard = UITheme.createCardPanel(24);
        errCard.setLayout(new BoxLayout(errCard, BoxLayout.Y_AXIS));
        errCard.setMaximumSize(new Dimension(500, 160));

        JLabel errTitle = new JLabel("Insufficient Funds or Limit Exceeded", SwingConstants.CENTER);
        errTitle.setFont(UITheme.FONT_TITLE);
        errTitle.setForeground(UITheme.DANGER_RED);
        errTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        errMessageLabel = new JLabel("Please review your balance and try another amount.", SwingConstants.CENTER);
        errMessageLabel.setFont(UITheme.FONT_BODY);
        errMessageLabel.setForeground(UITheme.TEXT_MUTED);
        errMessageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        errCard.add(errTitle);
        errCard.add(Box.createVerticalStrut(8));
        errCard.add(errMessageLabel);

        center.add(errCard);
        panel.add(center, BorderLayout.CENTER);

        // Bottom Actions
        JPanel bottomNav = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        bottomNav.setOpaque(false);

        JButton retryBtn = UITheme.createModernButton("↺ Choose Different Amount", UITheme.ACCENT_BLUE, UITheme.TEXT_WHITE);
        retryBtn.setPreferredSize(new Dimension(220, 42));
        retryBtn.addActionListener(e -> setStep(ScreenState.SELECTION));

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

    private void initiateFastCash(int amount) {
        AtmSession session = screenManager.getSession();
        if (session == null) return;

        this.selectedAmount = amount;
        BigDecimal amt = BigDecimal.valueOf(amount);

        if (amt.compareTo(session.getAccount().getBalance()) > 0) {
            errMessageLabel.setText("<html><center>Available Balance: " + UITheme.formatCurrency(session.getAccount().getBalance()) +
                    "<br>Requested: " + UITheme.formatCurrency(amt) + "</center></html>");
            setStep(ScreenState.ERROR);
            return;
        }

        confAmountLabel.setText(UITheme.formatCurrency(amt));
        confBalanceLabel.setText(UITheme.formatCurrency(session.getAccount().getBalance()));
        BigDecimal rem = session.getAccount().getBalance().subtract(amt);
        confRemainingLabel.setText(UITheme.formatCurrency(rem));

        setStep(ScreenState.CONFIRMATION);
    }

    private void executeFastCash() {
        AtmSession session = screenManager.getSession();
        if (session == null) return;

        setStep(ScreenState.PROCESSING);

        Timer timer = new Timer(500, e -> {
            ((Timer) e.getSource()).stop();
            AtmService.OperationResult<Map<Integer, Integer>> result = screenManager.getAtmService().withdraw(session, selectedAmount);
            if (result.isSuccess()) {
                succAmountLabel.setText(UITheme.formatCurrency(BigDecimal.valueOf(selectedAmount)));
                succBalanceLabel.setText(UITheme.formatCurrency(session.getAccount().getBalance()));

                succNotesPanel.removeAll();
                if (result.getData() != null) {
                    result.getData().forEach((denom, count) -> {
                        JLabel badge = UITheme.createStatusBadge(count + " × $" + denom + " Note(s)", UITheme.SCREEN_CARD_LIGHT, UITheme.ACCENT_CYAN);
                        succNotesPanel.add(badge);
                    });
                }
                succNotesPanel.revalidate();
                succNotesPanel.repaint();

                setStep(ScreenState.SUCCESS);
            } else {
                errMessageLabel.setText("<html><center>" + result.getMessage() + "</center></html>");
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
            new ReceiptPopup(screenManager.getFrame(), session, txs.get(0), "ATM Receipt - Fast Cash").setVisible(true);
        }
    }

    @Override
    public void onKeyPressed(String key) {
        if (currentState == ScreenState.SELECTION) {
            switch (key) {
                case "1": initiateFastCash(20); break;
                case "2": initiateFastCash(40); break;
                case "3": initiateFastCash(60); break;
                case "4": initiateFastCash(100); break;
                case "5": initiateFastCash(200); break;
                case "6": initiateFastCash(500); break;
            }
        }
    }

    @Override public void onClear() {}
    @Override public void onCancel() {
        if (currentState == ScreenState.CONFIRMATION) {
            setStep(ScreenState.SELECTION);
        } else {
            setStep(ScreenState.SELECTION);
            screenManager.showScreen("MAIN_MENU");
        }
    }
    @Override public void onEnter() {
        if (currentState == ScreenState.CONFIRMATION) {
            executeFastCash();
        }
    }

    @Override
    public void refreshScreen() {
        selectedAmount = 0;
        setStep(ScreenState.SELECTION);
        AtmSession session = screenManager.getSession();
        if (session != null) {
            balanceLabel.setText("Available Balance: " + UITheme.formatCurrency(session.getAccount().getBalance()));
        }
    }
}
