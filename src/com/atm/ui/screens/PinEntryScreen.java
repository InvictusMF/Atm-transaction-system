package com.atm.ui.screens;

import com.atm.model.AtmCard;
import com.atm.service.AtmService;
import com.atm.ui.ModernButton;
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
    private JLabel cardStatusBadge;
    private JLabel attemptWarningLabel;
    private final JPanel[] pinBoxes = new JPanel[4];
    private final JLabel[] pinDots = new JLabel[4];
    private JLabel feedbackLabel;
    private JButton submitBtn;

    public PinEntryScreen(ScreenManager screenManager) {
        this.screenManager = screenManager;
        setLayout(new BorderLayout(16, 16));
        setBackground(UITheme.SCREEN_BG);
        setBorder(BorderFactory.createEmptyBorder(20, 35, 20, 35));

        initComponents();
    }

    public void setTargetCard(AtmCard card) {
        this.targetCard = card;
        refreshScreen();
    }

    private void initComponents() {
        // Header
        JPanel headerPanel = UITheme.createScreenHeader(
                "SECURITY VERIFICATION  •  STEP 2 OF 2",
                "Enter Your 4-Digit PIN",
                "Shield the keypad while entering your confidential access code"
        );
        add(headerPanel, BorderLayout.NORTH);

        // Center Content
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBackground(UITheme.SCREEN_BG);

        // Cardholder Information Pill Card
        JPanel infoCard = UITheme.createCardPanel(14);
        infoCard.setLayout(new BorderLayout(12, 0));
        infoCard.setMaximumSize(new Dimension(540, 68));

        JPanel textStack = new JPanel();
        textStack.setLayout(new BoxLayout(textStack, BoxLayout.Y_AXIS));
        textStack.setOpaque(false);

        cardHolderLabel = new JLabel("Cardholder: --");
        cardHolderLabel.setFont(UITheme.FONT_BODY_BOLD);
        cardHolderLabel.setForeground(UITheme.TEXT_WHITE);

        cardNumberLabel = new JLabel("Card: •••• •••• •••• ••••");
        cardNumberLabel.setFont(UITheme.FONT_MONO);
        cardNumberLabel.setForeground(UITheme.TEXT_MUTED);

        textStack.add(cardHolderLabel);
        textStack.add(Box.createVerticalStrut(3));
        textStack.add(cardNumberLabel);

        cardStatusBadge = UITheme.createStatusBadge("ACTIVE", new Color(16, 185, 129, 40), UITheme.SUCCESS_GREEN);

        infoCard.add(textStack, BorderLayout.WEST);
        infoCard.add(cardStatusBadge, BorderLayout.EAST);
        centerPanel.add(infoCard);
        centerPanel.add(Box.createVerticalStrut(14));

        // PIN 4-Box Visual Container
        JPanel pinRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 0));
        pinRow.setOpaque(false);

        for (int i = 0; i < 4; i++) {
            pinBoxes[i] = new JPanel(new GridBagLayout());
            pinBoxes[i].setPreferredSize(new Dimension(58, 62));
            pinBoxes[i].setBackground(new Color(15, 23, 42));
            pinBoxes[i].setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(UITheme.CARD_BORDER, 2, true),
                    BorderFactory.createEmptyBorder(4, 4, 4, 4)
            ));

            pinDots[i] = new JLabel("○");
            pinDots[i].setFont(new Font("Segoe UI", Font.BOLD, 26));
            pinDots[i].setForeground(UITheme.TEXT_DIM);
            pinBoxes[i].add(pinDots[i]);
            pinRow.add(pinBoxes[i]);
        }
        centerPanel.add(pinRow);
        centerPanel.add(Box.createVerticalStrut(14));

        // Feedback & Security Warning
        attemptWarningLabel = new JLabel("Security Warning: 3 invalid attempts will lock the card.", SwingConstants.CENTER);
        attemptWarningLabel.setFont(UITheme.FONT_SMALL_BOLD);
        attemptWarningLabel.setForeground(UITheme.ACCENT_GOLD);
        attemptWarningLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        centerPanel.add(attemptWarningLabel);
        centerPanel.add(Box.createVerticalStrut(6));

        feedbackLabel = new JLabel("Enter 4 digits using the physical ATM PIN pad or on-screen keys", SwingConstants.CENTER);
        feedbackLabel.setFont(UITheme.FONT_BODY);
        feedbackLabel.setForeground(UITheme.TEXT_MUTED);
        feedbackLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        centerPanel.add(feedbackLabel);

        add(centerPanel, BorderLayout.CENTER);

        // Bottom Actions
        JPanel bottomRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        bottomRow.setBackground(UITheme.SCREEN_BG);

        JButton cancelBtn = UITheme.createModernButton("⬅ Cancel & Eject Card", UITheme.DANGER_RED, UITheme.TEXT_WHITE);
        cancelBtn.setPreferredSize(new Dimension(190, 42));
        cancelBtn.addActionListener(e -> onCancel());

        JButton clearBtn = UITheme.createModernButton("⌫ Clear PIN", UITheme.WARNING_YELLOW, new Color(15, 23, 42));
        clearBtn.setPreferredSize(new Dimension(140, 42));
        clearBtn.addActionListener(e -> onClear());

        submitBtn = UITheme.createModernButton("✔ Submit PIN", UITheme.SUCCESS_GREEN, Color.BLACK);
        submitBtn.setPreferredSize(new Dimension(150, 42));
        submitBtn.addActionListener(e -> onEnter());

        bottomRow.add(cancelBtn);
        bottomRow.add(clearBtn);
        bottomRow.add(submitBtn);

        add(bottomRow, BorderLayout.SOUTH);
    }

    private void updatePinDisplay() {
        int len = enteredPin.length();
        for (int i = 0; i < 4; i++) {
            if (i < len) {
                pinDots[i].setText("●");
                pinDots[i].setForeground(UITheme.ACCENT_CYAN);
                pinBoxes[i].setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(UITheme.ACCENT_CYAN, 2, true),
                        BorderFactory.createEmptyBorder(4, 4, 4, 4)
                ));
            } else {
                pinDots[i].setText("○");
                pinDots[i].setForeground(UITheme.TEXT_DIM);
                pinBoxes[i].setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(UITheme.CARD_BORDER, 2, true),
                        BorderFactory.createEmptyBorder(4, 4, 4, 4)
                ));
            }
        }
    }

    @Override
    public void onKeyPressed(String key) {
        if (key.matches("\\d") && enteredPin.length() < 4) {
            enteredPin.append(key);
            updatePinDisplay();
            if (enteredPin.length() == 4) {
                feedbackLabel.setText("4 Digits entered. Press Submit or hit ENTER on keypad.");
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
            feedbackLabel.setText("⚠ Please enter all 4 digits of your PIN code.");
            feedbackLabel.setForeground(UITheme.WARNING_YELLOW);
            return;
        }

        // Show verifying feedback
        feedbackLabel.setText("Verifying encrypted PIN with card issuer...");
        feedbackLabel.setForeground(UITheme.ACCENT_CYAN);

        AtmService.AuthResult result = screenManager.getAtmService().authenticate(targetCard.getCardNumber(), enteredPin.toString());
        if (result.isSuccess()) {
            screenManager.setSession(result.getSession());
            screenManager.showScreen("MAIN_MENU");
        } else {
            enteredPin.setLength(0);
            updatePinDisplay();
            feedbackLabel.setText("✖ " + result.getMessage());
            feedbackLabel.setForeground(UITheme.DANGER_RED);

            if (result.isCardBlocked()) {
                cardStatusBadge.setText(" LOCKED ");
                cardStatusBadge.setForeground(UITheme.DANGER_RED);
                attemptWarningLabel.setText("CARD BLOCKED: Security lockout triggered after 3 failed attempts.");
                attemptWarningLabel.setForeground(UITheme.DANGER_RED);
                JOptionPane.showMessageDialog(this,
                        result.getMessage() + "\n\nFor security reasons, this card has been retained by the system.",
                        "CARD BLOCKED / LOCKED",
                        JOptionPane.ERROR_MESSAGE);
                onCancel();
            } else {
                int fails = targetCard.getFailedPinAttempts();
                int remaining = Math.max(0, 3 - fails);
                attemptWarningLabel.setText("⚠ Security Alert: " + remaining + " invalid attempt(s) remaining before card lockout!");
                attemptWarningLabel.setForeground(UITheme.DANGER_RED);
            }
        }
    }

    @Override
    public void refreshScreen() {
        enteredPin.setLength(0);
        updatePinDisplay();
        if (targetCard != null) {
            cardHolderLabel.setText("Cardholder: " + targetCard.getCardHolderName());
            cardNumberLabel.setText("Card: " + targetCard.getMaskedCardNumber() + "  |  Exp: " + targetCard.getExpiryDate());

            if ("BLOCKED".equalsIgnoreCase(targetCard.getCardStatus())) {
                cardStatusBadge.setText(" BLOCKED ");
                cardStatusBadge.setForeground(UITheme.DANGER_RED);
                attemptWarningLabel.setText("⛔ CARD STATUS: BLOCKED. Please contact customer support.");
                attemptWarningLabel.setForeground(UITheme.DANGER_RED);
            } else {
                cardStatusBadge.setText(" ACTIVE ");
                cardStatusBadge.setForeground(UITheme.SUCCESS_GREEN);

                int fails = targetCard.getFailedPinAttempts();
                if (fails > 0) {
                    attemptWarningLabel.setText("⚠ Security Notice: " + (3 - fails) + " attempt(s) remaining before permanent lockout!");
                    attemptWarningLabel.setForeground(UITheme.WARNING_YELLOW);
                } else {
                    attemptWarningLabel.setText("Security Notice: Maximum 3 attempts before automatic card lockout.");
                    attemptWarningLabel.setForeground(UITheme.ACCENT_GOLD);
                }
            }
        }
        feedbackLabel.setText("Enter 4 digits using the physical ATM PIN pad or on-screen keys");
        feedbackLabel.setForeground(UITheme.TEXT_MUTED);
    }
}
