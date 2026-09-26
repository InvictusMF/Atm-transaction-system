package com.atm.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

/**
 * ModernButton is a custom-painted Swing button designed for high visual appeal,
 * crisp anti-aliased typography, rich gradients, tactile feedback, and total
 * immunity to Windows Look-and-Feel white-out glitches.
 */
public class ModernButton extends JButton {

    public enum ButtonType {
        PRIMARY,    // Electric Royal Blue
        SUCCESS,    // Emerald Green
        DANGER,     // Crimson Red
        WARNING,    // Amber Gold
        SECONDARY,  // Dark Slate
        KEYPAD_NUM, // Brushed Metallic Keypad
        PILL,       // Compact Capsule (Quick amounts)
        CARD,       // Interactive Menu / Account Card
        CUSTOM      // Custom colors
    }

    public enum IconType {
        NONE,
        BACK,
        CHECK,
        CANCEL,
        CLEAR,
        PRINT,
        CASH,
        DATABASE,
        PDF,
        REFRESH,
        CARD,
        SHIELD,
        ENTER,
        TRANSFER,
        DEPOSIT,
        HISTORY,
        LOGOUT,
        LOCK,
        INFO
    }

    private ButtonType buttonType;
    private IconType iconType = IconType.NONE;
    private Color customBg;
    private Color customFg;
    private Color accentColor;
    private String subtitleText;
    private int cornerRadius = 10;
    private boolean isHovered = false;
    private boolean isPressed = false;

    public ModernButton(String text, ButtonType type) {
        super(text);
        this.buttonType = type;
        initButton();
    }

    public ModernButton(String text, ButtonType type, IconType icon) {
        super(text);
        this.buttonType = type;
        this.iconType = icon;
        initButton();
    }

    public ModernButton(String text, Color bg, Color fg) {
        super(text);
        this.buttonType = ButtonType.CUSTOM;
        this.customBg = bg;
        this.customFg = fg;
        initButton();
    }

    public ModernButton(String text, Color bg, Color fg, IconType icon) {
        super(text);
        this.buttonType = ButtonType.CUSTOM;
        this.customBg = bg;
        this.customFg = fg;
        this.iconType = icon;
        initButton();
    }

