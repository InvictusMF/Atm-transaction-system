package com.atm.ui.screens;

import com.atm.service.AtmSession;
import com.atm.ui.ModernButton;
import com.atm.ui.ScreenManager;
import com.atm.ui.UITheme;

import javax.swing.*;
import java.awt.*;

public class MainMenuScreen extends JPanel implements ScreenManager.KeypadListener, ScreenManager.RefreshableScreen {
    private final ScreenManager screenManager;

    private JLabel welcomeLabel;
    private JLabel accountDetailsLabel;
    private JLabel liveBalanceLabel;
    private JLabel dailyLimitLabel;
    private JLabel statusBadge;

    public MainMenuScreen(ScreenManager screenManager) {
        this.screenManager = screenManager;
        setLayout(new BorderLayout(14, 14));
        setBackground(UITheme.SCREEN_BG);
        setBorder(BorderFactory.createEmptyBorder(16, 28, 16, 28));

        initComponents();
    }

    private void initComponents() {
        // Top Header + Account Overview Card
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.setBackground(UITheme.SCREEN_BG);

        // Header Title Bar
        JPanel headerTitleBar = UITheme.createScreenHeader(
                "APEX NATIONAL BANK  •  ATM DASHBOARD",
                "Select a Transaction",
                "Choose an operation below or press the corresponding number key (1-8)"
        );
        topPanel.add(headerTitleBar);
        topPanel.add(Box.createVerticalStrut(10));

        // Account Overview Card
        JPanel accountCard = UITheme.createCardPanel(14);
        accountCard.setLayout(new BorderLayout(16, 0));
        accountCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 76));

        // Left info stack
        JPanel leftInfo = new JPanel();
        leftInfo.setLayout(new BoxLayout(leftInfo, BoxLayout.Y_AXIS));
        leftInfo.setOpaque(false);

        welcomeLabel = new JLabel("Welcome, Alexander Vance");
        welcomeLabel.setFont(UITheme.FONT_TITLE);
        welcomeLabel.setForeground(UITheme.TEXT_WHITE);

        accountDetailsLabel = new JLabel("Account: ACC-1001-8842  |  Type: SAVINGS  |  Card: **** 4455");
        accountDetailsLabel.setFont(UITheme.FONT_SMALL);
        accountDetailsLabel.setForeground(UITheme.TEXT_MUTED);

        leftInfo.add(welcomeLabel);
        leftInfo.add(Box.createVerticalStrut(3));
        leftInfo.add(accountDetailsLabel);

        // Right live balance badge
        JPanel rightBalance = new JPanel();
        rightBalance.setLayout(new BoxLayout(rightBalance, BoxLayout.Y_AXIS));
        rightBalance.setOpaque(false);

        JLabel balTitle = new JLabel("AVAILABLE BALANCE", SwingConstants.RIGHT);
        balTitle.setFont(UITheme.FONT_SMALL_BOLD);
        balTitle.setForeground(UITheme.TEXT_DIM);
        balTitle.setAlignmentX(Component.RIGHT_ALIGNMENT);

        liveBalanceLabel = new JLabel("$0.00", SwingConstants.RIGHT);
        liveBalanceLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        liveBalanceLabel.setForeground(UITheme.SUCCESS_GREEN);
        liveBalanceLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);

        dailyLimitLabel = new JLabel("Daily Limit: $2,000.00", SwingConstants.RIGHT);
        dailyLimitLabel.setFont(UITheme.FONT_SMALL);
        dailyLimitLabel.setForeground(UITheme.TEXT_MUTED);
        dailyLimitLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);

        rightBalance.add(balTitle);
        rightBalance.add(liveBalanceLabel);
        rightBalance.add(dailyLimitLabel);

        accountCard.add(leftInfo, BorderLayout.WEST);
        accountCard.add(rightBalance, BorderLayout.EAST);
        topPanel.add(accountCard);

        add(topPanel, BorderLayout.NORTH);

        // Center 2x4 Action Grid
        JPanel gridPanel = new JPanel(new GridLayout(4, 2, 16, 10));
        gridPanel.setBackground(UITheme.SCREEN_BG);

        // Row 1: Fast Cash & Cash Withdrawal
        gridPanel.add(createMenuButton("1. Fast Cash", "Instant preset cash dispense ($20–$500)", UITheme.ACCENT_BLUE, e -> screenManager.showScreen("FAST_CASH")));
        gridPanel.add(createMenuButton("2. Cash Withdrawal", "Custom amount & exact note breakdown", UITheme.ACCENT_BLUE, e -> screenManager.showScreen("WITHDRAW")));

        // Row 2: Cash Deposit & Fund Transfer
        gridPanel.add(createMenuButton("3. Cash Deposit", "Accept banknote deposits into hopper", new Color(13, 148, 136), e -> screenManager.showScreen("DEPOSIT")));
        gridPanel.add(createMenuButton("4. Fund Transfer", "Transfer money to another account", new Color(217, 119, 6), e -> screenManager.showScreen("TRANSFER")));

        // Row 3: Balance Inquiry & Mini Statement
        gridPanel.add(createMenuButton("5. Balance Inquiry", "View detailed balance & account standing", new Color(79, 70, 229), e -> screenManager.showScreen("BALANCE")));
        gridPanel.add(createMenuButton("6. Mini Statement", "Recent transactions ledger & print receipt", new Color(147, 51, 234), e -> screenManager.showScreen("MINI_STATEMENT")));

        // Row 4: Change PIN & Logout
        gridPanel.add(createMenuButton("7. Change PIN", "Update your confidential 4-digit code", new Color(71, 85, 105), e -> screenManager.showScreen("PIN_CHANGE")));
        gridPanel.add(createMenuButton("8. Eject Card / Exit", "End your secure ATM banking session", UITheme.DANGER_RED, e -> exitSession()));

        add(gridPanel, BorderLayout.CENTER);

        // Bottom status & tip
        JPanel bottomBar = new JPanel(new BorderLayout());
        bottomBar.setBackground(UITheme.SCREEN_BG);

        JLabel tipLabel = new JLabel("● Touchscreen navigation active  |  Press Keypad [1–8] for instant shortcut", SwingConstants.CENTER);
        tipLabel.setFont(UITheme.FONT_SMALL);
        tipLabel.setForeground(UITheme.TEXT_MUTED);
        bottomBar.add(tipLabel, BorderLayout.CENTER);

        add(bottomBar, BorderLayout.SOUTH);
    }

    private JButton createMenuButton(String title, String subtitle, Color themeColor, java.awt.event.ActionListener action) {
        return UITheme.createCardButton(title, subtitle, themeColor, action);
    }

    private void exitSession() {
        int opt = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to end your session and eject your ATM card?",
                "CONFIRM CARD EJECT",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );
        if (opt == JOptionPane.YES_OPTION) {
            screenManager.setSession(null);
            screenManager.showScreen("WELCOME");
        }
    }

    @Override
    public void onKeyPressed(String key) {
        switch (key) {
            case "1": screenManager.showScreen("FAST_CASH"); break;
            case "2": screenManager.showScreen("WITHDRAW"); break;
            case "3": screenManager.showScreen("DEPOSIT"); break;
            case "4": screenManager.showScreen("BALANCE"); break;
            case "5": screenManager.showScreen("MINI_STATEMENT"); break;
            case "6": screenManager.showScreen("TRANSFER"); break;
            case "7": screenManager.showScreen("PIN_CHANGE"); break;
            case "8": exitSession(); break;
        }
    }

    @Override public void onClear() {}
    @Override public void onCancel() { exitSession(); }
    @Override public void onEnter() {}

    @Override
    public void refreshScreen() {
        AtmSession session = screenManager.getSession();
        if (session != null) {
            welcomeLabel.setText("Welcome, " + session.getCustomer().getFullName());
            accountDetailsLabel.setText("Account: " + session.getAccount().getAccountNumber() +
                    "  |  Type: " + session.getAccount().getAccountType() +
                    "  |  Card: " + session.getCard().getMaskedCardNumber());
            liveBalanceLabel.setText(UITheme.formatCurrency(session.getAccount().getBalance()));
            dailyLimitLabel.setText("Daily Limit: " + UITheme.formatCurrency(session.getAccount().getDailyWithdrawalLimit()));
        }
    }
}
