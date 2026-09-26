package com.atm.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.math.BigDecimal;

public class UITheme {
    // Primary Color Palette - Banking / Fintech Theme
    public static final Color BG_DARK = new Color(11, 18, 36);          // Deep Midnight Blue
    public static final Color CHASSIS_BG = new Color(22, 33, 56);        // Slate Steel Kiosk
    public static final Color SCREEN_BG = new Color(10, 18, 36);         // ATM Midnight Display
    public static final Color SCREEN_CARD = new Color(18, 30, 56);       // Elevated Card Blue
    public static final Color SCREEN_CARD_LIGHT = new Color(26, 42, 74); // Lighter Card Surface
    public static final Color CARD_BORDER = new Color(36, 54, 94);       // Refined Slate Navy Border
    public static final Color ACCENT_CYAN = new Color(56, 189, 248);     // Electric Cyan 400
    public static final Color ACCENT_BLUE = new Color(37, 99, 235);      // Royal Blue 600
    public static final Color ACCENT_GOLD = new Color(251, 191, 36);     // Amber Gold 400
    public static final Color SUCCESS_GREEN = new Color(34, 197, 94);    // Emerald 500
    public static final Color DANGER_RED = new Color(239, 68, 68);       // Crimson 500
    public static final Color WARNING_YELLOW = new Color(245, 158, 11);  // Amber 500

    // Text Colors
    public static final Color TEXT_WHITE = new Color(248, 250, 252);
    public static final Color TEXT_MUTED = new Color(148, 163, 184);
    public static final Color TEXT_DIM = new Color(100, 116, 139);
    public static final Color TEXT_CYAN = new Color(125, 211, 252);

