package com.atm.ui.screens;

import com.atm.db.DatabaseManager;
import com.atm.model.AtmCard;
import com.atm.ui.ScreenManager;
import com.atm.ui.UITheme;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;

public class WelcomeScreen extends JPanel implements ScreenManager.RefreshableScreen {
    private final ScreenManager screenManager;
    private JTextField manualCardField;
    private JLabel statusLabel;

    public WelcomeScreen(ScreenManager screenManager) {
        this.screenManager = screenManager;
        setLayout(new BorderLayout(15, 15));
        setBackground(UITheme.SCREEN_BG);
        setBorder(BorderFactory.createEmptyBorder(25, 30, 25, 30));

        initComponents();
    }

    private void initComponents() {
        // Top Header
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setBackground(UITheme.SCREEN_BG);

        JLabel bankTitle = new JLabel("APEX NATIONAL BANK", SwingConstants.CENTER);
        bankTitle.setFont(UITheme.FONT_TITLE_LARGE);
        bankTitle.setForeground(UITheme.ACCENT_CYAN);
        bankTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel welcomeSub = new JLabel("SECURE 24/7 ATM SELF-SERVICE TERMINAL", SwingConstants.CENTER);
        welcomeSub.setFont(UITheme.FONT_SUBTITLE);
        welcomeSub.setForeground(UITheme.TEXT_WHITE);
        welcomeSub.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel instruct = new JLabel("Please insert your debit card or select a demo account below", SwingConstants.CENTER);
        instruct.setFont(UITheme.FONT_BODY);
        instruct.setForeground(UITheme.TEXT_MUTED);
        instruct.setAlignmentX(Component.CENTER_ALIGNMENT);

        headerPanel.add(bankTitle);
        headerPanel.add(Box.createVerticalStrut(4));
        headerPanel.add(welcomeSub);
        headerPanel.add(Box.createVerticalStrut(6));
        headerPanel.add(instruct);

        add(headerPanel, BorderLayout.NORTH);

        // Center Content: Realistic Card Display & Demo Fast-Picker
        JPanel centerPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        centerPanel.setBackground(UITheme.SCREEN_BG);

        // Left Card: Graphic Emulation
        JPanel cardVisual = createDebitCardGraphic();
        centerPanel.add(cardVisual);

        // Right Card: Quick Selection or Manual Entry
        JPanel actionsCard = UITheme.createCardPanel();
        actionsCard.setLayout(new BoxLayout(actionsCard, BoxLayout.Y_AXIS));

        JLabel selectTitle = new JLabel("Quick Account Selection (Demo Mode)");
        selectTitle.setFont(UITheme.FONT_BODY_BOLD);
        selectTitle.setForeground(UITheme.ACCENT_GOLD);
        actionsCard.add(selectTitle);
        actionsCard.add(Box.createVerticalStrut(8));

        JButton demo1 = createDemoButton("Alexander Vance", "4532 1100 2233 4455", "PIN: 1234 | Savings ($5,420.50)", "4532110022334455", UITheme.ACCENT_BLUE);
        JButton demo2 = createDemoButton("Sophia Williams", "5412 7500 8899 1234", "PIN: 4321 | Checking ($3,150.00)", "5412750088991234", new Color(16, 185, 129));
        JButton demo3 = createDemoButton("David Miller (BLOCKED)", "4000 1234 5678 9010", "PIN: 9999 | Status: BLOCKED", "4000123456789010", UITheme.DANGER_RED);

        actionsCard.add(demo1);
        actionsCard.add(Box.createVerticalStrut(6));
        actionsCard.add(demo2);
        actionsCard.add(Box.createVerticalStrut(6));
        actionsCard.add(demo3);

        actionsCard.add(Box.createVerticalStrut(10));
        JLabel manualLabel = new JLabel("Or Enter Card Number Manually:");
        manualLabel.setFont(UITheme.FONT_BODY);
        manualLabel.setForeground(UITheme.TEXT_MUTED);
        actionsCard.add(manualLabel);
        actionsCard.add(Box.createVerticalStrut(4));

        JPanel manualRow = new JPanel(new BorderLayout(8, 0));
        manualRow.setBackground(UITheme.SCREEN_CARD);
        manualRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        manualCardField = new JTextField();
        manualCardField.setFont(UITheme.FONT_TITLE);
        manualCardField.setBackground(new Color(11, 20, 38));
        manualCardField.setForeground(UITheme.ACCENT_CYAN);
        manualCardField.setCaretColor(Color.WHITE);
        manualCardField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(56, 189, 248), 1),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));

        JButton insertBtn = UITheme.createModernButton("Insert Card", UITheme.ACCENT_BLUE, UITheme.TEXT_WHITE);
        insertBtn.addActionListener(e -> handleCardInsert(manualCardField.getText().trim().replace(" ", "").replace("-", "")));

        manualRow.add(manualCardField, BorderLayout.CENTER);
        manualRow.add(insertBtn, BorderLayout.EAST);
        actionsCard.add(manualRow);

        centerPanel.add(actionsCard);
        add(centerPanel, BorderLayout.CENTER);

        // Bottom Status / Security Notice
        statusLabel = new JLabel("● 256-Bit SSL Encrypted Connection  |  Terminal Hardware Status: Normal", SwingConstants.CENTER);
        statusLabel.setFont(UITheme.FONT_MONO);
        statusLabel.setForeground(UITheme.SUCCESS_GREEN);
        add(statusLabel, BorderLayout.SOUTH);
    }

    private JPanel createDebitCardGraphic() {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Gradient background for metallic credit card
                GradientPaint gp = new GradientPaint(0, 0, new Color(30, 58, 138), getWidth(), getHeight(), new Color(15, 23, 42));
                g2.setPaint(gp);
                g2.fillRoundRect(5, 5, getWidth() - 10, getHeight() - 10, 20, 20);

                // Border highlight
                g2.setColor(new Color(96, 165, 250, 100));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(5, 5, getWidth() - 10, getHeight() - 10, 20, 20);

                // Bank Name
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 16));
                g2.drawString("APEX NATIONAL BANK", 25, 40);

                // Chip Graphic (Golden EMV Chip)
                g2.setColor(new Color(234, 179, 8));
                g2.fillRoundRect(25, 60, 44, 32, 6, 6);
                g2.setColor(new Color(161, 98, 7));
                g2.drawRoundRect(25, 60, 44, 32, 6, 6);
                g2.drawLine(25, 76, 69, 76);
                g2.drawLine(47, 60, 47, 92);

                // Contactless Wave Icon
                g2.setColor(new Color(203, 213, 225));
                g2.drawArc(78, 65, 12, 22, -60, 120);
                g2.drawArc(84, 61, 16, 30, -60, 120);

                // Card Number Embossed Text
                g2.setColor(new Color(241, 245, 249));
                g2.setFont(new Font("Consolas", Font.BOLD, 18));
                g2.drawString("4532  1100  2233  4455", 25, 130);

                // Expiry and Name
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                g2.setColor(new Color(148, 163, 184));
                g2.drawString("VALID THRU", 25, 160);
                g2.setFont(new Font("Consolas", Font.BOLD, 12));
                g2.setColor(Color.WHITE);
                g2.drawString("12/28", 25, 176);

                g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
                g2.drawString("ALEXANDER VANCE", 25, 205);

                // Card Brand Logo (Visa/Mastercard style circles)
                g2.setColor(new Color(239, 68, 68, 220));
                g2.fillOval(getWidth() - 85, getHeight() - 55, 32, 32);
                g2.setColor(new Color(245, 158, 11, 220));
                g2.fillOval(getWidth() - 65, getHeight() - 55, 32, 32);

                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setPreferredSize(new Dimension(320, 220));
        return card;
    }

    private JButton createDemoButton(String name, String cardNum, String details, String rawCard, Color accent) {
        ModernButton btn = (ModernButton) UITheme.createCardButton(name, cardNum + "  •  " + details, accent, e -> handleCardInsert(rawCard));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        return btn;
    }

    private void handleCardInsert(String cardNumber) {
        if (cardNumber.isEmpty()) {
            statusLabel.setText("● Please enter or select a valid card number.");
            statusLabel.setForeground(UITheme.WARNING_YELLOW);
            return;
        }

        AtmCard card = DatabaseManager.getInstance().getCard(cardNumber);
        if (card == null) {
            statusLabel.setText("● Card Number Not Recognized in System.");
            statusLabel.setForeground(UITheme.DANGER_RED);
            return;
        }

        PinEntryScreen pinScreen = (PinEntryScreen) screenManager.getScreen("PIN_ENTRY");
        pinScreen.setTargetCard(card);
        screenManager.showScreen("PIN_ENTRY");
    }

    @Override
    public void refreshScreen() {
        if (manualCardField != null) manualCardField.setText("");
        if (statusLabel != null) {
            statusLabel.setText("● 256-Bit SSL Encrypted Connection  |  Terminal Hardware Status: Normal");
            statusLabel.setForeground(UITheme.SUCCESS_GREEN);
        }
    }
}
