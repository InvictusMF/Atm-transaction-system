package com.atm.ui.screens;

import com.atm.model.Transaction;
import com.atm.service.AtmSession;
import com.atm.ui.ScreenManager;
import com.atm.ui.UITheme;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.math.BigDecimal;
import java.util.List;

public class MiniStatementScreen extends JPanel implements ScreenManager.RefreshableScreen {
    private final ScreenManager screenManager;

    private JTable txTable;
    private DefaultTableModel tableModel;
    private JLabel accountSummaryLabel;

    public MiniStatementScreen(ScreenManager screenManager) {
        this.screenManager = screenManager;
        setLayout(new BorderLayout(15, 15));
        setBackground(UITheme.SCREEN_BG);
        setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        initComponents();
    }

    private void initComponents() {
        // Header
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setBackground(UITheme.SCREEN_BG);

        JLabel title = new JLabel("MINI-STATEMENT & TRANSACTION LEDGER", SwingConstants.CENTER);
        title.setFont(UITheme.FONT_TITLE_LARGE);
        title.setForeground(UITheme.ACCENT_CYAN);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        accountSummaryLabel = new JLabel("Recent Account Activity", SwingConstants.CENTER);
        accountSummaryLabel.setFont(UITheme.FONT_SUBTITLE);
        accountSummaryLabel.setForeground(UITheme.TEXT_WHITE);
        accountSummaryLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        headerPanel.add(title);
        headerPanel.add(Box.createVerticalStrut(4));
        headerPanel.add(accountSummaryLabel);
        add(headerPanel, BorderLayout.NORTH);

        // Center Table
        String[] columnNames = {"Tx ID", "Date / Time", "Type", "Amount", "Balance After", "Status", "Remarks"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        txTable = new JTable(tableModel);
        txTable.setFont(UITheme.FONT_MONO);
        txTable.setRowHeight(28);
        txTable.setBackground(UITheme.SCREEN_CARD);
        txTable.setForeground(UITheme.TEXT_WHITE);
        txTable.setGridColor(new Color(40, 60, 95));
        txTable.setSelectionBackground(new Color(30, 58, 138));
        txTable.setSelectionForeground(Color.WHITE);

        JTableHeader tableHeader = txTable.getTableHeader();
        tableHeader.setFont(UITheme.FONT_BODY_BOLD);
        tableHeader.setBackground(new Color(30, 41, 59));
        tableHeader.setForeground(UITheme.ACCENT_CYAN);
        tableHeader.setBorder(BorderFactory.createLineBorder(new Color(51, 65, 85)));

        // Custom Cell Renderer for Type & Amount Color Coding
        DefaultTableCellRenderer customRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? UITheme.SCREEN_CARD : new Color(17, 28, 50));
                    c.setForeground(UITheme.TEXT_WHITE);

                    if (column == 3) { // Amount
                        String val = String.valueOf(value);
                        if (val.startsWith("+")) {
                            c.setForeground(UITheme.SUCCESS_GREEN);
                        } else if (val.startsWith("-")) {
                            c.setForeground(UITheme.DANGER_RED);
                        }
                    } else if (column == 2) { // Type
                        c.setForeground(UITheme.TEXT_CYAN);
                    } else if (column == 5) { // Status
                        c.setForeground(UITheme.SUCCESS_GREEN);
                    }
                }
                return c;
            }
        };

        for (int i = 0; i < txTable.getColumnCount(); i++) {
            txTable.getColumnModel().getColumn(i).setCellRenderer(customRenderer);
        }

        // Adjust column widths
        txTable.getColumnModel().getColumn(0).setPreferredWidth(120); // ID
        txTable.getColumnModel().getColumn(1).setPreferredWidth(130); // Date
        txTable.getColumnModel().getColumn(2).setPreferredWidth(110); // Type
        txTable.getColumnModel().getColumn(3).setPreferredWidth(90);  // Amount
        txTable.getColumnModel().getColumn(4).setPreferredWidth(100); // Balance
        txTable.getColumnModel().getColumn(5).setPreferredWidth(80);  // Status
        txTable.getColumnModel().getColumn(6).setPreferredWidth(160); // Remarks

        JScrollPane scrollPane = new JScrollPane(txTable);
        scrollPane.getViewport().setBackground(UITheme.SCREEN_BG);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(40, 60, 95), 1));
        add(scrollPane, BorderLayout.CENTER);

        // Bottom Controls
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        bottomPanel.setBackground(UITheme.SCREEN_BG);

        JButton backBtn = UITheme.createModernButton("⬅ Back to Menu", UITheme.CHASSIS_BG, UITheme.TEXT_WHITE);
        backBtn.addActionListener(e -> screenManager.showScreen("MAIN_MENU"));

        JButton printSlipBtn = UITheme.createModernButton("🖨 Print Thermal Receipt", UITheme.ACCENT_BLUE, UITheme.TEXT_WHITE);
        printSlipBtn.addActionListener(e -> printSelectedReceipt());

        bottomPanel.add(backBtn);
        bottomPanel.add(printSlipBtn);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void printSelectedReceipt() {
        AtmSession session = screenManager.getSession();
        if (session == null) return;

        List<Transaction> txs = screenManager.getAtmService().getMiniStatement(session, 10);
        if (txs.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No transaction history found to print.", "NO TRANSACTIONS", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int selectedRow = txTable.getSelectedRow();
        Transaction targetTx = (selectedRow >= 0 && selectedRow < txs.size()) ? txs.get(selectedRow) : txs.get(0);
        new ReceiptPopup(screenManager.getFrame(), session, targetTx, "ATM Thermal Receipt").setVisible(true);
    }

    @Override
    public void refreshScreen() {
        AtmSession session = screenManager.getSession();
        tableModel.setRowCount(0);

        if (session != null) {
            accountSummaryLabel.setText("Account: " + session.getAccount().getAccountNumber() +
                    "  |  Current Balance: $" + String.format("%.2f", session.getAccount().getBalance()));

            List<Transaction> txs = screenManager.getAtmService().getMiniStatement(session, 15);
            for (Transaction t : txs) {
                String amtPrefix = "";
                if ("DEPOSIT".equals(t.getTransactionType()) || "TRANSFER_IN".equals(t.getTransactionType())) {
                    amtPrefix = "+$";
                } else if ("WITHDRAWAL".equals(t.getTransactionType()) || "TRANSFER_OUT".equals(t.getTransactionType())) {
                    amtPrefix = "-$";
                } else {
                    amtPrefix = "$";
                }

                tableModel.addRow(new Object[]{
                        t.getTransactionId(),
                        t.getFormattedDate(),
                        t.getTransactionType(),
                        amtPrefix + String.format("%.2f", t.getAmount()),
                        "$" + String.format("%.2f", t.getBalanceAfter()),
                        t.getStatus(),
                        t.getRemarks() != null ? t.getRemarks() : ""
                });
            }
        }
    }
}
