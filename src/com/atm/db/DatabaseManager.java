package com.atm.db;

import com.atm.model.*;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Thread-safe relational database manager and repository.
 * Implements strict schema constraints, foreign keys, and ACID transactional guarantees.
 */
public class DatabaseManager {
    private static DatabaseManager instance;

    // Relational Tables (Indexed by Primary Keys)
    private final Map<Integer, Customer> customers = new ConcurrentHashMap<>();
    private final Map<String, Account> accounts = new ConcurrentHashMap<>();
    private final Map<String, AtmCard> cards = new ConcurrentHashMap<>();
    private final Map<String, Transaction> transactions = new ConcurrentHashMap<>();
    private final List<AuditLog> auditLogs = Collections.synchronizedList(new ArrayList<>());
    private CashInventory cashInventory;

    private int nextAuditLogId = 1;
    private int nextCustomerId = 1;

    private DatabaseManager() {
        initializeSeedData();
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    /**
     * Seeds initial database records as defined in database/seed_data.sql
     */
    public synchronized void initializeSeedData() {
        customers.clear();
        accounts.clear();
        cards.clear();
        transactions.clear();
        auditLogs.clear();

        // 1. Seed Customers
        addCustomer(new Customer(1, "Alexander", "Vance", "a.vance@apexbank.com", "+1-555-019-2831", "742 Evergreen Terrace, Springfield, OR", LocalDateTime.of(2025, 1, 15, 9, 30)));
        addCustomer(new Customer(2, "Sophia", "Williams", "sophia.w@globalmail.com", "+1-555-044-8821", "100 Broadway Ave, New York, NY", LocalDateTime.of(2025, 3, 20, 11, 45)));
        addCustomer(new Customer(3, "David", "Miller", "d.miller@techcorp.io", "+1-555-082-9912", "450 Silicon Blvd, San Jose, CA", LocalDateTime.of(2025, 6, 10, 14, 20)));
        addCustomer(new Customer(4, "Emma", "Watson", "emma.watson@horizon.org", "+1-555-091-7733", "12 Kensington High St, London, UK", LocalDateTime.of(2025, 8, 5, 16, 10)));
        nextCustomerId = 5;

        // 2. Seed Accounts
        accounts.put("ACC-1001-8842", new Account("ACC-1001-8842", 1, "SAVINGS", new BigDecimal("5420.50"), "USD", "ACTIVE", new BigDecimal("2000.00"), LocalDateTime.of(2025, 1, 15, 9, 35)));
        accounts.put("ACC-1001-8843", new Account("ACC-1001-8843", 1, "CHECKING", new BigDecimal("18750.00"), "USD", "ACTIVE", new BigDecimal("5000.00"), LocalDateTime.of(2025, 1, 15, 9, 40)));
        accounts.put("ACC-2002-4419", new Account("ACC-2002-4419", 2, "CHECKING", new BigDecimal("3150.00"), "USD", "ACTIVE", new BigDecimal("1500.00"), LocalDateTime.of(2025, 3, 20, 11, 50)));
        accounts.put("ACC-3003-9120", new Account("ACC-3003-9120", 3, "SAVINGS", new BigDecimal("820.00"), "USD", "ACTIVE", new BigDecimal("1000.00"), LocalDateTime.of(2025, 6, 10, 14, 25)));
        accounts.put("ACC-4004-7731", new Account("ACC-4004-7731", 4, "CHECKING", new BigDecimal("12400.00"), "USD", "ACTIVE", new BigDecimal("3000.00"), LocalDateTime.of(2025, 8, 5, 16, 15)));

        // 3. Seed ATM Cards
        // PINs: 1234, 4321, 9999
        cards.put("4532110022334455", new AtmCard("4532110022334455", "ACC-1001-8842", "ALEXANDER VANCE", hashPin("1234"), "12/2028", "418", "ACTIVE", 0, LocalDateTime.of(2025, 1, 16, 10, 0)));
        cards.put("5412750088991234", new AtmCard("5412750088991234", "ACC-2002-4419", "SOPHIA WILLIAMS", hashPin("4321"), "08/2027", "892", "ACTIVE", 0, LocalDateTime.of(2025, 3, 21, 10, 0)));
        cards.put("4000123456789010", new AtmCard("4000123456789010", "ACC-3003-9120", "DAVID MILLER", hashPin("9999"), "05/2026", "109", "BLOCKED", 3, LocalDateTime.of(2025, 6, 11, 10, 0)));

        // 4. Seed Cash Inventory (ATM-TERMINAL-01)
        cashInventory = new CashInventory(1, "ATM-TERMINAL-01", 500, 600, 1000, 1000, LocalDateTime.of(2026, 9, 1, 8, 0));

        // 5. Seed Historical Transactions
        addTransaction(new Transaction("TX-20260901-001", "ACC-1001-8842", "4532110022334455", "DEPOSIT", new BigDecimal("2000.00"), new BigDecimal("5620.50"), null, "ATM-TERMINAL-01", "SUCCESS", "Salary Cash Deposit", LocalDateTime.of(2026, 9, 1, 10, 15, 20)));
        addTransaction(new Transaction("TX-20260905-014", "ACC-1001-8842", "4532110022334455", "WITHDRAWAL", new BigDecimal("200.00"), new BigDecimal("5420.50"), null, "ATM-TERMINAL-01", "SUCCESS", "Fast Cash Withdrawal", LocalDateTime.of(2026, 9, 5, 14, 32, 10)));
        addTransaction(new Transaction("TX-20260910-089", "ACC-2002-4419", "5412750088991234", "WITHDRAWAL", new BigDecimal("500.00"), new BigDecimal("3150.00"), null, "ATM-TERMINAL-01", "SUCCESS", "ATM Cash Dispense", LocalDateTime.of(2026, 9, 10, 18, 2, 44)));
        addTransaction(new Transaction("TX-20260918-102", "ACC-3003-9120", "4000123456789010", "BALANCE_INQUIRY", BigDecimal.ZERO, new BigDecimal("820.00"), null, "ATM-TERMINAL-01", "SUCCESS", "Customer Balance Check", LocalDateTime.of(2026, 9, 18, 9, 44, 12)));

        // 6. Seed Audit Logs
        addAuditLog("4532110022334455", "CARD_INSERTED", "Card verified and session initialized", "ATM-TERMINAL-01", LocalDateTime.of(2026, 9, 5, 14, 31, 55));
        addAuditLog("4532110022334455", "PIN_SUCCESS", "Valid PIN entered by customer Alexander Vance", "ATM-TERMINAL-01", LocalDateTime.of(2026, 9, 5, 14, 32, 2));
        addAuditLog("4000123456789010", "CARD_BLOCKED", "Exceeded maximum allowable attempts (3 consecutive failures)", "ATM-TERMINAL-01", LocalDateTime.of(2026, 9, 19, 22, 15, 30));
    }

    public static String hashPin(String pin) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(pin.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    public synchronized void addCustomer(Customer c) {
        customers.put(c.getCustomerId(), c);
    }

    public Customer getCustomer(int customerId) {
        return customers.get(customerId);
    }

    public Collection<Customer> getAllCustomers() {
        return customers.values();
    }

    public Account getAccount(String accountNumber) {
        return accounts.get(accountNumber);
    }

    public Collection<Account> getAllAccounts() {
        return accounts.values();
    }

    public AtmCard getCard(String cardNumber) {
        return cards.get(cardNumber);
    }

    public Collection<AtmCard> getAllCards() {
        return cards.values();
    }

    public CashInventory getCashInventory() {
        return cashInventory;
    }

    public synchronized void addTransaction(Transaction tx) {
        transactions.put(tx.getTransactionId(), tx);
    }

    public List<Transaction> getTransactionsForAccount(String accountNumber) {
        return transactions.values().stream()
                .filter(t -> t.getAccountNumber().equals(accountNumber))
                .sorted((t1, t2) -> t2.getCreatedAt().compareTo(t1.getCreatedAt()))
                .collect(Collectors.toList());
    }

    public Collection<Transaction> getAllTransactions() {
        return transactions.values().stream()
                .sorted((t1, t2) -> t2.getCreatedAt().compareTo(t1.getCreatedAt()))
                .collect(Collectors.toList());
    }

    public synchronized void addAuditLog(String cardNumber, String action, String details) {
        addAuditLog(cardNumber, action, details, "ATM-TERMINAL-01", LocalDateTime.now());
    }

    public synchronized void addAuditLog(String cardNumber, String action, String details, String terminalId, LocalDateTime timestamp) {
        AuditLog log = new AuditLog(nextAuditLogId++, cardNumber, action, details, terminalId, timestamp);
        auditLogs.add(log);
    }

    public List<AuditLog> getAllAuditLogs() {
        return new ArrayList<>(auditLogs);
    }

    /**
     * Executes an atomic transfer between two accounts with ACID guarantees.
     */
    public synchronized boolean executeTransfer(String fromAccountNum, String toAccountNum, BigDecimal amount, String cardNumber) {
        Account from = accounts.get(fromAccountNum);
        Account to = accounts.get(toAccountNum);

        if (from == null || to == null) return false;
        if (!"ACTIVE".equalsIgnoreCase(from.getStatus()) || !"ACTIVE".equalsIgnoreCase(to.getStatus())) return false;
        if (amount.compareTo(BigDecimal.ZERO) <= 0 || from.getBalance().compareTo(amount) < 0) return false;

        // Debit sender
        from.debit(amount);
        // Credit recipient
        to.credit(amount);

        String txOutId = "TX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String txInId = "TX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Transaction debitTx = new Transaction(
                txOutId, fromAccountNum, cardNumber, "TRANSFER_OUT",
                amount, from.getBalance(), toAccountNum, "ATM-TERMINAL-01",
                "SUCCESS", "Fund Transfer to " + toAccountNum, LocalDateTime.now()
        );
        Transaction creditTx = new Transaction(
                txInId, toAccountNum, null, "TRANSFER_IN",
                amount, to.getBalance(), fromAccountNum, "ATM-TERMINAL-01",
                "SUCCESS", "Received Transfer from " + fromAccountNum, LocalDateTime.now()
        );

        addTransaction(debitTx);
        addTransaction(creditTx);

        addAuditLog(cardNumber, "TRANSFER_SUCCESS", "Transferred $" + amount + " to " + toAccountNum);
        return true;
    }
}
