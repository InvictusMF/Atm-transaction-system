package com.atm.ui.screens;

import com.atm.model.Transaction;
import com.atm.service.AtmService;
import com.atm.service.AtmSession;
import com.atm.ui.ScreenManager;
import com.atm.ui.UITheme;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.util.List;

public class DepositScreen extends JPanel implements ScreenManager.RefreshableScreen {
    private final ScreenManager screenManager;

    private JSpinner spin100;
    private JSpinner spin50;
    private JSpinner spin20;
    private JSpinner spin10;
    private JLabel totalLabel;
    private JLabel currentBalanceLabel;

    public DepositScreen(ScreenManager screenManager) {
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

        JLabel title = new JLabel("CASH DEPOSIT HOPPER", SwingConstants.CENTER);
        title.setFont(UITheme.FONT_TITLE_LARGE);
        title.setForeground(UITheme.ACCENT_CYAN);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        currentBalanceLabel = new JLabel("Current Balance: $0.00", SwingConstants.CENTER);
        currentBalanceLabel.setFont(UITheme.FONT_SUBTITLE);
        currentBalanceLabel.setForeground(UITheme.TEXT_WHITE);
        currentBalanceLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        headerPanel.add(title);
        headerPanel.add(Box.createVerticalStrut(4));
        headerPanel.add(currentBalanceLabel);
        add(headerPanel, BorderLayout.NORTH);

        // Center Panel: Denominations Counter Card
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBackground(UITheme.SCREEN_BG);

        JPanel hopperCard = UITheme.createCardPanel();
        hopperCard.setLayout(new GridLayout(4, 3, 15, 10));
        hopperCard.setMaximumSize(new Dimension(520, 180));

        spin100 = createSpinner();
        spin50 = createSpinner();
        spin20 = createSpinner();
        spin10 = createSpinner();

        addHopperRow(hopperCard, "$100 Banknotes:", spin100);
        addHopperRow(hopperCard, "$50 Banknotes:",  spin50);
        addHopperRow(hopperCard, "$20 Banknotes:",  spin20);
        addHopperRow(hopperCard, "$10 Banknotes:",  spin10);

        centerPanel.add(hopperCard);
        centerPanel.add(Box.createVerticalStrut(15));

        // Total Display Box
        JPanel totalBox = new JPanel();
        totalBox.setBackground(new Color(15, 23, 42));
        totalBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.SUCCESS_GREEN, 1, true),
                BorderFactory.createEmptyBorder(10, 20, 10, 20)
        ));
        totalBox.setMaximumSize(new Dimension(420, 50));

        totalLabel = new JLabel("Total Cash Inserted: $0.00", SwingConstants.CENTER);
        totalLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        totalLabel.setForeground(UITheme.SUCCESS_GREEN);
        totalBox.add(totalLabel);

        centerPanel.add(totalBox);
        add(centerPanel, BorderLayout.CENTER);

        // Bottom Controls
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        bottomPanel.setBackground(UITheme.SCREEN_BG);

        JButton backBtn = UITheme.createModernButton("⬅ Cancel & Eject Notes", UITheme.CHASSIS_BG, UITheme.TEXT_WHITE);
        backBtn.addActionListener(e -> screenManager.showScreen("MAIN_MENU"));

        JButton resetBtn = UITheme.createModernButton("↺ Reset Counts", UITheme.WARNING_YELLOW, Color.BLACK);
        resetBtn.addActionListener(e -> resetSpinners());

        JButton confirmBtn = UITheme.createModernButton("✔ Accept & Deposit Cash", UITheme.SUCCESS_GREEN, Color.BLACK);
        confirmBtn.addActionListener(e -> executeDeposit());

        bottomPanel.add(backBtn);
        bottomPanel.add(resetBtn);
        bottomPanel.add(confirmBtn);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    private JSpinner createSpinner() {
        SpinnerNumberModel model = new SpinnerNumberModel(0, 0, 100, 1);
        JSpinner spinner = new JSpinner(model);
        spinner.setFont(UITheme.FONT_SUBTITLE);
        spinner.addChangeListener(e -> updateTotal());
        return spinner;
    }

    private void addHopperRow(JPanel parent, String labelText, JSpinner spinner) {
        JLabel lbl = new JLabel(labelText);
        lbl.setFont(UITheme.FONT_BODY_BOLD);
        lbl.setForeground(UITheme.TEXT_WHITE);

        JLabel desc = new JLabel("Accepted by slot");
        desc.setFont(UITheme.FONT_MONO);
        desc.setForeground(UITheme.TEXT_MUTED);

        parent.add(lbl);
        parent.add(spinner);
        parent.add(desc);
    }

    private void updateTotal() {
        int n100 = (Integer) spin100.getValue();
        int n50  = (Integer) spin50.getValue();
        int n20  = (Integer) spin20.getValue();
        int n10  = (Integer) spin10.getValue();

        int total = (n100 * 100) + (n50 * 50) + (n20 * 20) + (n10 * 10);
        totalLabel.setText("Total Cash Inserted: $" + String.format("%.2f", (double) total));
    }

    private void resetSpinners() {
        spin100.setValue(0);
        spin50.setValue(0);
        spin20.setValue(0);
        spin10.setValue(0);
        updateTotal();
    }

    private void executeDeposit() {
        AtmSession session = screenManager.getSession();
        if (session == null) return;

        int n100 = (Integer) spin100.getValue();
        int n50  = (Integer) spin50.getValue();
        int n20  = (Integer) spin20.getValue();
        int n10  = (Integer) spin10.getValue();

        int total = (n100 * 100) + (n50 * 50) + (n20 * 20) + (n10 * 10);
        if (total <= 0) {
            JOptionPane.showMessageDialog(this, "Please insert at least one banknote into the deposit hopper.", "NO CASH DETECTED", JOptionPane.WARNING_MESSAGE);
            return;
        }

        AtmService.OperationResult<BigDecimal> result = screenManager.getAtmService().deposit(session, n100, n50, n20, n10);
        if (result.isSuccess()) {
            int printReceipt = JOptionPane.showConfirmDialog(
                    this,
                    "Cash Deposit Successful!\n\n" +
                            "• Amount Deposited: $" + String.format("%.2f", (double) total) + "\n" +
                            "• Updated Account Balance: $" + String.format("%.2f", result.getData()) + "\n\n" +
                            "Would you like an official deposit receipt?",
                    "DEPOSIT CONFIRMED",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.INFORMATION_MESSAGE
            );

            if (printReceipt == JOptionPane.YES_OPTION) {
                List<Transaction> txs = screenManager.getAtmService().getMiniStatement(session, 1);
                if (!txs.isEmpty()) {
                    new ReceiptPopup(screenManager.getFrame(), session, txs.get(0), "ATM Receipt - Deposit").setVisible(true);
                }
            }

            resetSpinners();
            screenManager.showScreen("MAIN_MENU");
        } else {
            JOptionPane.showMessageDialog(this, result.getMessage(), "DEPOSIT FAILED", JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    public void refreshScreen() {
        resetSpinners();
        AtmSession session = screenManager.getSession();
        if (session != null) {
            currentBalanceLabel.setText("Current Account Balance: $" + String.format("%.2f", session.getAccount().getBalance()));
        }
    }
}
