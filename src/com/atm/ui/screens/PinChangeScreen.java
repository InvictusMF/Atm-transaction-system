package com.atm.ui.screens;

import com.atm.service.AtmService;
import com.atm.service.AtmSession;
import com.atm.ui.ModernButton;
import com.atm.ui.ScreenManager;
import com.atm.ui.UITheme;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;

public class PinChangeScreen extends JPanel implements ScreenManager.RefreshableScreen {
    private enum ScreenState {
        FORM,
        PROCESSING,
        SUCCESS,
        ERROR
    }

    private final ScreenManager screenManager;
    private ScreenState currentState = ScreenState.FORM;

    private final CardLayout stepLayout = new CardLayout();
    private final JPanel stepContainer = new JPanel(stepLayout);

    // Step 1: Form Inputs
    private JPasswordField oldPinField;
    private JPasswordField newPinField;
    private JPasswordField confirmPinField;
    private JLabel ruleLength;
    private JLabel ruleMatch;
    private JLabel ruleDiff;
    private JLabel feedbackLabel;

    // Step 4: Error
    private JLabel errMessageLabel;

    public PinChangeScreen(ScreenManager screenManager) {
        this.screenManager = screenManager;
        setLayout(new BorderLayout());
        setBackground(UITheme.SCREEN_BG);

        initAllSteps();
    }

    private void initAllSteps() {
        stepContainer.setBackground(UITheme.SCREEN_BG);
        stepContainer.add(createFormStep(), ScreenState.FORM.name());
        stepContainer.add(createProcessingStep(), ScreenState.PROCESSING.name());
        stepContainer.add(createSuccessStep(), ScreenState.SUCCESS.name());
        stepContainer.add(createErrorStep(), ScreenState.ERROR.name());

        add(stepContainer, BorderLayout.CENTER);
    }

    private JPanel createFormStep() {
        JPanel panel = new JPanel(new BorderLayout(14, 14));
        panel.setBackground(UITheme.SCREEN_BG);
        panel.setBorder(BorderFactory.createEmptyBorder(18, 35, 18, 35));

        JPanel header = UITheme.createScreenHeader(
                "SECURITY MANAGEMENT  •  PIN UPDATE",
                "Change Secret PIN Code",
                "Update your confidential 4-digit card authentication code"
        );
        panel.add(header, BorderLayout.NORTH);

        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);

        JPanel formCard = UITheme.createCardPanel(18);
        formCard.setLayout(new GridLayout(3, 2, 14, 12));
        formCard.setMaximumSize(new Dimension(500, 160));

        oldPinField = createPinField();
        newPinField = createPinField();
        confirmPinField = createPinField();

