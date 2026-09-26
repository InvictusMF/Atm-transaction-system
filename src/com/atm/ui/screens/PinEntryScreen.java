package com.atm.ui.screens;

import com.atm.model.AtmCard;
import com.atm.service.AtmService;
import com.atm.ui.ScreenManager;
import com.atm.ui.UITheme;

import javax.swing.*;
import java.awt.*;

public class PinEntryScreen extends JPanel implements ScreenManager.KeypadListener, ScreenManager.RefreshableScreen {
    private final ScreenManager screenManager;
    private AtmCard targetCard;
    private final StringBuilder enteredPin = new StringBuilder();

    private JLabel cardHolderLabel;
    private JLabel cardNumberLabel;
    private JLabel pinDotsLabel;
    private JLabel feedbackLabel;
    private JLabel attemptWarningLabel;

    public PinEntryScreen(ScreenManager screenManager) {
        this.screenManager = screenManager;
        setLayout(new BorderLayout(20, 20));
        setBackground(UITheme.SCREEN_BG);
        setBorder(BorderFactory.createEmptyBorder(25, 40, 25, 40));

        initComponents();
    }

    public void setTargetCard(AtmCard card) {
        this.targetCard = card;
        refreshScreen();
    }

    private void initComponents() {
        // Header
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setBackground(UITheme.SCREEN_BG);

        JLabel title = new JLabel("SECURITY AUTHENTICATION", SwingConstants.CENTER);
        title.setFont(UITheme.FONT_TITLE_LARGE);
        title.setForeground(UITheme.ACCENT_CYAN);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("Please enter your 4-digit secret PIN code", SwingConstants.CENTER);
        subtitle.setFont(UITheme.FONT_SUBTITLE);
        subtitle.setForeground(UITheme.TEXT_WHITE);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        headerPanel.add(title);
        headerPanel.add(Box.createVerticalStrut(4));
        headerPanel.add(subtitle);
        add(headerPanel, BorderLayout.NORTH);

        // Center Panel: Card info & PIN Display
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBackground(UITheme.SCREEN_BG);

        JPanel infoCard = UITheme.createCardPanel();
        infoCard.setLayout(new BoxLayout(infoCard, BoxLayout.Y_AXIS));
        infoCard.setMaximumSize(new Dimension(500, 160));

        cardHolderLabel = new JLabel("Cardholder: --");
        cardHolderLabel.setFont(UITheme.FONT_BODY_BOLD);
        cardHolderLabel.setForeground(UITheme.TEXT_WHITE);
        cardHolderLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        cardNumberLabel = new JLabel("Card: •••• •••• •••• ••••");
        cardNumberLabel.setFont(UITheme.FONT_MONO);
        cardNumberLabel.setForeground(UITheme.TEXT_MUTED);
        cardNumberLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        attemptWarningLabel = new JLabel("Security Warning: 3 invalid attempts will lock the card.", SwingConstants.CENTER);
        attemptWarningLabel.setFont(UITheme.FONT_BODY);
        attemptWarningLabel.setForeground(UITheme.ACCENT_GOLD);
        attemptWarningLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        infoCard.add(cardHolderLabel);
        infoCard.add(Box.createVerticalStrut(4));
        infoCard.add(cardNumberLabel);
        infoCard.add(Box.createVerticalStrut(10));
        infoCard.add(attemptWarningLabel);

        centerPanel.add(infoCard);
        centerPanel.add(Box.createVerticalStrut(20));

        // PIN Masked Display Box
        JPanel pinDisplayBox = new JPanel();
        pinDisplayBox.setBackground(new Color(15, 23, 42));
        pinDisplayBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.ACCENT_CYAN, 2, true),
                BorderFactory.createEmptyBorder(12, 30, 12, 30)
        ));
        pinDisplayBox.setMaximumSize(new Dimension(320, 65));

        pinDotsLabel = new JLabel("○   ○   ○   ○", SwingConstants.CENTER);
        pinDotsLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        pinDotsLabel.setForeground(UITheme.ACCENT_CYAN);
        pinDisplayBox.add(pinDotsLabel);

        centerPanel.add(pinDisplayBox);
        centerPanel.add(Box.createVerticalStrut(15));

        // Feedback Label
        feedbackLabel = new JLabel("Use on-screen keypad or physical ATM PIN pad below", SwingConstants.CENTER);
        feedbackLabel.setFont(UITheme.FONT_BODY_BOLD);
        feedbackLabel.setForeground(UITheme.TEXT_MUTED);
        feedbackLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        centerPanel.add(feedbackLabel);

        add(centerPanel, BorderLayout.CENTER);

        // Bottom Action Row
        JPanel bottomRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        bottomRow.setBackground(UITheme.SCREEN_BG);

        JButton cancelBtn = UITheme.createModernButton("✖ Cancel & Eject Card", UITheme.DANGER_RED, UITheme.TEXT_WHITE);
        cancelBtn.addActionListener(e -> onCancel());

        JButton clearBtn = UITheme.createModernButton("⌫ Clear PIN", UITheme.WARNING_YELLOW, Color.BLACK);
        clearBtn.addActionListener(e -> onClear());

        JButton submitBtn = UITheme.createModernButton("✔ Submit PIN", UITheme.SUCCESS_GREEN, Color.BLACK);
        submitBtn.addActionListener(e -> onEnter());

        bottomRow.add(cancelBtn);
        bottomRow.add(clearBtn);
        bottomRow.add(submitBtn);

        add(bottomRow, BorderLayout.SOUTH);
    }

    private void updatePinDisplay() {
        StringBuilder sb = new StringBuilder();
        int len = enteredPin.length();
        for (int i = 0; i < 4; i++) {
            if (i < len) {
                sb.append("●");
            } else {
                sb.append("○");
            }
            if (i < 3) sb.append("   ");
        }
        pinDotsLabel.setText(sb.toString());
    }

    @Override
    public void onKeyPressed(String key) {
        if (key.matches("\\d") && enteredPin.length() < 4) {
            enteredPin.append(key);
            updatePinDisplay();
            if (enteredPin.length() == 4) {
                // Auto submit or highlight enter
                feedbackLabel.setText("Press Enter or Submit to proceed");
                feedbackLabel.setForeground(UITheme.ACCENT_CYAN);
            }
        }
    }

    @Override
    public void onClear() {
        enteredPin.setLength(0);
        updatePinDisplay();
        feedbackLabel.setText("PIN Cleared. Please re-enter.");
        feedbackLabel.setForeground(UITheme.TEXT_MUTED);
    }

    @Override
    public void onCancel() {
        enteredPin.setLength(0);
        updatePinDisplay();
        targetCard = null;
        screenManager.setSession(null);
        screenManager.showScreen("WELCOME");
    }

    @Override
    public void onEnter() {
        if (targetCard == null) return;
        if (enteredPin.length() != 4) {
            feedbackLabel.setText("⚠ Please enter all 4 digits of your PIN.");
            feedbackLabel.setForeground(UITheme.WARNING_YELLOW);
            return;
        }

        AtmService.AuthResult result = screenManager.getAtmService().authenticate(targetCard.getCardNumber(), enteredPin.toString());
        if (result.isSuccess()) {
            screenManager.setSession(result.getSession());
            screenManager.showScreen("MAIN_MENU");
        } else {
            enteredPin.setLength(0);
            updatePinDisplay();
            feedbackLabel.setText(result.getMessage());
            feedbackLabel.setForeground(UITheme.DANGER_RED);

            if (result.isCardBlocked()) {
                JOptionPane.showMessageDialog(this,
                        result.getMessage(),
                        "CARD RETAINED / BLOCKED",
                        JOptionPane.ERROR_MESSAGE);
                onCancel();
            }
        }
    }

    @Override
    public void refreshScreen() {
        enteredPin.setLength(0);
        updatePinDisplay();
        if (targetCard != null) {
            cardHolderLabel.setText("Cardholder: " + targetCard.getCardHolderName());
            cardNumberLabel.setText("Card Number: " + targetCard.getMaskedCardNumber() + " (" + targetCard.getCardStatus() + ")");
            int fails = targetCard.getFailedPinAttempts();
            if (fails > 0) {
                attemptWarningLabel.setText("⚠ Failed Attempts: " + fails + " / 3 before permanent lockout!");
                attemptWarningLabel.setForeground(UITheme.DANGER_RED);
            } else {
                attemptWarningLabel.setText("Security: Max 3 invalid attempts before security lockout.");
                attemptWarningLabel.setForeground(UITheme.ACCENT_GOLD);
            }
        }
        feedbackLabel.setText("Use on-screen keypad or physical ATM PIN pad below");
        feedbackLabel.setForeground(UITheme.TEXT_MUTED);
    }
}
