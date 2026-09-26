package com.atm.ui.screens;

import com.atm.model.Transaction;
import com.atm.service.AtmSession;
import com.atm.ui.ModernButton;
import com.atm.ui.ScreenManager;
import com.atm.ui.UITheme;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MiniStatementScreen extends JPanel implements ScreenManager.RefreshableScreen {
    private final ScreenManager screenManager;

    private JTable txTable;
    private DefaultTableModel tableModel;
    private JLabel accountSummaryLabel;
    private JLabel inflowLabel;
    private JLabel outflowLabel;
    private List<Transaction> allAccountTransactions = new ArrayList<>();
    private String currentFilter = "ALL";

    public MiniStatementScreen(ScreenManager screenManager) {
        this.screenManager = screenManager;
        setLayout(new BorderLayout(14, 14));
        setBackground(UITheme.SCREEN_BG);
        setBorder(BorderFactory.createEmptyBorder(18, 30, 18, 30));

        initComponents();
    }

    private void initComponents() {
        // Top Header
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.setOpaque(false);

        JPanel header = UITheme.createScreenHeader(
                "TRANSACTION HISTORY  •  AUDIT LEDGER",
                "Account Statement & Activity",
                "Chronological record of recent deposits, withdrawals, and wire transfers"
        );
        topPanel.add(header);
        topPanel.add(Box.createVerticalStrut(10));

        // Sub-bar with account summary + category filter buttons
        JPanel filterRow = new JPanel(new BorderLayout());
        filterRow.setOpaque(false);
        filterRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        accountSummaryLabel = new JLabel("Account: ACC-1001-8842  |  Current Balance: $0.00");
        accountSummaryLabel.setFont(UITheme.FONT_BODY_BOLD);
        accountSummaryLabel.setForeground(UITheme.TEXT_WHITE);
        accountSummaryLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 16));
        filterRow.add(accountSummaryLabel, BorderLayout.WEST);

        // Filter Pills
        JPanel pills = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        pills.setOpaque(false);
        JButton btnAll = UITheme.createPillButton("All", e -> filterTransactions("ALL"));
        JButton btnWithdraw = UITheme.createPillButton("Withdrawals", e -> filterTransactions("WITHDRAWAL"));
        JButton btnDeposit = UITheme.createPillButton("Deposits", e -> filterTransactions("DEPOSIT"));
        JButton btnTransfer = UITheme.createPillButton("Transfers", e -> filterTransactions("TRANSFER"));

        pills.add(btnAll);
        pills.add(btnWithdraw);
        pills.add(btnDeposit);
        pills.add(btnTransfer);
        filterRow.add(pills, BorderLayout.EAST);

        topPanel.add(filterRow);
        add(topPanel, BorderLayout.NORTH);

        // Center Table
        String[] columnNames = {"Tx ID", "Date / Time", "Type", "Amount", "Balance After", "Status", "Description"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        txTable = new JTable(tableModel);
        txTable.setFont(UITheme.FONT_MONO);
        txTable.setRowHeight(30);
        txTable.setBackground(UITheme.SCREEN_CARD);
        txTable.setForeground(UITheme.TEXT_WHITE);
        txTable.setGridColor(new Color(36, 54, 94));
        txTable.setSelectionBackground(new Color(30, 58, 138));
        txTable.setSelectionForeground(Color.WHITE);

        JTableHeader tableHeader = txTable.getTableHeader();
        tableHeader.setFont(UITheme.FONT_BODY_BOLD);
        tableHeader.setBackground(new Color(22, 33, 56));
        tableHeader.setForeground(UITheme.ACCENT_CYAN);
        tableHeader.setBorder(BorderFactory.createLineBorder(UITheme.CARD_BORDER));

        // Custom Cell Renderer
        DefaultTableCellRenderer customRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? UITheme.SCREEN_CARD : new Color(15, 25, 48));
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
        txTable.getColumnModel().getColumn(0).setPreferredWidth(125); // ID
        txTable.getColumnModel().getColumn(1).setPreferredWidth(135); // Date
        txTable.getColumnModel().getColumn(2).setPreferredWidth(110); // Type
        txTable.getColumnModel().getColumn(3).setPreferredWidth(95);  // Amount
        txTable.getColumnModel().getColumn(4).setPreferredWidth(105); // Balance
        txTable.getColumnModel().getColumn(5).setPreferredWidth(80);  // Status
        txTable.getColumnModel().getColumn(6).setPreferredWidth(160); // Remarks

        JScrollPane scrollPane = new JScrollPane(txTable);
        scrollPane.getViewport().setBackground(UITheme.SCREEN_BG);
        scrollPane.setBorder(BorderFactory.createLineBorder(UITheme.CARD_BORDER, 1));

        // Center Panel with Table + Financial Inflow/Outflow Summary
        JPanel centerWrap = new JPanel(new BorderLayout(0, 8));
        centerWrap.setOpaque(false);
        centerWrap.add(scrollPane, BorderLayout.CENTER);

        // Inflow / Outflow Summary strip
        JPanel summaryStrip = UITheme.createCardPanel(10);
        summaryStrip.setLayout(new BorderLayout());

        JPanel totalsStack = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 0));
        totalsStack.setOpaque(false);

        inflowLabel = new JLabel("Total Credits: +$0.00");
        inflowLabel.setFont(UITheme.FONT_BODY_BOLD);
        inflowLabel.setForeground(UITheme.SUCCESS_GREEN);
        inflowLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));

        outflowLabel = new JLabel("Total Debits: -$0.00");
        outflowLabel.setFont(UITheme.FONT_BODY_BOLD);
        outflowLabel.setForeground(UITheme.DANGER_RED);
        outflowLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 8));

        totalsStack.add(inflowLabel);
        totalsStack.add(outflowLabel);

        JLabel selectHint = new JLabel("Click any transaction row to select it for receipt printing");
        selectHint.setFont(UITheme.FONT_SMALL);
        selectHint.setForeground(UITheme.TEXT_MUTED);

        summaryStrip.add(selectHint, BorderLayout.WEST);
        summaryStrip.add(totalsStack, BorderLayout.EAST);
        centerWrap.add(summaryStrip, BorderLayout.SOUTH);

        add(centerWrap, BorderLayout.CENTER);

        // Bottom Navigation
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        bottomPanel.setOpaque(false);

        JButton backBtn = UITheme.createModernButton("⬅ Back to Menu", UITheme.CHASSIS_BG, UITheme.TEXT_WHITE);
        backBtn.setPreferredSize(new Dimension(170, 42));
        backBtn.addActionListener(e -> screenManager.showScreen("MAIN_MENU"));

        JButton printSlipBtn = UITheme.createModernButton("🖨 Print Selected Receipt", UITheme.ACCENT_BLUE, UITheme.TEXT_WHITE);
        printSlipBtn.setPreferredSize(new Dimension(210, 42));
        printSlipBtn.addActionListener(e -> printSelectedReceipt());

        bottomPanel.add(backBtn);
        bottomPanel.add(printSlipBtn);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void filterTransactions(String filter) {
        this.currentFilter = filter;
        tableModel.setRowCount(0);

        BigDecimal totalIn = BigDecimal.ZERO;
        BigDecimal totalOut = BigDecimal.ZERO;

        List<Transaction> filtered = allAccountTransactions.stream().filter(t -> {
            if ("ALL".equalsIgnoreCase(filter)) return true;
            if ("WITHDRAWAL".equalsIgnoreCase(filter)) return "WITHDRAWAL".equalsIgnoreCase(t.getTransactionType());
            if ("DEPOSIT".equalsIgnoreCase(filter)) return "DEPOSIT".equalsIgnoreCase(t.getTransactionType());
            if ("TRANSFER".equalsIgnoreCase(filter)) return t.getTransactionType().startsWith("TRANSFER");
            return true;
        }).collect(Collectors.toList());

        for (Transaction t : filtered) {
            String amtPrefix = "";
            if ("DEPOSIT".equals(t.getTransactionType()) || "TRANSFER_IN".equals(t.getTransactionType())) {
                amtPrefix = "+$";
                totalIn = totalIn.add(t.getAmount());
            } else if ("WITHDRAWAL".equals(t.getTransactionType()) || "TRANSFER_OUT".equals(t.getTransactionType())) {
                amtPrefix = "-$";
                totalOut = totalOut.add(t.getAmount());
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

        inflowLabel.setText("Total Credits: +" + UITheme.formatCurrency(totalIn));
        outflowLabel.setText("Total Debits: -" + UITheme.formatCurrency(totalOut));
    }

    private void printSelectedReceipt() {
        AtmSession session = screenManager.getSession();
        if (session == null) return;

        if (allAccountTransactions.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No transaction records found to print.", "NO TRANSACTIONS", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int selectedRow = txTable.getSelectedRow();
        Transaction targetTx = (selectedRow >= 0 && selectedRow < allAccountTransactions.size()) ? allAccountTransactions.get(selectedRow) : allAccountTransactions.get(0);
        new ReceiptPopup(screenManager.getFrame(), session, targetTx, "ATM Thermal Receipt").setVisible(true);
    }

    @Override
    public void refreshScreen() {
        AtmSession session = screenManager.getSession();
        if (session != null) {
            accountSummaryLabel.setText("Account: " + session.getAccount().getAccountNumber() +
                    "  |  Current Balance: " + UITheme.formatCurrency(session.getAccount().getBalance()));

            allAccountTransactions = screenManager.getAtmService().getMiniStatement(session, 25);
            filterTransactions(currentFilter);
        }
    }
}