    private void initButton() {
        setOpaque(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Default fonts and paddings based on type
        switch (buttonType) {
            case PRIMARY:
            case SUCCESS:
            case DANGER:
            case SECONDARY:
                setFont(UITheme.FONT_SUBTITLE);
                setForeground(Color.WHITE);
                setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
                cornerRadius = 10;
                break;
            case WARNING:
                setFont(UITheme.FONT_SUBTITLE);
                setForeground(new Color(15, 23, 42)); // High-contrast deep navy on gold
                setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
                cornerRadius = 10;
                break;
            case KEYPAD_NUM:
                setFont(UITheme.FONT_KEYPAD);
                setForeground(Color.WHITE);
                setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
                cornerRadius = 8;
                break;
            case PILL:
                setFont(UITheme.FONT_BODY_BOLD);
                setForeground(UITheme.ACCENT_CYAN);
                setBorder(BorderFactory.createEmptyBorder(6, 16, 6, 16));
                cornerRadius = 18;
                break;
            case CARD:
                setFont(UITheme.FONT_SUBTITLE);
                setForeground(Color.WHITE);
                setHorizontalAlignment(SwingConstants.LEFT);
                setBorder(BorderFactory.createEmptyBorder(12, 18, 12, 18));
                cornerRadius = 12;
                break;
            case CUSTOM:
                setFont(UITheme.FONT_SUBTITLE);
                if (customFg != null) setForeground(customFg);
                setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
                cornerRadius = 10;
                break;
        }

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                isHovered = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                isHovered = false;
                isPressed = false;
                repaint();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                isPressed = true;
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                isPressed = false;
                repaint();
            }
        });
    }

    public void setSubtitle(String subtitle) {
        this.subtitleText = subtitle;
        repaint();
    }

    public void setAccentColor(Color color) {
        this.accentColor = color;
        repaint();
    }

    public void setCornerRadius(int radius) {
        this.cornerRadius = radius;
        repaint();
    }

    public void setIconType(IconType icon) {
        this.iconType = icon;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        int w = getWidth();
        int h = getHeight();

        // Determine gradient stops based on button type
        Color topColor;
        Color bottomColor;
        Color borderColor;

        switch (buttonType) {
            case PRIMARY:
                topColor = isHovered ? new Color(59, 130, 246) : new Color(37, 99, 235);
                bottomColor = isHovered ? new Color(37, 99, 235) : new Color(29, 78, 216);
                borderColor = isHovered ? new Color(147, 197, 253) : new Color(59, 130, 246);
                break;
            case SUCCESS:
                topColor = isHovered ? new Color(34, 197, 94) : new Color(16, 185, 129);
                bottomColor = isHovered ? new Color(22, 163, 74) : new Color(5, 150, 105);
                borderColor = isHovered ? new Color(134, 239, 172) : new Color(34, 197, 94);
                break;
            case DANGER:
                topColor = isHovered ? new Color(248, 113, 113) : new Color(239, 68, 68);
                bottomColor = isHovered ? new Color(220, 38, 38) : new Color(185, 28, 28);
                borderColor = isHovered ? new Color(252, 165, 165) : new Color(248, 113, 113);
                break;
            case WARNING:
                topColor = isHovered ? new Color(252, 211, 77) : new Color(245, 158, 11);
                bottomColor = isHovered ? new Color(245, 158, 11) : new Color(217, 119, 6);
                borderColor = isHovered ? new Color(254, 240, 138) : new Color(251, 191, 36);
                break;
            case SECONDARY:
                topColor = isHovered ? new Color(71, 85, 105) : new Color(51, 65, 85);
                bottomColor = isHovered ? new Color(51, 65, 85) : new Color(30, 41, 59);
                borderColor = isHovered ? new Color(148, 163, 184) : new Color(71, 85, 105);
                break;
            case KEYPAD_NUM:
                topColor = isHovered ? new Color(71, 85, 105) : new Color(51, 65, 85);
                bottomColor = isHovered ? new Color(51, 65, 85) : new Color(30, 41, 59);
                borderColor = isHovered ? new Color(125, 211, 252) : new Color(71, 85, 105);
                break;
            case PILL:
                topColor = isHovered ? new Color(51, 65, 85) : new Color(30, 41, 59);
                bottomColor = isHovered ? new Color(30, 41, 59) : new Color(15, 23, 42);
                borderColor = isHovered ? UITheme.ACCENT_CYAN : new Color(56, 189, 248, 140);
                break;
            case CARD:
                topColor = isHovered ? new Color(40, 56, 84) : new Color(25, 38, 64);
                bottomColor = isHovered ? new Color(25, 38, 64) : new Color(15, 23, 42);
                borderColor = isHovered ? (accentColor != null ? accentColor : UITheme.ACCENT_CYAN) : (accentColor != null ? new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 140) : new Color(51, 65, 85));
                break;
            case CUSTOM:
            default:
                Color base = customBg != null ? customBg : new Color(37, 99, 235);
                topColor = isHovered ? base.brighter() : base;
                bottomColor = isHovered ? base : base.darker();
                borderColor = isHovered ? topColor.brighter() : topColor;
                break;
        }

        // Shift on press for mechanical click feel
        if (isPressed) {
            topColor = bottomColor;
            bottomColor = bottomColor.darker();
        }

        // Draw smooth rounded background
        RoundRectangle2D.Float shape = new RoundRectangle2D.Float(1, 1, w - 2, h - 2, cornerRadius, cornerRadius);
        g2.setPaint(new GradientPaint(0, 0, topColor, 0, h, bottomColor));
        g2.fill(shape);

        // Specular highlight line on top edge for modern glossy elevation
        g2.setColor(new Color(255, 255, 255, isHovered ? 45 : 25));
        g2.drawRoundRect(2, 2, w - 4, Math.max(1, h / 2), cornerRadius, cornerRadius);

        // Optional left accent stripe for CARD buttons
        if (buttonType == ButtonType.CARD && accentColor != null) {
            g2.setColor(accentColor);
            g2.fillRoundRect(3, 4, 5, Math.max(1, h - 8), 4, 4);
        }

        // Draw crisp border
        g2.setColor(borderColor);
        g2.setStroke(new BasicStroke(isHovered ? 1.8f : 1.2f));
        g2.draw(shape);

        // Draw Text & Built-in Vector Icon
        int textOffset = isPressed ? 1 : 0;
        int currentX = (buttonType == ButtonType.CARD && accentColor != null) ? 20 : 12;

        // Draw Icon if specified
        if (iconType != IconType.NONE) {
            int iconSize = 14;
            int iconY = (h - iconSize) / 2 + textOffset;
            int iconX = currentX;

            Color iconColor = getForeground();
            if (buttonType == ButtonType.WARNING) {
                iconColor = new Color(15, 23, 42);
            }
            drawVectorIcon(g2, iconType, iconX, iconY, iconSize, iconColor);
            currentX += iconSize + 10;
        }

        // Text rendering
        String text = getText();
        if (text != null && !text.isEmpty()) {
            FontMetrics fm = g2.getFontMetrics(getFont());
            g2.setFont(getFont());
            g2.setColor(getForeground());

            if (buttonType == ButtonType.CARD) {
                // Dual-line rendering: Title + Subtitle
                int titleY = subtitleText != null && !subtitleText.isEmpty() ? (h / 2) - 2 + textOffset : (h + fm.getAscent() - fm.getDescent()) / 2 + textOffset;
                g2.drawString(text, currentX, titleY);

                if (subtitleText != null && !subtitleText.isEmpty()) {
                    g2.setFont(UITheme.FONT_BODY);
                    g2.setColor(new Color(148, 163, 184)); // Slate 400
                    g2.drawString(subtitleText, currentX, titleY + 16);
                }

                // Draw right chevron arrow for navigation hint
                g2.setColor(isHovered ? UITheme.ACCENT_CYAN : new Color(100, 116, 139));
                drawVectorIcon(g2, IconType.ENTER, w - 24, (h - 12) / 2 + textOffset, 12, isHovered ? UITheme.ACCENT_CYAN : new Color(100, 116, 139));

            } else {
                // Centered button label (with icon offset considered if present)
                int textW = fm.stringWidth(text);
                int textX;
                if (iconType != IconType.NONE) {
                    textX = currentX;
                    int totalContentW = (currentX - 12) + textW;
                    if (totalContentW < w) {
                        textX = (w - textW + 14) / 2;
                    }
                } else if (getHorizontalAlignment() == SwingConstants.LEFT) {
                    textX = 16;
                } else {
                    textX = (w - textW) / 2;
                }
                int textY = (h + fm.getAscent() - fm.getDescent()) / 2 + textOffset;

                // Subtle shadow for extra depth
                if (buttonType != ButtonType.WARNING) {
                    g2.setColor(new Color(0, 0, 0, 80));
                    g2.drawString(text, textX, textY + 1);
                }

                g2.setColor(getForeground());
                g2.drawString(text, textX, textY);
            }
        }

        g2.dispose();
    }

    private void drawVectorIcon(Graphics2D g2, IconType type, int x, int y, int size, Color color) {
        g2.setColor(color);
        g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        switch (type) {
            case BACK:
                // Left arrow: <--
                g2.drawLine(x + size - 2, y + size / 2, x + 2, y + size / 2);
                g2.drawLine(x + 6, y + 2, x + 2, y + size / 2);
                g2.drawLine(x + 6, y + size - 2, x + 2, y + size / 2);
                break;
            case CHECK:
                // Checkmark: v/
                g2.drawLine(x + 2, y + size / 2, x + size / 3 + 1, y + size - 2);
                g2.drawLine(x + size / 3 + 1, y + size - 2, x + size - 1, y + 2);
                break;
            case CANCEL:
                // Cross: X
                g2.drawLine(x + 2, y + 2, x + size - 2, y + size - 2);
                g2.drawLine(x + size - 2, y + 2, x + 2, y + size - 2);
                break;
            case CLEAR:
                // Backspace badge with X
                Path2D.Float p = new Path2D.Float();
                p.moveTo(x + 4, y + 2);
                p.lineTo(x + size - 1, y + 2);
                p.lineTo(x + size - 1, y + size - 2);
                p.lineTo(x + 4, y + size - 2);
                p.lineTo(x, y + size / 2);
                p.closePath();
                g2.draw(p);
                g2.drawLine(x + 6, y + 5, x + size - 4, y + size - 5);
                g2.drawLine(x + size - 4, y + 5, x + 6, y + size - 5);
                break;
            case PRINT:
                // Thermal printer: body + paper feed
                g2.drawRoundRect(x + 1, y + 4, size - 2, size - 6, 3, 3);
                g2.drawLine(x + 3, y + 4, x + 3, y + 1);
                g2.drawLine(x + 3, y + 1, x + size - 4, y + 1);
                g2.drawLine(x + size - 4, y + 1, x + size - 4, y + 4);
                g2.drawLine(x + 4, y + size - 2, x + size - 4, y + size - 2);
                break;
            case CASH:
                // Banknote outline with dollar sign
                g2.drawRoundRect(x, y + 2, size, size - 4, 3, 3);
                g2.drawOval(x + size / 2 - 2, y + size / 2 - 2, 4, 4);
                break;
            case DATABASE:
                // Cylinder
                g2.drawOval(x + 1, y + 1, size - 2, 4);
                g2.drawLine(x + 1, y + 3, x + 1, y + size - 3);
                g2.drawLine(x + size - 1, y + 3, x + size - 1, y + size - 3);
                g2.drawArc(x + 1, y + size - 6, size - 2, 5, 180, 180);
                break;
            case PDF:
                // Document sheet with folded corner
                g2.drawRect(x + 2, y + 1, size - 4, size - 2);
                g2.drawLine(x + 4, y + 5, x + size - 4, y + 5);
                g2.drawLine(x + 4, y + 8, x + size - 4, y + 8);
                break;
            case REFRESH:
                // Circular arc with arrow
                g2.drawArc(x + 1, y + 1, size - 2, size - 2, 45, 270);
                g2.drawLine(x + size - 3, y + size / 2, x + size - 3, y + 2);
                g2.drawLine(x + size - 3, y + 2, x + size / 2 + 1, y + 2);
                break;
            case CARD:
                // Credit card
                g2.drawRoundRect(x, y + 2, size, size - 4, 2, 2);
                g2.drawLine(x, y + 5, x + size, y + 5);
                break;
            case SHIELD:
                // Bank Shield
                Path2D.Float s = new Path2D.Float();
                s.moveTo(x + size / 2, y);
                s.lineTo(x + size - 1, y + 3);
                s.lineTo(x + size - 1, y + size / 2);
                s.curveTo(x + size - 1, y + size - 2, x + size / 2, y + size, x + size / 2, y + size);
                s.curveTo(x + size / 2, y + size, x + 1, y + size - 2, x + 1, y + size / 2);
                s.lineTo(x + 1, y + 3);
                s.closePath();
                g2.draw(s);
                break;
            case ENTER:
                // Right chevron: >
                g2.drawLine(x + 2, y + 2, x + size - 3, y + size / 2);
                g2.drawLine(x + size - 3, y + size / 2, x + 2, y + size - 2);
                break;
            case TRANSFER:
                // Double horizontal exchange arrows
                g2.drawLine(x + 1, y + 4, x + size - 2, y + 4);
                g2.drawLine(x + size - 5, y + 1, x + size - 2, y + 4);
                g2.drawLine(x + 1, y + size - 4, x + size - 2, y + size - 4);
                g2.drawLine(x + 4, y + size - 1, x + 1, y + size - 4);
                break;
            case DEPOSIT:
                // Tray with down arrow
                g2.drawRoundRect(x + 1, y + size / 2, size - 2, size / 2 - 1, 2, 2);
                g2.drawLine(x + size / 2, y + 1, x + size / 2, y + size - 4);
                g2.drawLine(x + size / 2 - 3, y + size - 7, x + size / 2, y + size - 4);
                g2.drawLine(x + size / 2 + 3, y + size - 7, x + size / 2, y + size - 4);
                break;
            case HISTORY:
                // Clock face
                g2.drawOval(x + 1, y + 1, size - 2, size - 2);
                g2.drawLine(x + size / 2, y + size / 2, x + size / 2, y + 4);
                g2.drawLine(x + size / 2, y + size / 2, x + size - 4, y + size / 2);
                break;
            case LOGOUT:
                // Door exit arrow
                g2.drawRect(x + 1, y + 1, size / 2, size - 2);
                g2.drawLine(x + size / 3, y + size / 2, x + size - 1, y + size / 2);
                g2.drawLine(x + size - 4, y + size / 2 - 3, x + size - 1, y + size / 2);
                g2.drawLine(x + size - 4, y + size / 2 + 3, x + size - 1, y + size / 2);
                break;
            case LOCK:
                // Padlock
                g2.drawRoundRect(x + 2, y + size / 2 - 1, size - 4, size / 2, 2, 2);
                g2.drawArc(x + 4, y + 1, size - 8, size / 2 + 2, 0, 180);
                break;
            case INFO:
                // Circle with 'i'
                g2.drawOval(x + 1, y + 1, size - 2, size - 2);
                g2.fillRect(x + size / 2 - 1, y + 4, 2, 2);
                g2.drawLine(x + size / 2, y + 7, x + size / 2, y + size - 4);
                break;
            case NONE:
            default:
                break;
        }
    }
}
