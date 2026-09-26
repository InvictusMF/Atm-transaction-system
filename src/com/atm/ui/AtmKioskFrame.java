package com.atm.ui;

import com.atm.db.DatabaseManager;
import com.atm.model.*;
import com.atm.pdf.PdfReportGenerator;
import com.atm.service.AtmSession;
import com.atm.ui.screens.*;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;

public class AtmKioskFrame extends JFrame {
    private final ScreenManager screenManager;
    private JLabel clockLabel;
    private JLabel terminalStatusLabel;
    private JLabel cardSlotLed;
    private JLabel cashSlotLabel;

    public AtmKioskFrame() {
        super("Apex National Bank - ATM Transaction Simulation System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 850);
        setMinimumSize(new Dimension(1000, 800));
        setLocationRelativeTo(null);

        this.screenManager = new ScreenManager(this);

        initChassisUI();
        registerAllScreens();

        // Start Live Clock
        Timer timer = new Timer(1000, e -> updateClock());
        timer.start();

        // Global Physical Keyboard Listener
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                char c = e.getKeyChar();
                if (Character.isDigit(c)) {
                    screenManager.forwardKeypadPress(String.valueOf(c));
                } else if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
                    screenManager.forwardKeypadClear();
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    screenManager.forwardKeypadCancel();
                } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    screenManager.forwardKeypadEnter();
                }
            }
        });
        setFocusable(true);

        // Show Welcome Screen initially
        screenManager.showScreen("WELCOME");
    }

    private void registerAllScreens() {
        screenManager.registerScreen("WELCOME", new WelcomeScreen(screenManager));
        screenManager.registerScreen("PIN_ENTRY", new PinEntryScreen(screenManager));
        screenManager.registerScreen("MAIN_MENU", new MainMenuScreen(screenManager));
        screenManager.registerScreen("FAST_CASH", new FastCashScreen(screenManager));
        screenManager.registerScreen("WITHDRAW", new WithdrawScreen(screenManager));
        screenManager.registerScreen("DEPOSIT", new DepositScreen(screenManager));
        screenManager.registerScreen("BALANCE", new BalanceScreen(screenManager));
        screenManager.registerScreen("TRANSFER", new TransferScreen(screenManager));
        screenManager.registerScreen("MINI_STATEMENT", new MiniStatementScreen(screenManager));
        screenManager.registerScreen("PIN_CHANGE", new PinChangeScreen(screenManager));
    }

    private void initChassisUI() {
        JPanel outerKiosk = new JPanel(new BorderLayout(10, 10));
        outerKiosk.setBackground(UITheme.CHASSIS_BG);
        outerKiosk.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(15, 23, 42), 6),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)
        ));

        // 1. Top Illuminated Marquee & Action Header
        JPanel topMarquee = createTopMarquee();
        outerKiosk.add(topMarquee, BorderLayout.NORTH);

        // 2. Center Touchscreen Monitor
        JPanel screenBezel = new JPanel(new BorderLayout());
        screenBezel.setBackground(new Color(5, 10, 20));
        screenBezel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(30, 58, 138), 3, true),
                BorderFactory.createEmptyBorder(6, 6, 6, 6)
        ));
        screenBezel.add(screenManager.getContainerPanel(), BorderLayout.CENTER);
        outerKiosk.add(screenBezel, BorderLayout.CENTER);

        // 3. Bottom Hardware Peripherals Console (Card slot, Cash slot, Receipt, Pinpad)
        JPanel bottomHardware = createBottomHardwareConsole();
        outerKiosk.add(bottomHardware, BorderLayout.SOUTH);

        setContentPane(outerKiosk);
    }

    private JPanel createTopMarquee() {
        JPanel marquee = new JPanel(new BorderLayout(15, 0));
        marquee.setBackground(new Color(15, 23, 42));
        marquee.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(51, 65, 85), 1, true),
                BorderFactory.createEmptyBorder(10, 16, 10, 16)
        ));

        // Left Branding
        JPanel brandPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        brandPanel.setBackground(new Color(15, 23, 42));

        JLabel logoIcon = new JLabel("", SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                java.awt.geom.Path2D.Float s = new java.awt.geom.Path2D.Float();
                s.moveTo(14, 2);
                s.lineTo(26, 6);
                s.lineTo(26, 16);
                s.curveTo(26, 23, 14, 28, 14, 28);
                s.curveTo(14, 28, 2, 23, 2, 16);
                s.lineTo(2, 6);
                s.closePath();
                g2.setPaint(new GradientPaint(0, 0, new Color(56, 189, 248), 28, 28, new Color(37, 99, 235)));
                g2.fill(s);
                g2.setColor(new Color(255, 255, 255, 180));
                g2.setStroke(new BasicStroke(1.2f));
                g2.draw(s);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
                g2.drawString("A", 10, 18);
                g2.dispose();
            }
        };
        logoIcon.setPreferredSize(new Dimension(28, 30));

        JPanel textStack = new JPanel();
        textStack.setLayout(new BoxLayout(textStack, BoxLayout.Y_AXIS));
        textStack.setBackground(new Color(15, 23, 42));

        JLabel bankTitle = new JLabel("APEX NATIONAL BANK");
        bankTitle.setFont(UITheme.FONT_TITLE);
        bankTitle.setForeground(UITheme.ACCENT_CYAN);

        terminalStatusLabel = new JLabel("TERMINAL #ATM-TERMINAL-01  |  ONLINE ●");
        terminalStatusLabel.setFont(UITheme.FONT_MONO);
        terminalStatusLabel.setForeground(UITheme.SUCCESS_GREEN);

        textStack.add(bankTitle);
        textStack.add(terminalStatusLabel);

        brandPanel.add(logoIcon);
        brandPanel.add(textStack);
        marquee.add(brandPanel, BorderLayout.WEST);

        // Center Clock
        clockLabel = new JLabel("2026-09-21 00:00:00", SwingConstants.CENTER);
        clockLabel.setFont(UITheme.FONT_MONO_BOLD);
        clockLabel.setForeground(UITheme.TEXT_WHITE);
        marquee.add(clockLabel, BorderLayout.CENTER);

        // Right Action Utilities (PDF Export & DB Viewer)
        JPanel utilPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        utilPanel.setBackground(new Color(15, 23, 42));

        JButton dbViewBtn = UITheme.createModernButton("View Database", new Color(51, 65, 85), Color.WHITE);
        dbViewBtn.setFont(UITheme.FONT_BODY_BOLD);
        dbViewBtn.addActionListener(e -> showDatabaseViewerDialog());

        JButton exportPdfBtn = UITheme.createModernButton("Generate Single PDF Report", UITheme.ACCENT_BLUE, Color.WHITE);
        exportPdfBtn.setFont(UITheme.FONT_BODY_BOLD);
        exportPdfBtn.addActionListener(e -> generateReportPdf());

        utilPanel.add(dbViewBtn);
        utilPanel.add(exportPdfBtn);
        marquee.add(utilPanel, BorderLayout.EAST);

        return marquee;
    }

    private JPanel createBottomHardwareConsole() {
        JPanel console = new JPanel(new BorderLayout(20, 0));
        console.setBackground(new Color(20, 30, 48));
        console.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(40, 60, 95), 1, true),
                BorderFactory.createEmptyBorder(12, 18, 12, 18)
        ));

        // Left Side: Peripherals (Card Reader + Receipt Printer + Cash Slot)
        JPanel leftPeripherals = new JPanel(new GridLayout(3, 1, 0, 10));
        leftPeripherals.setBackground(new Color(20, 30, 48));

        // 1. Card Reader Slot
        JPanel cardSlotPanel = new JPanel(new BorderLayout(8, 0));
        cardSlotPanel.setBackground(new Color(11, 20, 38));
        cardSlotPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(51, 65, 85), 1, true),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));

        cardSlotLed = new JLabel("● CARD READER: INSERT CHIP / SWIPE ● READY");
        cardSlotLed.setFont(UITheme.FONT_MONO_BOLD);
        cardSlotLed.setForeground(UITheme.SUCCESS_GREEN);

        JButton ejectBtn = UITheme.createModernButton("EJECT", UITheme.DANGER_RED, UITheme.TEXT_WHITE);
        ejectBtn.setFont(UITheme.FONT_MONO_BOLD);
        ejectBtn.setPreferredSize(new Dimension(85, 28));
        ejectBtn.addActionListener(e -> {
            screenManager.setSession(null);
            screenManager.showScreen("WELCOME");
        });

        cardSlotPanel.add(cardSlotLed, BorderLayout.CENTER);
        cardSlotPanel.add(ejectBtn, BorderLayout.EAST);
        leftPeripherals.add(cardSlotPanel);

        // 2. Receipt Dispenser Slot
        JPanel receiptSlotPanel = new JPanel(new BorderLayout());
        receiptSlotPanel.setBackground(new Color(11, 20, 38));
        receiptSlotPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(51, 65, 85), 1, true),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)
               JLabel receiptLabel = new JLabel("● THERMAL RECEIPT PRINTER  [ SLOT READY ]");
        receiptLabel.setFont(UITheme.FONT_MONO);
        receiptLabel.setForeground(UITheme.TEXT_MUTED);
        receiptSlotPanel.add(receiptLabel, BorderLayout.CENTER);
        leftPeripherals.add(receiptSlotPanel);

        // 3. Cash Dispenser Shutter Slot
        JPanel cashSlotPanel = new JPanel(new BorderLayout());
        cashSlotPanel.setBackground(new Color(11, 20, 38));
        cashSlotPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(51, 65, 85), 1, true),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));

        cashSlotLabel = new JLabel("● CASH DISPENSER & DEPOSIT HOPPER  [ SECURED ]");
        cashSlotLabel.setFont(UITheme.FONT_MONO_BOLD);
        cashSlotLabel.setForeground(UITheme.ACCENT_CYAN);
        cashSlotPanel.add(cashSlotLabel, BorderLayout.CENTER);
        leftPeripherals.add(cashSlotPanel);

        console.add(leftPeripherals, BorderLayout.CENTER);

        // Right Side: Metallic ATM Pinpad Hardware Simulation
        JPanel pinpadPanel = createMetallicPinpad();
        console.add(pinpadPanel, BorderLayout.EAST);

        return console;
    }

    private JPanel createMetallicPinpad() {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setBackground(new Color(15, 23, 42));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(71, 85, 105), 2, true),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));

        // 3x4 Digits Grid
        JPanel digits = new JPanel(new GridLayout(4, 3, 6, 6));
        digits.setBackground(new Color(15, 23, 42));

        String[] keys = {
                "1", "2", "3",
                "4", "5", "6",
                "7", "8", "9",
                "*", "0", "#"
        };

        for (String k : keys) {
            JButton btn = UITheme.createKeypadButton(k, e -> {
                screenManager.forwardKeypadPress(k);
                requestFocusInWindow();
            });
            btn.setPreferredSize(new Dimension(50, 40));
            digits.add(btn);
        }
        panel.add(digits, BorderLayout.CENTER);

        // Function Buttons (Cancel, Clear, Enter)
        JPanel actions = new JPanel(new GridLayout(3, 1, 0, 6));
        actions.setBackground(new Color(15, 23, 42));

        JButton cancelBtn = UITheme.createModernButton("CANCEL", UITheme.DANGER_RED, UITheme.TEXT_WHITE);
        cancelBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        cancelBtn.setPreferredSize(new Dimension(85, 40));
        cancelBtn.addActionListener(e -> {
            screenManager.forwardKeypadCancel();
            requestFocusInWindow();
        });

        JButton clearBtn = UITheme.createModernButton("CLEAR", UITheme.WARNING_YELLOW, new Color(15, 23, 42));
        clearBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        clearBtn.setPreferredSize(new Dimension(85, 40));
        clearBtn.addActionListener(e -> {
            screenManager.forwardKeypadClear();
            requestFocusInWindow();
        });

        JButton enterBtn = UITheme.createModernButton("ENTER", UITheme.SUCCESS_GREEN, UITheme.TEXT_WHITE);
        enterBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        enterBtn.setPreferredSize(new Dimension(85, 40));
        enterBtn.addActionListener(e -> {
            screenManager.forwardKeypadEnter();
            requestFocusInWindow();
        });

        actions.add(cancelBtn);
        actions.add(clearBtn);
        actions.add(enterBtn);
        panel.add(actions, BorderLayout.EAST);

        return panel;
    }

    private void updateClock() {
        clockLabel.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
    }

    public void updateKioskStatus(String screenName, AtmSession session) {
        if (session != null) {
            terminalStatusLabel.setText("LOGGED IN: " + session.getCustomer().getFullName() + "  |  ACTIVE ●");
            terminalStatusLabel.setForeground(UITheme.ACCENT_CYAN);
            cardSlotLed.setText("● CARD INSERTED: " + session.getCard().getMaskedCardNumber() + " ● BUSY");
            cardSlotLed.setForeground(UITheme.ACCENT_CYAN);
        } else {
            terminalStatusLabel.setText("TERMINAL #ATM-TERMINAL-01  |  ONLINE ●");
            terminalStatusLabel.setForeground(UITheme.SUCCESS_GREEN);
            cardSlotLed.setText("● CARD READER: INSERT CHIP / SWIPE ● READY");
            cardSlotLed.setForeground(UITheme.SUCCESS_GREEN);
        }
        requestFocusInWindow();
    }

    /**
     * Opens an interactive multi-tab database viewer dialog showing live relational tables.
     */
    public void showDatabaseViewerDialog() {
        JDialog dialog = new JDialog(this, "Live Relational Database Tables Inspector", true);
        dialog.setSize(900, 550);
        dialog.setLocationRelativeTo(this);

        JTabbedPane tabs = new JTabbedPane();
        DatabaseManager db = DatabaseManager.getInstance();

        // 1. Customers Tab
        tabs.addTab("customers", createTableScroll(
                new String[]{"ID", "First Name", "Last Name", "Email", "Phone", "Address", "Created At"},
                db.getAllCustomers().stream().map(c -> new Object[]{
                        c.getCustomerId(), c.getFirstName(), c.getLastName(), c.getEmail(), c.getPhone(), c.getAddress(), c.getCreatedAt()
                }).toArray(Object[][]::new)
        ));

        // 2. Accounts Tab
        tabs.addTab("accounts", createTableScroll(
                new String[]{"Account Number", "Customer ID", "Type", "Balance", "Currency", "Status", "Daily Limit"},
                db.getAllAccounts().stream().map(a -> new Object[]{
                        a.getAccountNumber(), a.getCustomerId(), a.getAccountType(), "$" + a.getBalance(), a.getCurrency(), a.getStatus(), "$" + a.getDailyWithdrawalLimit()
                }).toArray(Object[][]::new)
        ));

        // 3. ATM Cards Tab
        tabs.addTab("atm_cards", createTableScroll(
                new String[]{"Card Number", "Account Number", "Cardholder", "PIN Hash (SHA-256)", "Expiry", "CVV", "Status", "Fails"},
                db.getAllCards().stream().map(c -> new Object[]{
                        c.getCardNumber(), c.getAccountNumber(), c.getCardHolderName(), c.getPinHash().substring(0, 16) + "...", c.getExpiryDate(), c.getCvv(), c.getCardStatus(), c.getFailedPinAttempts()
                }).toArray(Object[][]::new)
        ));

        // 4. Transactions Tab
        tabs.addTab("transactions", createTableScroll(
                new String[]{"Tx ID", "Account", "Type", "Amount", "Balance After", "Status", "Timestamp"},
                db.getAllTransactions().stream().map(t -> new Object[]{
                        t.getTransactionId(), t.getAccountNumber(), t.getTransactionType(), "$" + t.getAmount(), "$" + t.getBalanceAfter(), t.getStatus(), t.getFormattedDate()
                }).toArray(Object[][]::new)
        ));

        // 5. Cash Inventory Tab
        CashInventory inv = db.getCashInventory();
        tabs.addTab("atm_cash_inventory", createTableScroll(
                new String[]{"Terminal ID", "$100 Bills", "$50 Bills", "$20 Bills", "$10 Bills", "Total Reserve", "Last Serviced"},
                new Object[][]{{
                        inv.getAtmId(), inv.getBills100(), inv.getBills50(), inv.getBills20(), inv.getBills10(), "$" + inv.getTotalCashReserve(), inv.getLastReplenished()
                }}
        ));

        // 6. Audit Logs Tab
        tabs.addTab("audit_logs", createTableScroll(
                new String[]{"Log ID", "Card", "Action", "Details", "Terminal", "Timestamp"},
                db.getAllAuditLogs().stream().map(l -> new Object[]{
                        l.getLogId(), l.getCardNumber(), l.getAction(), l.getDetails(), l.getIpOrTerminalId(), l.getFormattedTimestamp()
                }).toArray(Object[][]::new)
        ));

        dialog.setContentPane(tabs);
        dialog.setVisible(true);
    }

    private JScrollPane createTableScroll(String[] columns, Object[][] data) {
        DefaultTableModel model = new DefaultTableModel(data, columns) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        table.setFont(UITheme.FONT_MONO);
        table.setRowHeight(24);
        return new JScrollPane(table);
    }

    public void generateReportPdf() {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try {
            String outputPath = PdfReportGenerator.generateSystemReport(this);
            setCursor(Cursor.getDefaultCursor());
            JOptionPane.showMessageDialog(this,
                    "Successfully generated single PDF report!\n\nLocation:\n" + outputPath,
                    "PDF GENERATION COMPLETE",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            setCursor(Cursor.getDefaultCursor());
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Error generating PDF report: " + ex.getMessage(),
                    "PDF GENERATION ERROR",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    public ScreenManager getScreenManager() {
        return screenManager;
    }
}
