package com.atm.ui.screens;

import com.atm.service.AtmSession;
import com.atm.ui.ScreenManager;
import com.atm.ui.UITheme;

import javax.swing.*;
import java.awt.*;

public class MainMenuScreen extends JPanel implements ScreenManager.KeypadListener, ScreenManager.RefreshableScreen {
    private final ScreenManager screenManager;

    private JLabel welcomeLabel;
    private JLabel accountDetailsLabel;

    public MainMenuScreen(ScreenManager screenManager) {
        this.screenManager = screenManager;
        setLayout(new BorderLayout(15, 15));
        setBackground(UITheme.SCREEN_BG);
        setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        initComponents();
    }

    private void initComponents() {
        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(UITheme.SCREEN_BG);

        JPanel userPanel = new JPanel();
        userPanel.setLayout(new BoxLayout(userPanel, BoxLayout.Y_AXIS));
        userPanel.setBackground(UITheme.SCREEN_BG);

        welcomeLabel = new JLabel("Welcome, Customer");
        welcomeLabel.setFont(UITheme.FONT_TITLE_LARGE);
        welcomeLabel.setForeground(UITheme.ACCENT_CYAN);

        accountDetailsLabel = new JLabel("Account: -- | Type: --");
        accountDetailsLabel.setFont(UITheme.FONT_SUBTITLE);
        accountDetailsLabel.setForeground(UITheme.TEXT_MUTED);

        userPanel.add(welcomeLabel);
        userPanel.add(Box.createVerticalStrut(4));
        userPanel.add(accountDetailsLabel);

        headerPanel.add(userPanel, BorderLayout.WEST);

        JLabel selectPrompt = new JLabel("Select Transaction [1 - 8]");
        selectPrompt.setFont(UITheme.FONT_BODY_BOLD);
        selectPrompt.setForeground(UITheme.ACCENT_GOLD);
        headerPanel.add(selectPrompt, BorderLayout.EAST);

        add(headerPanel, BorderLayout.NORTH);

        // Center 2x4 Menu Grid
        JPanel gridPanel = new JPanel(new GridLayout(4, 2, 16, 12));
        gridPanel.setBackground(UITheme.SCREEN_BG);

        gridPanel.add(createMenuButton("1. Fast Cash", "Quick preset cash dispensing", UITheme.ACCENT_BLUE, e -> screenManager.showScreen("FAST_CASH")));
        gridPanel.add(createMenuButton("2. Cash Withdrawal", "Custom amount & bill breakdown", UITheme.ACCENT_BLUE, e -> screenManager.showScreen("WITHDRAW")));
        gridPanel.add(createMenuButton("3. Cash Deposit", "Accept currency notes into hopper", new Color(13, 148, 136), e -> screenManager.showScreen("DEPOSIT")));
        gridPanel.add(createMenuButton("4. Balance Inquiry", "View real-time account balances", new Color(79, 70, 229), e -> screenManager.showScreen("BALANCE")));
        gridPanel.add(createMenuButton("5. Mini Statement", "Recent transactions & print receipt", new Color(147, 51, 234), e -> screenManager.showScreen("MINI_STATEMENT")));
        gridPanel.add(createMenuButton("6. Fund Transfer", "Transfer funds to another account", new Color(217, 119, 6), e -> screenManager.showScreen("TRANSFER")));
        gridPanel.add(createMenuButton("7. Change PIN", "Update your secret 4-digit code", new Color(71, 85, 105), e -> screenManager.showScreen("PIN_CHANGE")));
        gridPanel.add(createMenuButton("8. Eject Card / Exit", "End your secure ATM session", UITheme.DANGER_RED, e -> exitSession()));

        add(gridPanel, BorderLayout.CENTER);

        // Bottom status tip
        JLabel tipLabel = new JLabel("● Tip: Use your touch screen or press the corresponding number key (1-8) on the keypad", SwingConstants.CENTER);
        tipLabel.setFont(UITheme.FONT_BODY);
        tipLabel.setForeground(UITheme.TEXT_MUTED);
        add(tipLabel, BorderLayout.SOUTH);
    }

    private JButton createMenuButton(String title, String subtitle, Color themeColor, java.awt.event.ActionListener action) {
        return UITheme.createCardButton(title, subtitle, themeColor, action);
    }

    private void exitSession() {
        int opt = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to end your session and eject your card?",
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
                    "  |  Currency: " + session.getAccount().getCurrency());
        }
    }
}
