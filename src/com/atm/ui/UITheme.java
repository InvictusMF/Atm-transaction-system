package com.atm.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class UITheme {
    // Primary Color Palette
    public static final Color BG_DARK = new Color(15, 23, 42);          // Deep Slate 900
    public static final Color CHASSIS_BG = new Color(30, 41, 59);        // Slate 800
    public static final Color SCREEN_BG = new Color(11, 20, 38);         // ATM Midnight Display
    public static final Color SCREEN_CARD = new Color(22, 36, 64);       // Elevated Card Blue
    public static final Color ACCENT_CYAN = new Color(56, 189, 248);     // Electric Cyan 400
    public static final Color ACCENT_BLUE = new Color(37, 99, 235);      // Royal Blue 600
    public static final Color ACCENT_GOLD = new Color(251, 191, 36);     // Amber Gold 400
    public static final Color SUCCESS_GREEN = new Color(34, 197, 94);    // Emerald 500
    public static final Color DANGER_RED = new Color(239, 68, 68);       // Crimson 500
    public static final Color WARNING_YELLOW = new Color(245, 158, 11);  // Amber 500

    // Text Colors
    public static final Color TEXT_WHITE = new Color(248, 250, 252);
    public static final Color TEXT_MUTED = new Color(148, 163, 184);
    public static final Color TEXT_CYAN = new Color(125, 211, 252);

    // Fonts
    public static final Font FONT_TITLE_LARGE = new Font("Segoe UI", Font.BOLD, 24);
    public static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 18);
    public static final Font FONT_SUBTITLE = new Font("Segoe UI", Font.BOLD, 14);
    public static final Font FONT_BODY = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_BODY_BOLD = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONT_KEYPAD = new Font("Segoe UI", Font.BOLD, 18);
    public static final Font FONT_MONO = new Font("Consolas", Font.PLAIN, 12);
    public static final Font FONT_MONO_BOLD = new Font("Consolas", Font.BOLD, 13);

    /**
     * Creates an ultra-modern, anti-aliased button with gradient styling and vector icons.
     * Automatically parses legacy unicode emoji/symbols into vector icons to avoid missing-glyph boxes.
     */
    public static JButton createModernButton(String text, Color bgColor, Color fgColor) {
        ModernButton.IconType icon = ModernButton.IconType.NONE;
        String cleanText = text;

        if (text != null) {
            if (text.contains("⬅") || text.contains("Back") || text.contains("←")) {
                icon = ModernButton.IconType.BACK;
                cleanText = text.replace("⬅", "").replace("←", "").trim();
            } else if (text.contains("✔") || text.contains("Accept") || text.contains("Submit") || text.contains("Confirm") || text.contains("Withdraw Cash")) {
                icon = ModernButton.IconType.CHECK;
                cleanText = text.replace("✔", "").trim();
            } else if (text.contains("✖") || text.contains("Cancel") || text.contains("Eject") || text.contains("✕")) {
                icon = ModernButton.IconType.CANCEL;
                cleanText = text.replace("✖", "").replace("✕", "").trim();
            } else if (text.contains("⌫") || text.contains("Clear") || text.contains("Reset")) {
                if (text.contains("↺")) {
                    icon = ModernButton.IconType.REFRESH;
                    cleanText = text.replace("↺", "").trim();
                } else {
                    icon = ModernButton.IconType.CLEAR;
                    cleanText = text.replace("⌫", "").trim();
                }
            } else if (text.contains("🖨") || text.contains("Print")) {
                icon = ModernButton.IconType.PRINT;
                cleanText = text.replace("🖨", "").trim();
            } else if (text.contains("🗄") || text.contains("Database")) {
                icon = ModernButton.IconType.DATABASE;
                cleanText = text.replace("🗄", "").trim();
            } else if (text.contains("📄") || text.contains("PDF")) {
                icon = ModernButton.IconType.PDF;
                cleanText = text.replace("📄", "").trim();
            } else if (text.contains("💵")) {
                icon = ModernButton.IconType.CASH;
                cleanText = text.replace("💵", "").trim();
            } else if (text.contains("💳")) {
                icon = ModernButton.IconType.CARD;
                cleanText = text.replace("💳", "").trim();
            }
        }

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
        JPanel panel = new JPanel();
        panel.setBackground(SCREEN_CARD);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(40, 60, 95), 1, true),
                BorderFactory.createEmptyBorder(16, 16, 16, 16)
        ));
        return panel;
    }
}