    // Typography
    public static final Font FONT_TITLE_LARGE = new Font("Segoe UI", Font.BOLD, 22);
    public static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 17);
    public static final Font FONT_SUBTITLE = new Font("Segoe UI", Font.BOLD, 14);
    public static final Font FONT_BODY = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_BODY_BOLD = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 11);
    public static final Font FONT_SMALL_BOLD = new Font("Segoe UI", Font.BOLD, 11);
    public static final Font FONT_KEYPAD = new Font("Segoe UI", Font.BOLD, 18);
    public static final Font FONT_MONO = new Font("Consolas", Font.PLAIN, 12);
    public static final Font FONT_MONO_BOLD = new Font("Consolas", Font.BOLD, 13);
    public static final Font FONT_AMOUNT = new Font("Segoe UI", Font.BOLD, 28);

    /**
     * Creates an ultra-modern, anti-aliased button with gradient styling and vector icons.
     */
    public static JButton createModernButton(String text, Color bgColor, Color fgColor) {
        ModernButton.IconType icon = ModernButton.IconType.NONE;
        String cleanText = text;

        if (text != null) {
            if (text.contains("⬅") || text.contains("Back") || text.contains("←")) {
                icon = ModernButton.IconType.BACK;
                cleanText = text.replace("⬅", "").replace("←", "").trim();
            } else if (text.contains("✔") || text.contains("Accept") || text.contains("Submit") || text.contains("Confirm") || text.contains("Authorize") || text.contains("Withdraw Cash")) {
                icon = ModernButton.IconType.CHECK;
                cleanText = text.replace("✔", "").trim();
            } else if (text.contains("✖") || text.contains("Cancel") || text.contains("Eject") || text.contains("✕") || text.contains("Exit")) {
                if (text.contains("Eject") || text.contains("Exit") || text.contains("Logout")) {
                    icon = ModernButton.IconType.LOGOUT;
                } else {
                    icon = ModernButton.IconType.CANCEL;
                }
                cleanText = text.replace("✖", "").replace("✕", "").trim();
            } else if (text.contains("⌫") || text.contains("Clear") || text.contains("Reset")) {
                if (text.contains("↺")) {
                    icon = ModernButton.IconType.REFRESH;
                    cleanText = text.replace("↺", "").trim();
                } else {
                    icon = ModernButton.IconType.CLEAR;
                    cleanText = text.replace("⌫", "").trim();
                }
            } else if (text.contains("🖨") || text.contains("Print") || text.contains("Receipt")) {
                icon = ModernButton.IconType.PRINT;
                cleanText = text.replace("🖨", "").trim();
            } else if (text.contains("🗄") || text.contains("Database")) {
                icon = ModernButton.IconType.DATABASE;
                cleanText = text.replace("🗄", "").trim();
            } else if (text.contains("📄") || text.contains("PDF")) {
                icon = ModernButton.IconType.PDF;
                cleanText = text.replace("📄", "").trim();
            } else if (text.contains("💵") || text.contains("Cash")) {
                icon = ModernButton.IconType.CASH;
                cleanText = text.replace("💵", "").trim();
            } else if (text.contains("💳") || text.contains("Card")) {
                icon = ModernButton.IconType.CARD;
                cleanText = text.replace("💳", "").trim();
            } else if (text.contains("Transfer") || text.contains("↗")) {
                icon = ModernButton.IconType.TRANSFER;
                cleanText = text.replace("↗", "").trim();
            } else if (text.contains("Deposit") || text.contains("📥")) {
                icon = ModernButton.IconType.DEPOSIT;
                cleanText = text.replace("📥", "").trim();
            } else if (text.contains("History") || text.contains("Statement")) {
                icon = ModernButton.IconType.HISTORY;
            } else if (text.contains("PIN") || text.contains("Security") || text.contains("Lock")) {
                icon = ModernButton.IconType.LOCK;
            }

            if (cleanText.contains("➡") || cleanText.contains("→")) {
                cleanText = cleanText.replace("➡", "").replace("→", "").trim();
                if (icon == ModernButton.IconType.NONE) {
                    icon = ModernButton.IconType.ENTER;
                }
            }
        }

        // Guarantee all emojis and non-ASCII glyphs are stripped so no missing-box renders
        cleanText = cleanText.replaceAll("[^\\x00-\\x7F]", "").trim();

        ModernButton.ButtonType type;
        if (bgColor.equals(SUCCESS_GREEN) || bgColor.equals(new Color(16, 185, 129))) {
            type = ModernButton.ButtonType.SUCCESS;
        } else if (bgColor.equals(DANGER_RED) || bgColor.equals(new Color(220, 38, 38))) {
            type = ModernButton.ButtonType.DANGER;
        } else if (bgColor.equals(WARNING_YELLOW) || bgColor.equals(new Color(234, 179, 8))) {
            type = ModernButton.ButtonType.WARNING;
        } else if (bgColor.equals(ACCENT_BLUE) || bgColor.equals(new Color(37, 99, 235))) {
            type = ModernButton.ButtonType.PRIMARY;
        } else if (bgColor.equals(CHASSIS_BG) || bgColor.equals(new Color(51, 65, 85))) {
            type = ModernButton.ButtonType.SECONDARY;
        } else {
            type = ModernButton.ButtonType.CUSTOM;
        }

        ModernButton btn;
        if (type == ModernButton.ButtonType.CUSTOM) {
            btn = new ModernButton(cleanText, bgColor, fgColor, icon);
        } else {
            btn = new ModernButton(cleanText, type, icon);
        }

        return btn;
    }

    /**
     * Creates an elevated card button for navigation menus and account selections.
     */
    public static JButton createCardButton(String title, String subtitle, Color accentColor, ActionListener action) {
        ModernButton btn = new ModernButton(title, ModernButton.ButtonType.CARD);
        btn.setSubtitle(subtitle);
        btn.setAccentColor(accentColor);
        if (action != null) {
            btn.addActionListener(action);
        }
        return btn;
    }

    /**
     * Creates a quick increment pill button (e.g. +$50, +$100).
     */
    public static JButton createPillButton(String text, ActionListener action) {
        ModernButton btn = new ModernButton(text, ModernButton.ButtonType.PILL);
        if (action != null) {
            btn.addActionListener(action);
        }
        return btn;
    }

    /**
     * Creates a tactile brushed-metallic keypad digit button.
     */
    public static JButton createKeypadButton(String text, ActionListener action) {
        ModernButton btn = new ModernButton(text, ModernButton.ButtonType.KEYPAD_NUM);
        if (action != null) {
            btn.addActionListener(action);
        }
        return btn;
    }

    public static JPanel createCardPanel() {
        return createCardPanel(16);
    }

    public static JPanel createCardPanel(int padding) {
        JPanel panel = new JPanel();
        panel.setBackground(SCREEN_CARD);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(CARD_BORDER, 1, true),
                BorderFactory.createEmptyBorder(padding, padding, padding, padding)
        ));
        return panel;
    }

    /**
     * Standardized top header for screens with breadcrumb context, prominent title, and subtitle.
     */
    public static JPanel createScreenHeader(String breadcrumb, String title, String subtitle) {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBackground(SCREEN_BG);

        if (breadcrumb != null && !breadcrumb.isEmpty()) {
            String cleanBc = breadcrumb.replace('–', '-').replace('—', '-');
            JLabel bcLabel = new JLabel(cleanBc.toUpperCase(), SwingConstants.CENTER);
            bcLabel.setFont(FONT_SMALL_BOLD);
            bcLabel.setForeground(ACCENT_CYAN);
            bcLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            bcLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
            header.add(bcLabel);
            header.add(Box.createVerticalStrut(2));
        }

        JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
        titleLabel.setFont(FONT_TITLE_LARGE);
        titleLabel.setForeground(TEXT_WHITE);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        titleLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        header.add(titleLabel);

        if (subtitle != null && !subtitle.isEmpty()) {
            header.add(Box.createVerticalStrut(4));
            String cleanSub = subtitle.replace('–', '-').replace('—', '-');
            JLabel subLabel = new JLabel(cleanSub, SwingConstants.CENTER);
            subLabel.setFont(FONT_BODY);
            subLabel.setForeground(TEXT_MUTED);
            subLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            subLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
            header.add(subLabel);
        }

        return header;
    }

    /**
     * Creates a two-column detail row for summaries and receipts.
     */
    public static JPanel createDetailRow(String label, String value, Color valueColor) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));

        JLabel l = new JLabel(label);
        l.setFont(FONT_BODY);
        l.setForeground(TEXT_MUTED);

        JLabel v = new JLabel(value);
        v.setFont(FONT_BODY_BOLD);
        v.setForeground(valueColor != null ? valueColor : TEXT_WHITE);

        row.add(l, BorderLayout.WEST);
        row.add(v, BorderLayout.EAST);
        return row;
    }

    /**
     * Creates a status pill badge with icon indicator.
     */
    public static JLabel createStatusBadge(String text, Color bg, Color fg) {
        JLabel badge = new JLabel(" " + text + " ") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        badge.setFont(FONT_SMALL_BOLD);
        badge.setForeground(fg);
        badge.setOpaque(false);
        return badge;
    }

    /**
     * Currency formatter helper.
     */
    public static String formatCurrency(BigDecimal amount) {
        if (amount == null) return "$0.00";
        return String.format("$%,.2f", amount);
    }

    public static String formatCurrency(double amount) {
        return String.format("$%,.2f", amount);
    }
}
