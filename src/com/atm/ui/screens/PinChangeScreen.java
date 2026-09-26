package com.atm.ui.screens;

import com.atm.service.AtmService;
import com.atm.service.AtmSession;
import com.atm.ui.ScreenManager;
import com.atm.ui.UITheme;

import javax.swing.*;
import java.awt.*;

public class PinChangeScreen extends JPanel implements ScreenManager.RefreshableScreen {
    private final ScreenManager screenManager;

    private JPasswordField oldPinField;
    private JPasswordField newPinField;
    private JPasswordField confirmPinField;
    private JLabel feedbackLabel;

    public PinChangeScreen(ScreenManager screenManager) {
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

        JLabel title = new JLabel("UPDATE SECRET PIN", SwingConstants.CENTER);
        title.setFont(UITheme.FONT_TITLE_LARGE);
        title.setForeground(UITheme.ACCENT_CYAN);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("Card Security & Access Code Management", SwingConstants.CENTER);
        subtitle.setFont(UITheme.FONT_SUBTITLE);
        subtitle.setForeground(UITheme.TEXT_MUTED);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        headerPanel.add(title);
        headerPanel.add(Box.createVerticalStrut(4));
        headerPanel.add(subtitle);
        add(headerPanel, BorderLayout.NORTH);

        // Center Form
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBackground(UITheme.SCREEN_BG);

        JPanel formCard = UITheme.createCardPanel();
        formCard.setLayout(new GridLayout(3, 2, 12, 14));
        formCard.setMaximumSize(new Dimension(460, 160));

        oldPinField = createPinField();
        newPinField = createPinField();
        confirmPinField = createPinField();

        addFormRow(formCard, "Current 4-Digit PIN:", oldPinField);
        addFormRow(formCard, "Enter New 4-Digit PIN:", newPinField);
        addFormRow(formCard, "Confirm New PIN:", confirmPinField);

        centerPanel.add(formCard);
        centerPanel.add(Box.createVerticalStrut(15));

        feedbackLabel = new JLabel("PIN must be strictly 4 numeric digits", SwingConstants.CENTER);
        feedbackLabel.setFont(UITheme.FONT_BODY);
        feedbackLabel.setForeground(UITheme.TEXT_MUTED);
        feedbackLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        centerPanel.add(feedbackLabel);

        add(centerPanel, BorderLayout.CENTER);

        // Bottom Controls
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        bottomPanel.setBackground(UITheme.SCREEN_BG);

        JButton backBtn = UITheme.createModernButton("⬅ Back to Menu", UITheme.CHASSIS_BG, UITheme.TEXT_WHITE);
        backBtn.addActionListener(e -> screenManager.showScreen("MAIN_MENU"));

        JButton clearBtn = UITheme.createModernButton("⌫ Clear Fields", UITheme.WARNING_YELLOW, Color.BLACK);
        clearBtn.addActionListener(e -> clearInputs());

        JButton submitBtn = UITheme.createModernButton("✔ Update PIN", UITheme.SUCCESS_GREEN, Color.BLACK);
        submitBtn.addActionListener(e -> executePinChange());

        bottomPanel.add(backBtn);
        bottomPanel.add(clearBtn);
        bottomPanel.add(submitBtn);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    private JPasswordField createPinField() {
        JPasswordField pf = new JPasswordField();
        pf.setFont(new Font("Segoe UI", Font.BOLD, 20));
        pf.setHorizontalAlignment(JTextField.CENTER);
        pf.setBackground(new Color(11, 20, 38));
        pf.setForeground(UITheme.ACCENT_CYAN);
        pf.setCaretColor(Color.WHITE);
        pf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.ACCENT_CYAN, 1),
                BorderFactory.createEmptyBorder(4, 10, 4, 10)
        ));
        return pf;
    }

    private void addFormRow(JPanel parent, String label, JPasswordField pf) {
        JLabel l = new JLabel(label);
        l.setFont(UITheme.FONT_BODY_BOLD);
        l.setForeground(UITheme.TEXT_WHITE);
        parent.add(l);
        parent.add(pf);
    }

    private void clearInputs() {
        oldPinField.setText("");
        newPinField.setText("");
        confirmPinField.setText("");
        feedbackLabel.setText("PIN must be strictly 4 numeric digits");
        feedbackLabel.setForeground(UITheme.TEXT_MUTED);
    }

    private void executePinChange() {
        AtmSession session = screenManager.getSession();
        if (session == null) return;

        String oldPin = new String(oldPinField.getPassword());
        String newPin = new String(newPinField.getPassword());
        String confirmPin = new String(confirmPinField.getPassword());

        AtmService.OperationResult<Boolean> result = screenManager.getAtmService().changePin(session, oldPin, newPin, confirmPin);
        if (result.isSuccess()) {
            JOptionPane.showMessageDialog(this,
                    "Your ATM Card PIN has been updated successfully!\nPlease memorize your new PIN.",
                    "PIN CHANGED SUCCESSFULLY",
                    JOptionPane.INFORMATION_MESSAGE);
            clearInputs();
            screenManager.showScreen("MAIN_MENU");
        } else {
            feedbackLabel.setText("✖ " + result.getMessage());
            feedbackLabel.setForeground(UITheme.DANGER_RED);
            JOptionPane.showMessageDialog(this, result.getMessage(), "PIN UPDATE FAILED", JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    public void refreshScreen() {
        clearInputs();
    }
}
