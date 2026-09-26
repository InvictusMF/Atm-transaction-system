package com.atm.ui.screens;

import com.atm.db.DatabaseManager;
import com.atm.model.AtmCard;
import com.atm.ui.ModernButton;
import com.atm.ui.ScreenManager;
import com.atm.ui.UITheme;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class WelcomeScreen extends JPanel implements ScreenManager.RefreshableScreen {
    private final ScreenManager screenManager;
    private JTextField manualCardField;
    private JLabel statusLabel;

    // Dynamic Card Graphic State
    private String displayCardNumber = "4532 1100 2233 4455";
    private String displayCardHolder = "ALEXANDER VANCE";
    private String displayExpiry = "12/28";
    private Color displayCardGradStart = new Color(30, 58, 138);
    private Color displayCardGradEnd = new Color(15, 23, 42);
    private JPanel cardVisual;

    public WelcomeScreen(ScreenManager screenManager) {
        this.screenManager = screenManager;
        setLayout(new BorderLayout(16, 16));
        setBackground(UITheme.SCREEN_BG);
        setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        initComponents();
    }

    private void initComponents() {
        // Top Header
        JPanel headerPanel = UITheme.createScreenHeader(
                "APEX NATIONAL BANK  •  SELF-SERVICE TERMINAL",
                "Welcome to Apex Banking",
                "Please insert your ATM/Debit card or choose a demo account to begin"
        );
        add(headerPanel, BorderLayout.NORTH);

        // Center Content: Realistic Card Preview (Left) & Selection Panel (Right)
        JPanel centerPanel = new JPanel(new GridLayout(1, 2, 24, 0));
        centerPanel.setBackground(UITheme.SCREEN_BG);

        // Left: Graphic Card Emulation
        cardVisual = createDebitCardGraphic();
        JPanel leftContainer = new JPanel(new BorderLayout(0, 8));
        leftContainer.setOpaque(false);
        leftContainer.add(cardVisual, BorderLayout.CENTER);

        JLabel cardTip = new JLabel("● EMV Chip & Contactless Card Reader Active", SwingConstants.CENTER);
        cardTip.setFont(UITheme.FONT_SMALL);
        cardTip.setForeground(UITheme.TEXT_MUTED);
        leftContainer.add(cardTip, BorderLayout.SOUTH);

        centerPanel.add(leftContainer);

        // Right: Quick Selection & Manual Input
        JPanel actionsCard = UITheme.createCardPanel(12);
        actionsCard.setLayout(new BoxLayout(actionsCard, BoxLayout.Y_AXIS));

        JPanel demoTitleRow = new JPanel(new BorderLayout());
        demoTitleRow.setOpaque(false);
        JLabel selectTitle = new JLabel("QUICK DEMO ACCOUNTS");
        selectTitle.setFont(UITheme.FONT_SMALL_BOLD);
        selectTitle.setForeground(UITheme.ACCENT_GOLD);
        demoTitleRow.add(selectTitle, BorderLayout.WEST);

        JLabel selectHint = new JLabel("Click to select & preview");
        selectHint.setFont(UITheme.FONT_SMALL);
        selectHint.setForeground(UITheme.TEXT_DIM);
        demoTitleRow.add(selectHint, BorderLayout.EAST);

        actionsCard.add(demoTitleRow);
        actionsCard.add(Box.createVerticalStrut(6));

        // Demo Buttons with hover preview
        actionsCard.add(createDemoCardButton(
                "Alexander Vance (Savings)",
                "4532 •••• •••• 4455  |  PIN: 1234  |  $5,420.50",
                "4532110022334455",
                "ALEXANDER VANCE",
                "12/28",
                new Color(30, 58, 138),
                UITheme.ACCENT_BLUE
        ));
        actionsCard.add(Box.createVerticalStrut(6));

        actionsCard.add(createDemoCardButton(
                "Sophia Williams (Checking)",
                "5412 •••• •••• 1234  |  PIN: 4321  |  $3,150.00",
                "5412750088991234",
                "SOPHIA WILLIAMS",
                "08/27",
                new Color(13, 148, 136),
                UITheme.SUCCESS_GREEN
        ));
        actionsCard.add(Box.createVerticalStrut(6));

        actionsCard.add(createDemoCardButton(
                "David Miller (BLOCKED CARD)",
                "4000 •••• •••• 9010  |  PIN: 9999  |  Status: LOCKED",
                "4000123456789010",
                "DAVID MILLER",
                "05/26",
                new Color(153, 27, 27),
                UITheme.DANGER_RED
        ));

        actionsCard.add(Box.createVerticalStrut(8));
        JSeparator sep = new JSeparator();
        sep.setForeground(UITheme.CARD_BORDER);
        actionsCard.add(sep);
        actionsCard.add(Box.createVerticalStrut(6));

        // Manual Card Entry
        JLabel manualLabel = new JLabel("Or Enter Card Number Manually:");
        manualLabel.setFont(UITheme.FONT_BODY_BOLD);
        manualLabel.setForeground(UITheme.TEXT_WHITE);
        actionsCard.add(manualLabel);
        actionsCard.add(Box.createVerticalStrut(4));

        JPanel manualRow = new JPanel(new BorderLayout(8, 0));
        manualRow.setOpaque(false);
        manualRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        manualCardField = new JTextField();
        manualCardField.setFont(new Font("Consolas", Font.BOLD, 16));
        manualCardField.setBackground(new Color(11, 20, 38));
        manualCardField.setForeground(UITheme.ACCENT_CYAN);
        manualCardField.setCaretColor(Color.WHITE);
        manualCardField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.CARD_BORDER, 1),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));

        // Enter key listener on manual field
        manualCardField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    handleCardInsert(manualCardField.getText().trim().replace(" ", "").replace("-", ""));
                }
            }
        });

        JButton insertBtn = UITheme.createModernButton("Insert Card 💳", UITheme.ACCENT_BLUE, UITheme.TEXT_WHITE);
        insertBtn.setPreferredSize(new Dimension(135, 38));
        insertBtn.addActionListener(e -> handleCardInsert(manualCardField.getText().trim().replace(" ", "").replace("-", "")));

        manualRow.add(manualCardField, BorderLayout.CENTER);
        manualRow.add(insertBtn, BorderLayout.EAST);
        actionsCard.add(manualRow);

        centerPanel.add(actionsCard);
        add(centerPanel, BorderLayout.CENTER);

        // Bottom Status / Security Notice
        JPanel bottomBar = new JPanel(new BorderLayout());
        bottomBar.setBackground(UITheme.SCREEN_BG);

        statusLabel = new JLabel("● 256-Bit TLS Hardware Encryption  |  Card Reader: Ready  |  Cash Dispenser: Online", SwingConstants.CENTER);
        statusLabel.setFont(UITheme.FONT_MONO);
        statusLabel.setForeground(UITheme.SUCCESS_GREEN);
        bottomBar.add(statusLabel, BorderLayout.CENTER);

        add(bottomBar, BorderLayout.SOUTH);
    }

    private JPanel createDebitCardGraphic() {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();

                // Outer Card Shadow
                g2.setColor(new Color(0, 0, 0, 90));
                g2.fillRoundRect(8, 8, w - 16, h - 16, 22, 22);

                // Gradient background for metallic credit card
                GradientPaint gp = new GradientPaint(0, 0, displayCardGradStart, w, h, displayCardGradEnd);
                g2.setPaint(gp);
                g2.fillRoundRect(5, 5, w - 14, h - 14, 20, 20);

                // High-gloss subtle diagonal shimmer
                g2.setPaint(new GradientPaint(0, 0, new Color(255, 255, 255, 35), w / 2, h / 2, new Color(255, 255, 255, 0)));
                g2.fillRoundRect(5, 5, w - 14, h - 14, 20, 20);

                // Border highlight
                g2.setColor(new Color(255, 255, 255, 50));
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(5, 5, w - 14, h - 14, 20, 20);

                // Bank Name & Logo
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 15));
                g2.drawString("APEX NATIONAL BANK", 26, 38);

                g2.setFont(UITheme.FONT_SMALL);
                g2.setColor(UITheme.ACCENT_CYAN);
                g2.drawString("PREMIER DEBIT", 26, 52);

                // Golden EMV Chip
                g2.setColor(new Color(245, 158, 11));
                g2.fillRoundRect(26, 68, 46, 34, 6, 6);
                g2.setColor(new Color(180, 83, 9));
                g2.drawRoundRect(26, 68, 46, 34, 6, 6);
                g2.drawLine(26, 85, 72, 85);
                g2.drawLine(49, 68, 49, 102);

                // Contactless Wave Icon
                g2.setColor(new Color(226, 232, 240));
                g2.setStroke(new BasicStroke(1.8f));
                g2.drawArc(84, 75, 12, 20, -50, 100);
                g2.drawArc(90, 71, 16, 28, -50, 100);

                // Card Number Embossed Text
                g2.setColor(new Color(248, 250, 252));
                g2.setFont(new Font("Consolas", Font.BOLD, 18));
                g2.drawString(displayCardNumber, 26, 142);

                // Expiry and Name
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                g2.setColor(new Color(148, 163, 184));
                g2.drawString("VALID THRU", 26, 170);

                g2.setFont(new Font("Consolas", Font.BOLD, 12));
                g2.setColor(Color.WHITE);
                g2.drawString(displayExpiry, 26, 185);

                g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
                g2.drawString(displayCardHolder, 26, 215);

                // Card Brand Logo (Interlocking Mastercard style circles)
                g2.setColor(new Color(239, 68, 68, 230));
                g2.fillOval(w - 86, h - 58, 32, 32);
                g2.setColor(new Color(245, 158, 11, 230));
                g2.fillOval(w - 66, h - 58, 32, 32);

                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setPreferredSize(new Dimension(320, 220));
        return card;
    }

    private JButton createDemoCardButton(String name, String details, String rawCard, String cardHolder, String expiry, Color gradStart, Color accent) {
        ModernButton btn = (ModernButton) UITheme.createCardButton(name, details, accent, e -> {
            updateCardPreview(rawCard, cardHolder, expiry, gradStart);
            handleCardInsert(rawCard);
        });
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));

        // Hover effect to update card visual dynamically
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                updateCardPreview(rawCard, cardHolder, expiry, gradStart);
            }
        });
        return btn;
    }

    private void updateCardPreview(String rawCard, String cardHolder, String expiry, Color gradStart) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < rawCard.length(); i++) {
            if (i > 0 && i % 4 == 0) sb.append(" ");
            sb.append(rawCard.charAt(i));
        }
        this.displayCardNumber = sb.toString();
        this.displayCardHolder = cardHolder;
        this.displayExpiry = expiry;
        this.displayCardGradStart = gradStart;
        if (cardVisual != null) cardVisual.repaint();
    }

    private void handleCardInsert(String cardNumber) {
        if (cardNumber == null || cardNumber.isEmpty()) {
            statusLabel.setText("● Please enter or select a valid card number.");
            statusLabel.setForeground(UITheme.WARNING_YELLOW);
            return;
        }

        AtmCard card = DatabaseManager.getInstance().getCard(cardNumber);
        if (card == null) {
            statusLabel.setText("● Card Number Not Recognized in System. Please verify and try again.");
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
            statusLabel.setText("● 256-Bit TLS Hardware Encryption  |  Card Reader: Ready  |  Cash Dispenser: Online");
            statusLabel.setForeground(UITheme.SUCCESS_GREEN);
        }
        updateCardPreview("4532110022334455", "ALEXANDER VANCE", "12/28", new Color(30, 58, 138));
    }
}