        DocumentListener dl = new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { validateRules(); }
            @Override public void removeUpdate(DocumentEvent e) { validateRules(); }
            @Override public void changedUpdate(DocumentEvent e) { validateRules(); }
        };
        oldPinField.getDocument().addDocumentListener(dl);
        newPinField.getDocument().addDocumentListener(dl);
        confirmPinField.getDocument().addDocumentListener(dl);

        addFormRow(formCard, "Current 4-Digit PIN:", oldPinField);
        addFormRow(formCard, "Enter New 4-Digit PIN:", newPinField);
        addFormRow(formCard, "Confirm New PIN:", confirmPinField);

        center.add(formCard);
        center.add(Box.createVerticalStrut(12));

        // Live Rules Checklist
        JPanel rulesCard = UITheme.createCardPanel(10);
        rulesCard.setLayout(new FlowLayout(FlowLayout.CENTER, 16, 0));
        rulesCard.setMaximumSize(new Dimension(500, 42));

        ruleLength = new JLabel("● Exactly 4 Digits");
        ruleLength.setFont(UITheme.FONT_SMALL_BOLD);
        ruleLength.setForeground(UITheme.TEXT_MUTED);

        ruleMatch = new JLabel("● PINs Match");
        ruleMatch.setFont(UITheme.FONT_SMALL_BOLD);
        ruleMatch.setForeground(UITheme.TEXT_MUTED);

        ruleDiff = new JLabel("● Different From Old");
        ruleDiff.setFont(UITheme.FONT_SMALL_BOLD);
        ruleDiff.setForeground(UITheme.TEXT_MUTED);

        rulesCard.add(ruleLength);
        rulesCard.add(ruleMatch);
        rulesCard.add(ruleDiff);
        center.add(rulesCard);
        center.add(Box.createVerticalStrut(8));

        feedbackLabel = new JLabel("Shield the keypad while entering your new secret PIN", SwingConstants.CENTER);
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

        JButton clearBtn = UITheme.createModernButton("⌫ Clear Fields", UITheme.WARNING_YELLOW, Color.BLACK);
        clearBtn.setPreferredSize(new Dimension(150, 42));
        clearBtn.addActionListener(e -> clearInputs());

        JButton submitBtn = UITheme.createModernButton("✔ Update PIN ➡", UITheme.SUCCESS_GREEN, Color.BLACK);
        submitBtn.setPreferredSize(new Dimension(170, 42));
        submitBtn.addActionListener(e -> executePinChange());

        bottomNav.add(backBtn);
        bottomNav.add(clearBtn);
        bottomNav.add(submitBtn);

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

        JLabel spinner = new JLabel("🔒", SwingConstants.CENTER);
        spinner.setFont(new Font("Segoe UI", Font.PLAIN, 48));
        spinner.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel title = new JLabel("Updating Cryptographic PIN...", SwingConstants.CENTER);
        title.setFont(UITheme.FONT_TITLE_LARGE);
        title.setForeground(UITheme.ACCENT_CYAN);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("Hashing with SHA-256 and updating secure card database records...", SwingConstants.CENTER);
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
                "PIN UPDATE  •  COMPLETED",
                "PIN Changed Successfully",
                "Your card authentication credentials have been securely updated"
        );
        panel.add(header, BorderLayout.NORTH);

        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);

        JPanel succCard = UITheme.createCardPanel(24);
        succCard.setLayout(new BoxLayout(succCard, BoxLayout.Y_AXIS));
        succCard.setMaximumSize(new Dimension(500, 180));

        JLabel title = new JLabel("New PIN Is Now Active", SwingConstants.CENTER);
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.SUCCESS_GREEN);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel tip = new JLabel("<html><center>For your personal security, please memorize your new PIN.<br>Never write down or share your PIN with anyone.<br><br>All future ATM and POS transactions will require your new 4-digit code.</center></html>", SwingConstants.CENTER);
        tip.setFont(UITheme.FONT_BODY);
        tip.setForeground(UITheme.TEXT_WHITE);
        tip.setAlignmentX(Component.CENTER_ALIGNMENT);

        succCard.add(title);
        succCard.add(Box.createVerticalStrut(10));
        succCard.add(tip);

        center.add(succCard);
        panel.add(center, BorderLayout.CENTER);

        // Bottom Controls
        JPanel bottomNav = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        bottomNav.setOpaque(false);

        JButton menuBtn = UITheme.createModernButton("⬅ Return to Dashboard", UITheme.ACCENT_BLUE, UITheme.TEXT_WHITE);
        menuBtn.setPreferredSize(new Dimension(190, 42));
        menuBtn.addActionListener(e -> {
            clearInputs();
            screenManager.showScreen("MAIN_MENU");
        });

        JButton logoutBtn = UITheme.createModernButton("⏏ Eject Card & Logout", UITheme.DANGER_RED, UITheme.TEXT_WHITE);
        logoutBtn.setPreferredSize(new Dimension(190, 42));
        logoutBtn.addActionListener(e -> {
            screenManager.setSession(null);
            screenManager.showScreen("WELCOME");
        });

        bottomNav.add(menuBtn);
        bottomNav.add(logoutBtn);

        panel.add(bottomNav, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createErrorStep() {
        JPanel panel = new JPanel(new BorderLayout(14, 14));
        panel.setBackground(UITheme.SCREEN_BG);
        panel.setBorder(BorderFactory.createEmptyBorder(30, 35, 30, 35));

        JPanel header = UITheme.createScreenHeader(
                "PIN UPDATE  •  FAILED",
                "PIN Change Declined",
                "The requested PIN update could not be authorized"
        );
        panel.add(header, BorderLayout.NORTH);

        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);

        JPanel errCard = UITheme.createCardPanel(24);
        errCard.setLayout(new BoxLayout(errCard, BoxLayout.Y_AXIS));
        errCard.setMaximumSize(new Dimension(500, 160));

        JLabel errTitle = new JLabel("Authentication Error", SwingConstants.CENTER);
        errTitle.setFont(UITheme.FONT_TITLE);
        errTitle.setForeground(UITheme.DANGER_RED);
        errTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        errMessageLabel = new JLabel("Reason for decline", SwingConstants.CENTER);
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

        JButton retryBtn = UITheme.createModernButton("↺ Try Again", UITheme.ACCENT_BLUE, UITheme.TEXT_WHITE);
        retryBtn.setPreferredSize(new Dimension(170, 42));
        retryBtn.addActionListener(e -> setStep(ScreenState.FORM));

        JButton menuBtn = UITheme.createModernButton("⬅ Return to Dashboard", UITheme.CHASSIS_BG, UITheme.TEXT_WHITE);
        menuBtn.setPreferredSize(new Dimension(190, 42));
        menuBtn.addActionListener(e -> screenManager.showScreen("MAIN_MENU"));

        bottomNav.add(retryBtn);
        bottomNav.add(menuBtn);

        panel.add(bottomNav, BorderLayout.SOUTH);
        return panel;
    }

    private JPasswordField createPinField() {
        JPasswordField pf = new JPasswordField();
        pf.setFont(new Font("Segoe UI", Font.BOLD, 22));
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

    private void validateRules() {
        String oldP = new String(oldPinField.getPassword());
        String newP = new String(newPinField.getPassword());
        String confP = new String(confirmPinField.getPassword());

        boolean lenOk = newP.length() == 4 && newP.matches("\\d{4}");
        ruleLength.setForeground(lenOk ? UITheme.SUCCESS_GREEN : UITheme.TEXT_MUTED);
        ruleLength.setText(lenOk ? "✔ Exactly 4 Digits" : "● Exactly 4 Digits");

        boolean matchOk = !newP.isEmpty() && newP.equals(confP);
        ruleMatch.setForeground(matchOk ? UITheme.SUCCESS_GREEN : UITheme.TEXT_MUTED);
        ruleMatch.setText(matchOk ? "✔ PINs Match" : "● PINs Match");

        boolean diffOk = !newP.isEmpty() && !newP.equals(oldP);
        ruleDiff.setForeground(diffOk ? UITheme.SUCCESS_GREEN : UITheme.TEXT_MUTED);
        ruleDiff.setText(diffOk ? "✔ Different From Old" : "● Different From Old");
    }

    private void clearInputs() {
        oldPinField.setText("");
        newPinField.setText("");
        confirmPinField.setText("");
        validateRules();
        feedbackLabel.setText("Shield the keypad while entering your new secret PIN");
        feedbackLabel.setForeground(UITheme.TEXT_MUTED);
    }

    private void setStep(ScreenState state) {
        this.currentState = state;
        stepLayout.show(stepContainer, state.name());
    }

    private void executePinChange() {
        AtmSession session = screenManager.getSession();
        if (session == null) return;

        String oldPin = new String(oldPinField.getPassword());
        String newPin = new String(newPinField.getPassword());
        String confirmPin = new String(confirmPinField.getPassword());

        if (newPin.length() != 4 || !newPin.matches("\\d{4}")) {
            feedbackLabel.setText("⚠ New PIN must be strictly 4 numeric digits.");
            feedbackLabel.setForeground(UITheme.WARNING_YELLOW);
            return;
        }

        if (!newPin.equals(confirmPin)) {
            feedbackLabel.setText("⚠ New PIN and Confirmation PIN do not match.");
            feedbackLabel.setForeground(UITheme.WARNING_YELLOW);
            return;
        }

        setStep(ScreenState.PROCESSING);

        Timer timer = new Timer(500, e -> {
            ((Timer) e.getSource()).stop();
            AtmService.OperationResult<Boolean> result = screenManager.getAtmService().changePin(session, oldPin, newPin, confirmPin);
            if (result.isSuccess()) {
                setStep(ScreenState.SUCCESS);
            } else {
                errMessageLabel.setText("<html><center>" + result.getMessage() + "</center></html>");
                setStep(ScreenState.ERROR);
            }
        });
        timer.setRepeats(false);
        timer.start();
    }

    @Override
    public void refreshScreen() {
        clearInputs();
        setStep(ScreenState.FORM);
    }
}
