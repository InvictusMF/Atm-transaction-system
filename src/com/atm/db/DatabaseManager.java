package com.atm.db;

import com.atm.model.*;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enterprise relational database repository and ACID transaction manager.
 * Uses MySQL (InnoDB) as the authoritative persistent data store via JDBC.
 * Fully supports atomic fund transfers, withdrawals, deposits, and audit logging.
 */
public class DatabaseManager {
    private static DatabaseManager instance;

    private boolean connectedToMySQL = false;
    private String connectionError = null;

    // In-memory fallback cache only if MySQL is unreachable
    private final Map<Integer, Customer> fallbackCustomers = new ConcurrentHashMap<>();
    private final Map<String, Account> fallbackAccounts = new ConcurrentHashMap<>();
    private final Map<String, AtmCard> fallbackCards = new ConcurrentHashMap<>();
    private final Map<String, Transaction> fallbackTransactions = new ConcurrentHashMap<>();
    private final List<AuditLog> fallbackAuditLogs = Collections.synchronizedList(new ArrayList<>());
    private CashInventory fallbackCashInventory;
    private int nextFallbackAuditId = 1;

    private DatabaseManager() {
        checkAndInitializeConnection();
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    /**
     * Tests and establishes MySQL connectivity.
     * Seeds initial records if database tables are currently empty.
     */
    public synchronized void checkAndInitializeConnection() {
        DatabaseConfig.ConnectionTestResult test = DatabaseConfig.testConnection();
        if (test.isSuccess()) {
            this.connectedToMySQL = true;
            this.connectionError = null;
            System.out.println("✔ MySQL Connection Established: " + DatabaseConfig.getSafeUrl());
            seedDatabaseIfEmpty();
        } else {
            this.connectedToMySQL = false;
            this.connectionError = test.getMessage() + " (" + test.getDetails() + ")";
            System.err.println("⚠ MySQL Connection Warning: " + this.connectionError);
            System.err.println("  Operating in safe fallback mode. Configure database.properties to connect to live MySQL.");
            initializeFallbackSeedData();
        }
    }

    public boolean isConnectedToMySQL() {
        return connectedToMySQL;
    }

    public String getConnectionError() {
        return connectionError;
    }

    /**
     * Seeds initial records into MySQL if tables are empty. Safe and idempotent.
     */
    private void seedDatabaseIfEmpty() {
        if (!connectedToMySQL) return;

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM customers")) {

            if (rs.next() && rs.getInt(1) == 0) {
                System.out.println("• Database tables are empty. Running initial idempotent seed data script...");
                executeSeedStatements(conn);
                System.out.println("✔ Database successfully seeded.");
            }
        } catch (SQLException e) {
            System.err.println("WARN: Could not verify/seed database tables: " + e.getMessage());
        }
    }

    private void executeSeedStatements(Connection conn) {
        String[] seeds = new String[]{
                // Customers
                "INSERT IGNORE INTO customers (customer_id, first_name, last_name, email, phone, address, created_at) VALUES " +
                        "(1, 'Alexander', 'Vance', 'a.vance@apexbank.com', '+1-555-019-2831', '742 Evergreen Terrace, Springfield, OR', '2025-01-15 09:30:00')," +
                        "(2, 'Sophia', 'Williams', 'sophia.w@globalmail.com', '+1-555-044-8821', '100 Broadway Ave, New York, NY', '2025-03-20 11:45:00')," +
                        "(3, 'David', 'Miller', 'd.miller@techcorp.io', '+1-555-082-9912', '450 Silicon Blvd, San Jose, CA', '2025-06-10 14:20:00')," +
                        "(4, 'Emma', 'Watson', 'emma.watson@horizon.org', '+1-555-091-7733', '12 Kensington High St, London, UK', '2025-08-05 16:10:00')",

                // Accounts
                "INSERT IGNORE INTO accounts (account_number, customer_id, account_type, balance, currency, status, daily_withdrawal_limit, created_at) VALUES " +
                        "('ACC-1001-8842', 1, 'SAVINGS', 5420.50, 'USD', 'ACTIVE', 2000.00, '2025-01-15 09:35:00')," +
                        "('ACC-1001-8843', 1, 'CHECKING', 18750.00, 'USD', 'ACTIVE', 5000.00, '2025-01-15 09:40:00')," +
                        "('ACC-2002-4419', 2, 'CHECKING', 3150.00, 'USD', 'ACTIVE', 1500.00, '2025-03-20 11:50:00')," +
                        "('ACC-3003-9120', 3, 'SAVINGS', 820.00, 'USD', 'ACTIVE', 1000.00, '2025-06-10 14:25:00')," +
                        "('ACC-4004-7731', 4, 'CHECKING', 12400.00, 'USD', 'ACTIVE', 3000.00, '2025-08-05 16:15:00')",

                // ATM Cards
                "INSERT IGNORE INTO atm_cards (card_number, account_number, card_holder_name, pin_hash, expiry_date, cvv, card_status, failed_pin_attempts, issued_at) VALUES " +
                        "('4532110022334455', 'ACC-1001-8842', 'ALEXANDER VANCE', '03ac674216f3e15c761ee1a5e255f067953623c8b388b4459e13f978d7c846f4', '12/2028', '418', 'ACTIVE', 0, '2025-01-16 10:00:00')," +
                        "('5412750088991234', 'ACC-2002-4419', 'SOPHIA WILLIAMS', 'd74ff0ee8da3b9806b18c877dbf29bbde50b5bd8e4dad7a3a725000feb82e8f1', '08/2027', '892', 'ACTIVE', 0, '2025-03-21 10:00:00')," +
                        "('4000123456789010', 'ACC-3003-9120', 'DAVID MILLER', 'fa547353f687e59e69f459735661b00154fa03746c12c0c8fa5ecd102b84ed3d', '05/2026', '109', 'BLOCKED', 3, '2025-06-11 10:00:00')",

                // Inventory
                "INSERT IGNORE INTO atm_cash_inventory (inventory_id, atm_id, bills_100, bills_50, bills_20, bills_10, total_cash_reserve, last_replenished) VALUES " +
                        "(1, 'ATM-TERMINAL-01', 500, 600, 1000, 1000, 110000.00, '2026-09-01 08:00:00')",

                // Seed Transactions
                "INSERT IGNORE INTO transactions (transaction_id, account_number, card_number, transaction_type, amount, balance_after, beneficiary_account, atm_id, status, remarks, created_at) VALUES " +
                        "('TX-20260901-001', 'ACC-1001-8842', '4532110022334455', 'DEPOSIT', 2000.00, 5620.50, NULL, 'ATM-TERMINAL-01', 'SUCCESS', 'Salary Cash Deposit', '2026-09-01 10:15:20')," +
                        "('TX-20260905-014', 'ACC-1001-8842', '4532110022334455', 'WITHDRAWAL', 200.00, 5420.50, NULL, 'ATM-TERMINAL-01', 'SUCCESS', 'Fast Cash Withdrawal', '2026-09-05 14:32:10')," +
                        "('TX-20260910-089', 'ACC-2002-4419', '5412750088991234', 'WITHDRAWAL', 500.00, 3150.00, NULL, 'ATM-TERMINAL-01', 'SUCCESS', 'ATM Cash Dispense', '2026-09-10 18:02:44')," +
                        "('TX-20260918-102', 'ACC-3003-9120', '4000123456789010', 'BALANCE_INQUIRY', 0.00, 820.00, NULL, 'ATM-TERMINAL-01', 'SUCCESS', 'Customer Balance Check', '2026-09-18 09:44:12')",

                // Audit Logs
                "INSERT IGNORE INTO audit_logs (log_id, card_number, action, details, ip_or_terminal_id, timestamp) VALUES " +
                        "(1, '4532110022334455', 'CARD_INSERTED', 'Card verified and session initialized', 'ATM-TERMINAL-01', '2026-09-05 14:31:55')," +
                        "(2, '4532110022334455', 'PIN_SUCCESS', 'Valid PIN entered by customer Alexander Vance', 'ATM-TERMINAL-01', '2026-09-05 14:32:02')," +
                        "(3, '4000123456789010', 'CARD_BLOCKED', 'Exceeded maximum allowable attempts (3 consecutive failures)', 'ATM-TERMINAL-01', '2026-09-19 22:15:30')"
        };

        for (String sql : seeds) {
            try (Statement s = conn.createStatement()) {
                s.executeUpdate(sql);
            } catch (SQLException ex) {
                System.err.println("Seed statement warning: " + ex.getMessage());
            }
        }
    }

    public void initializeSeedData() {
        initializeFallbackSeedData();
    }

    private void initializeFallbackSeedData() {
        fallbackCustomers.clear();
        fallbackAccounts.clear();
        fallbackCards.clear();
        fallbackTransactions.clear();
        fallbackAuditLogs.clear();

        fallbackCustomers.put(1, new Customer(1, "Alexander", "Vance", "a.vance@apexbank.com", "+1-555-019-2831", "742 Evergreen Terrace, Springfield, OR", LocalDateTime.of(2025, 1, 15, 9, 30)));
        fallbackCustomers.put(2, new Customer(2, "Sophia", "Williams", "sophia.w@globalmail.com", "+1-555-044-8821", "100 Broadway Ave, New York, NY", LocalDateTime.of(2025, 3, 20, 11, 45)));
        fallbackCustomers.put(3, new Customer(3, "David", "Miller", "d.miller@techcorp.io", "+1-555-082-9912", "450 Silicon Blvd, San Jose, CA", LocalDateTime.of(2025, 6, 10, 14, 20)));
        fallbackCustomers.put(4, new Customer(4, "Emma", "Watson", "emma.watson@horizon.org", "+1-555-091-7733", "12 Kensington High St, London, UK", LocalDateTime.of(2025, 8, 5, 16, 10)));

        fallbackAccounts.put("ACC-1001-8842", new Account("ACC-1001-8842", 1, "SAVINGS", new BigDecimal("5420.50"), "USD", "ACTIVE", new BigDecimal("2000.00"), LocalDateTime.of(2025, 1, 15, 9, 35)));
        fallbackAccounts.put("ACC-1001-8843", new Account("ACC-1001-8843", 1, "CHECKING", new BigDecimal("18750.00"), "USD", "ACTIVE", new BigDecimal("5000.00"), LocalDateTime.of(2025, 1, 15, 9, 40)));
        fallbackAccounts.put("ACC-2002-4419", new Account("ACC-2002-4419", 2, "CHECKING", new BigDecimal("3150.00"), "USD", "ACTIVE", new BigDecimal("1500.00"), LocalDateTime.of(2025, 3, 20, 11, 50)));
        fallbackAccounts.put("ACC-3003-9120", new Account("ACC-3003-9120", 3, "SAVINGS", new BigDecimal("820.00"), "USD", "ACTIVE", new BigDecimal("1000.00"), LocalDateTime.of(2025, 6, 10, 14, 25)));
        fallbackAccounts.put("ACC-4004-7731", new Account("ACC-4004-7731", 4, "CHECKING", new BigDecimal("12400.00"), "USD", "ACTIVE", new BigDecimal("3000.00"), LocalDateTime.of(2025, 8, 5, 16, 15)));

        fallbackCards.put("4532110022334455", new AtmCard("4532110022334455", "ACC-1001-8842", "ALEXANDER VANCE", hashPin("1234"), "12/2028", "418", "ACTIVE", 0, LocalDateTime.of(2025, 1, 16, 10, 0)));
        fallbackCards.put("5412750088991234", new AtmCard("5412750088991234", "ACC-2002-4419", "SOPHIA WILLIAMS", hashPin("4321"), "08/2027", "892", "ACTIVE", 0, LocalDateTime.of(2025, 3, 21, 10, 0)));
        fallbackCards.put("4000123456789010", new AtmCard("4000123456789010", "ACC-3003-9120", "DAVID MILLER", hashPin("9999"), "05/2026", "109", "BLOCKED", 3, LocalDateTime.of(2025, 6, 11, 10, 0)));

        fallbackCashInventory = new CashInventory(1, "ATM-TERMINAL-01", 500, 600, 1000, 1000, LocalDateTime.of(2026, 9, 1, 8, 0));

        fallbackTransactions.put("TX-20260901-001", new Transaction("TX-20260901-001", "ACC-1001-8842", "4532110022334455", "DEPOSIT", new BigDecimal("2000.00"), new BigDecimal("5620.50"), null, "ATM-TERMINAL-01", "SUCCESS", "Salary Cash Deposit", LocalDateTime.of(2026, 9, 1, 10, 15, 20)));
        fallbackTransactions.put("TX-20260905-014", new Transaction("TX-20260905-014", "ACC-1001-8842", "4532110022334455", "WITHDRAWAL", new BigDecimal("200.00"), new BigDecimal("5420.50"), null, "ATM-TERMINAL-01", "SUCCESS", "Fast Cash Withdrawal", LocalDateTime.of(2026, 9, 5, 14, 32, 10)));
        fallbackTransactions.put("TX-20260910-089", new Transaction("TX-20260910-089", "ACC-2002-4419", "5412750088991234", "WITHDRAWAL", new BigDecimal("500.00"), new BigDecimal("3150.00"), null, "ATM-TERMINAL-01", "SUCCESS", "ATM Cash Dispense", LocalDateTime.of(2026, 9, 10, 18, 2, 44)));
        fallbackTransactions.put("TX-20260918-102", new Transaction("TX-20260918-102", "ACC-3003-9120", "4000123456789010", "BALANCE_INQUIRY", BigDecimal.ZERO, new BigDecimal("820.00"), null, "ATM-TERMINAL-01", "SUCCESS", "Customer Balance Check", LocalDateTime.of(2026, 9, 18, 9, 44, 12)));

        fallbackAuditLogs.add(new AuditLog(1, "4532110022334455", "CARD_INSERTED", "Card verified and session initialized", "ATM-TERMINAL-01", LocalDateTime.of(2026, 9, 5, 14, 31, 55)));
        fallbackAuditLogs.add(new AuditLog(2, "4532110022334455", "PIN_SUCCESS", "Valid PIN entered by customer Alexander Vance", "ATM-TERMINAL-01", LocalDateTime.of(2026, 9, 5, 14, 32, 2)));
        fallbackAuditLogs.add(new AuditLog(3, "4000123456789010", "CARD_BLOCKED", "Exceeded maximum allowable attempts (3 consecutive failures)", "ATM-TERMINAL-01", LocalDateTime.of(2026, 9, 19, 22, 15, 30)));
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

    // ========================================================================
    // CUSTOMER OPERATIONS
    // ========================================================================

    public Customer getCustomer(int customerId) {
        if (!connectedToMySQL) {
            return fallbackCustomers.get(customerId);
        }

        String sql = "SELECT customer_id, first_name, last_name, email, phone, address, created_at FROM customers WHERE customer_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapCustomer(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("SQL Error in getCustomer(" + customerId + "): " + e.getMessage());
        }
        return null;
    }

    public Collection<Customer> getAllCustomers() {
        if (!connectedToMySQL) {
            return fallbackCustomers.values();
        }

        List<Customer> list = new ArrayList<>();
        String sql = "SELECT customer_id, first_name, last_name, email, phone, address, created_at FROM customers ORDER BY customer_id ASC";
        try (Connection conn = DatabaseConfig.getConnection();
             Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapCustomer(rs));
            }
        } catch (SQLException e) {
            System.err.println("SQL Error in getAllCustomers: " + e.getMessage());
        }
        return list;
    }

    private Customer mapCustomer(ResultSet rs) throws SQLException {
        Timestamp ts = rs.getTimestamp("created_at");
        LocalDateTime created = ts != null ? ts.toLocalDateTime() : LocalDateTime.now();
        return new Customer(
                rs.getInt("customer_id"),
                rs.getString("first_name"),
                rs.getString("last_name"),
                rs.getString("email"),
                rs.getString("phone"),
                rs.getString("address"),
                created
        );
    }

    // ========================================================================
    // ACCOUNT OPERATIONS
    // ========================================================================

    public Account getAccount(String accountNumber) {
        if (accountNumber == null) return null;

        if (!connectedToMySQL) {
            return fallbackAccounts.get(accountNumber.trim());
        }

        String sql = "SELECT account_number, customer_id, account_type, balance, currency, status, daily_withdrawal_limit, created_at FROM accounts WHERE account_number = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, accountNumber.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapAccount(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("SQL Error in getAccount(" + accountNumber + "): " + e.getMessage());
        }
        return null;
    }

    public Collection<Account> getAllAccounts() {
        if (!connectedToMySQL) {
            return fallbackAccounts.values();
        }

        List<Account> list = new ArrayList<>();
        String sql = "SELECT account_number, customer_id, account_type, balance, currency, status, daily_withdrawal_limit, created_at FROM accounts ORDER BY account_number ASC";
        try (Connection conn = DatabaseConfig.getConnection();
             Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapAccount(rs));
            }
        } catch (SQLException e) {
            System.err.println("SQL Error in getAllAccounts: " + e.getMessage());
        }
        return list;
    }

    private Account mapAccount(ResultSet rs) throws SQLException {
        Timestamp ts = rs.getTimestamp("created_at");
        LocalDateTime created = ts != null ? ts.toLocalDateTime() : LocalDateTime.now();
        return new Account(
                rs.getString("account_number"),
                rs.getInt("customer_id"),
                rs.getString("account_type"),
                rs.getBigDecimal("balance"),
                rs.getString("currency"),
                rs.getString("status"),
                rs.getBigDecimal("daily_withdrawal_limit"),
                created
        );
    }

    // ========================================================================
    // ATM CARD OPERATIONS
    // ========================================================================

    public AtmCard getCard(String cardNumber) {
        if (cardNumber == null) return null;

        if (!connectedToMySQL) {
            return fallbackCards.get(cardNumber.trim());
        }

        String sql = "SELECT card_number, account_number, card_holder_name, pin_hash, expiry_date, cvv, card_status, failed_pin_attempts, issued_at FROM atm_cards WHERE card_number = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, cardNumber.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapCard(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("SQL Error in getCard(" + cardNumber + "): " + e.getMessage());
        }
        return null;
    }

    public Collection<AtmCard> getAllCards() {
        if (!connectedToMySQL) {
            return fallbackCards.values();
        }

        List<AtmCard> list = new ArrayList<>();
        String sql = "SELECT card_number, account_number, card_holder_name, pin_hash, expiry_date, cvv, card_status, failed_pin_attempts, issued_at FROM atm_cards ORDER BY card_number ASC";
        try (Connection conn = DatabaseConfig.getConnection();
             Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapCard(rs));
            }
        } catch (SQLException e) {
            System.err.println("SQL Error in getAllCards: " + e.getMessage());
        }
        return list;
    }

    public void updateCardFailedAttempts(String cardNumber, int attempts, String status) {
        if (!connectedToMySQL) {
            AtmCard c = fallbackCards.get(cardNumber);
            if (c != null) {
                c.setFailedPinAttempts(attempts);
                c.setCardStatus(status);
            }
            return;
        }

        String sql = "UPDATE atm_cards SET failed_pin_attempts = ?, card_status = ? WHERE card_number = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, attempts);
            ps.setString(2, status);
            ps.setString(3, cardNumber);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("SQL Error updating card failed attempts: " + e.getMessage());
        }
    }

    public void updateCardPin(String cardNumber, String newPinHash) {
        if (!connectedToMySQL) {
            AtmCard c = fallbackCards.get(cardNumber);
            if (c != null) {
                c.setPinHash(newPinHash);
                c.setFailedPinAttempts(0);
            }
            return;
        }

        String sql = "UPDATE atm_cards SET pin_hash = ?, failed_pin_attempts = 0 WHERE card_number = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newPinHash);
            ps.setString(2, cardNumber);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("SQL Error updating card PIN: " + e.getMessage());
        }
    }

    private AtmCard mapCard(ResultSet rs) throws SQLException {
        Timestamp ts = rs.getTimestamp("issued_at");
        LocalDateTime issued = ts != null ? ts.toLocalDateTime() : LocalDateTime.now();
        return new AtmCard(
                rs.getString("card_number"),
                rs.getString("account_number"),
                rs.getString("card_holder_name"),
                rs.getString("pin_hash"),
                rs.getString("expiry_date"),
                rs.getString("cvv"),
                rs.getString("card_status"),
                rs.getInt("failed_pin_attempts"),
                issued
        );
    }

    // ========================================================================
    // CASH INVENTORY OPERATIONS
    // ========================================================================

    public CashInventory getCashInventory() {
        if (!connectedToMySQL) {
            return fallbackCashInventory;
        }

        String sql = "SELECT inventory_id, atm_id, bills_100, bills_50, bills_20, bills_10, total_cash_reserve, last_replenished FROM atm_cash_inventory WHERE atm_id = 'ATM-TERMINAL-01' LIMIT 1";
        try (Connection conn = DatabaseConfig.getConnection();
             Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            if (rs.next()) {
                return mapCashInventory(rs);
            }
        } catch (SQLException e) {
            System.err.println("SQL Error in getCashInventory: " + e.getMessage());
        }
        return fallbackCashInventory != null ? fallbackCashInventory : new CashInventory(1, "ATM-TERMINAL-01", 500, 600, 1000, 1000, LocalDateTime.now());
    }

    private CashInventory mapCashInventory(ResultSet rs) throws SQLException {
        Timestamp ts = rs.getTimestamp("last_replenished");
        LocalDateTime last = ts != null ? ts.toLocalDateTime() : LocalDateTime.now();
        return new CashInventory(
                rs.getInt("inventory_id"),
                rs.getString("atm_id"),
                rs.getInt("bills_100"),
                rs.getInt("bills_50"),
                rs.getInt("bills_20"),
                rs.getInt("bills_10"),
                last
        );
    }

    // ========================================================================
    // TRANSACTION & AUDIT LOG OPERATIONS
    // ========================================================================

    public void addTransaction(Transaction tx) {
        if (!connectedToMySQL) {
            fallbackTransactions.put(tx.getTransactionId(), tx);
            return;
        }

        String sql = "INSERT INTO transactions (transaction_id, account_number, card_number, transaction_type, amount, balance_after, beneficiary_account, atm_id, status, remarks, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tx.getTransactionId());
            ps.setString(2, tx.getAccountNumber());
            ps.setString(3, tx.getCardNumber());
            ps.setString(4, tx.getTransactionType());
            ps.setBigDecimal(5, tx.getAmount());
            ps.setBigDecimal(6, tx.getBalanceAfter());
            ps.setString(7, tx.getBeneficiaryAccount());
            ps.setString(8, tx.getAtmId());
            ps.setString(9, tx.getStatus());
            ps.setString(10, tx.getRemarks());
            ps.setTimestamp(11, Timestamp.valueOf(tx.getCreatedAt()));
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("SQL Error in addTransaction: " + e.getMessage());
        }
    }

    public List<Transaction> getTransactionsForAccount(String accountNumber) {
        if (!connectedToMySQL) {
            List<Transaction> list = new ArrayList<>();
            for (Transaction t : fallbackTransactions.values()) {
                if (t.getAccountNumber().equals(accountNumber)) list.add(t);
            }
            list.sort((t1, t2) -> t2.getCreatedAt().compareTo(t1.getCreatedAt()));
            return list;
        }

        List<Transaction> list = new ArrayList<>();
        String sql = "SELECT transaction_id, account_number, card_number, transaction_type, amount, balance_after, beneficiary_account, atm_id, status, remarks, created_at " +
                "FROM transactions WHERE account_number = ? ORDER BY created_at DESC";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, accountNumber);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapTransaction(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("SQL Error in getTransactionsForAccount: " + e.getMessage());
        }
        return list;
    }

    public Collection<Transaction> getAllTransactions() {
        if (!connectedToMySQL) {
            List<Transaction> list = new ArrayList<>(fallbackTransactions.values());
            list.sort((t1, t2) -> t2.getCreatedAt().compareTo(t1.getCreatedAt()));
            return list;
        }

        List<Transaction> list = new ArrayList<>();
        String sql = "SELECT transaction_id, account_number, card_number, transaction_type, amount, balance_after, beneficiary_account, atm_id, status, remarks, created_at " +
                "FROM transactions ORDER BY created_at DESC";
        try (Connection conn = DatabaseConfig.getConnection();
             Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapTransaction(rs));
            }
        } catch (SQLException e) {
            System.err.println("SQL Error in getAllTransactions: " + e.getMessage());
        }
        return list;
    }

    private Transaction mapTransaction(ResultSet rs) throws SQLException {
        Timestamp ts = rs.getTimestamp("created_at");
        LocalDateTime created = ts != null ? ts.toLocalDateTime() : LocalDateTime.now();
        return new Transaction(
                rs.getString("transaction_id"),
                rs.getString("account_number"),
                rs.getString("card_number"),
                rs.getString("transaction_type"),
                rs.getBigDecimal("amount"),
                rs.getBigDecimal("balance_after"),
                rs.getString("beneficiary_account"),
                rs.getString("atm_id"),
                rs.getString("status"),
                rs.getString("remarks"),
                created
        );
    }

    public void addAuditLog(String cardNumber, String action, String details) {
        addAuditLog(cardNumber, action, details, "ATM-TERMINAL-01", LocalDateTime.now());
    }

    public void addAuditLog(String cardNumber, String action, String details, String terminalId, LocalDateTime timestamp) {
        if (!connectedToMySQL) {
            fallbackAuditLogs.add(new AuditLog(nextFallbackAuditId++, cardNumber, action, details, terminalId, timestamp));
            return;
        }

        String sql = "INSERT INTO audit_logs (card_number, action, details, ip_or_terminal_id, timestamp) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, cardNumber);
            ps.setString(2, action);
            ps.setString(3, details);
            ps.setString(4, terminalId);
            ps.setTimestamp(5, Timestamp.valueOf(timestamp != null ? timestamp : LocalDateTime.now()));
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("SQL Error in addAuditLog: " + e.getMessage());
        }
    }

    public List<AuditLog> getAllAuditLogs() {
        if (!connectedToMySQL) {
            List<AuditLog> list = new ArrayList<>(fallbackAuditLogs);
            list.sort((l1, l2) -> l2.getTimestamp().compareTo(l1.getTimestamp()));
            return list;
        }

        List<AuditLog> list = new ArrayList<>();
        String sql = "SELECT log_id, card_number, action, details, ip_or_terminal_id, timestamp FROM audit_logs ORDER BY timestamp DESC";
        try (Connection conn = DatabaseConfig.getConnection();
             Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) {
                Timestamp ts = rs.getTimestamp("timestamp");
                LocalDateTime t = ts != null ? ts.toLocalDateTime() : LocalDateTime.now();
                list.add(new AuditLog(
                        rs.getInt("log_id"),
                        rs.getString("card_number"),
                        rs.getString("action"),
                        rs.getString("details"),
                        rs.getString("ip_or_terminal_id"),
                        t
                ));
            }
        } catch (SQLException e) {
            System.err.println("SQL Error in getAllAuditLogs: " + e.getMessage());
        }
        return list;
    }

    // ========================================================================
    // ACID TRANSACTIONAL WORKFLOWS (TRANSFER, WITHDRAWAL, DEPOSIT)
    // ========================================================================

    /**
     * Executes an atomic electronic wire transfer between two accounts with strict ACID guarantees.
     * Enforces pessimistic row locks, prevents overdrafts, credits beneficiary, records both debit
     * and credit transaction entries, and audit logs within a single database transaction.
     */
    public synchronized boolean executeTransfer(String fromAccountNum, String toAccountNum, BigDecimal amount, String cardNumber) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) return false;
        if (fromAccountNum == null || toAccountNum == null || fromAccountNum.equalsIgnoreCase(toAccountNum)) return false;

        if (!connectedToMySQL) {
            Account from = fallbackAccounts.get(fromAccountNum);
            Account to = fallbackAccounts.get(toAccountNum);
            if (from == null || to == null) return false;
            if (!"ACTIVE".equalsIgnoreCase(from.getStatus()) || !"ACTIVE".equalsIgnoreCase(to.getStatus())) return false;
            if (from.getBalance().compareTo(amount) < 0) return false;

            from.debit(amount);
            to.credit(amount);

            String txOutId = "TX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            String txInId = "TX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

            fallbackTransactions.put(txOutId, new Transaction(txOutId, fromAccountNum, cardNumber, "TRANSFER_OUT", amount, from.getBalance(), toAccountNum, "ATM-TERMINAL-01", "SUCCESS", "Fund Transfer to " + toAccountNum, LocalDateTime.now()));
            fallbackTransactions.put(txInId, new Transaction(txInId, toAccountNum, null, "TRANSFER_IN", amount, to.getBalance(), fromAccountNum, "ATM-TERMINAL-01", "SUCCESS", "Received Transfer from " + fromAccountNum, LocalDateTime.now()));
            addAuditLog(cardNumber, "TRANSFER_SUCCESS", "Transferred $" + amount + " to " + toAccountNum);
            return true;
        }

        Connection conn = null;
        try {
            conn = DatabaseConfig.getConnection();
            conn.setAutoCommit(false); // Begin ACID Transaction

            BigDecimal fromNewBal;
            BigDecimal toNewBal;

            // 1. Check and Debit Sender Account
            String debitSql = "UPDATE accounts SET balance = balance - ? WHERE account_number = ? AND balance >= ? AND status = 'ACTIVE'";
            try (PreparedStatement psDebit = conn.prepareStatement(debitSql)) {
                psDebit.setBigDecimal(1, amount);
                psDebit.setString(2, fromAccountNum);
                psDebit.setBigDecimal(3, amount);
                int rows = psDebit.executeUpdate();
                if (rows == 0) {
                    conn.rollback();
                    return false; // Insufficient funds, inactive account, or sender not found
                }
            }

            // 2. Fetch updated sender balance
            String selSenderSql = "SELECT balance FROM accounts WHERE account_number = ?";
            try (PreparedStatement psSel = conn.prepareStatement(selSenderSql)) {
                psSel.setString(1, fromAccountNum);
                try (ResultSet rs = psSel.executeQuery()) {
                    if (rs.next()) {
                        fromNewBal = rs.getBigDecimal("balance");
                    } else {
                        conn.rollback();
                        return false;
                    }
                }
            }

            // 3. Credit Recipient Account
            String creditSql = "UPDATE accounts SET balance = balance + ? WHERE account_number = ? AND status = 'ACTIVE'";
            try (PreparedStatement psCredit = conn.prepareStatement(creditSql)) {
                psCredit.setBigDecimal(1, amount);
                psCredit.setString(2, toAccountNum);
                int rows = psCredit.executeUpdate();
                if (rows == 0) {
                    conn.rollback();
                    return false; // Recipient not found or inactive
                }
            }

            // 4. Fetch updated recipient balance
            String selRecipSql = "SELECT balance FROM accounts WHERE account_number = ?";
            try (PreparedStatement psSel = conn.prepareStatement(selRecipSql)) {
                psSel.setString(1, toAccountNum);
                try (ResultSet rs = psSel.executeQuery()) {
                    if (rs.next()) {
                        toNewBal = rs.getBigDecimal("balance");
                    } else {
                        conn.rollback();
                        return false;
                    }
                }
            }

            // 5. Generate Transaction IDs
            String txOutId = "TX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            String txInId = "TX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            Timestamp now = Timestamp.valueOf(LocalDateTime.now());

            String insertTxSql = "INSERT INTO transactions (transaction_id, account_number, card_number, transaction_type, amount, balance_after, beneficiary_account, atm_id, status, remarks, created_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            // 6. Record Debit Transaction (Sender)
            try (PreparedStatement psTx = conn.prepareStatement(insertTxSql)) {
                psTx.setString(1, txOutId);
                psTx.setString(2, fromAccountNum);
                psTx.setString(3, cardNumber);
                psTx.setString(4, "TRANSFER_OUT");
                psTx.setBigDecimal(5, amount);
                psTx.setBigDecimal(6, fromNewBal);
                psTx.setString(7, toAccountNum);
                psTx.setString(8, "ATM-TERMINAL-01");
                psTx.setString(9, "SUCCESS");
                psTx.setString(10, "Fund Transfer to " + toAccountNum);
                psTx.setTimestamp(11, now);
                psTx.executeUpdate();
            }

            // 7. Record Credit Transaction (Recipient)
            try (PreparedStatement psTx = conn.prepareStatement(insertTxSql)) {
                psTx.setString(1, txInId);
                psTx.setString(2, toAccountNum);
                psTx.setString(3, null);
                psTx.setString(4, "TRANSFER_IN");
                psTx.setBigDecimal(5, amount);
                psTx.setBigDecimal(6, toNewBal);
                psTx.setString(7, fromAccountNum);
                psTx.setString(8, "ATM-TERMINAL-01");
                psTx.setString(9, "SUCCESS");
                psTx.setString(10, "Received Transfer from " + fromAccountNum);
                psTx.setTimestamp(11, now);
                psTx.executeUpdate();
            }

            // 8. Record Audit Log
            String auditSql = "INSERT INTO audit_logs (card_number, action, details, ip_or_terminal_id, timestamp) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement psAudit = conn.prepareStatement(auditSql)) {
                psAudit.setString(1, cardNumber);
                psAudit.setString(2, "TRANSFER_SUCCESS");
                psAudit.setString(3, "Transferred $" + amount + " to " + toAccountNum);
                psAudit.setString(4, "ATM-TERMINAL-01");
                psAudit.setTimestamp(5, now);
                psAudit.executeUpdate();
            }

            conn.commit(); // Commit ACID Transaction
            return true;

        } catch (SQLException e) {
            System.err.println("Transaction Rollback in executeTransfer: " + e.getMessage());
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignored) {}
            }
            return false;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignored) {}
            }
        }
    }

    /**
     * Executes an atomic cash withdrawal transaction.
     * Debits sender balance, updates ATM hardware cash inventory, writes transaction ledger and audit log.
     */
    public synchronized boolean executeWithdrawal(String accountNum, String cardNum, int amount, Map<Integer, Integer> dispensedBills, String atmId) {
        BigDecimal withdrawalAmt = BigDecimal.valueOf(amount);

        if (!connectedToMySQL) {
            Account acc = fallbackAccounts.get(accountNum);
            CashInventory inv = fallbackCashInventory;
            if (acc == null || inv == null) return false;
            if (acc.getBalance().compareTo(withdrawalAmt) < 0) return false;

            acc.debit(withdrawalAmt);
            String txId = "TX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            fallbackTransactions.put(txId, new Transaction(txId, accountNum, cardNum, "WITHDRAWAL", withdrawalAmt, acc.getBalance(), null, atmId, "SUCCESS", "Cash Withdrawal dispensed", LocalDateTime.now()));
            addAuditLog(cardNum, "WITHDRAW_SUCCESS", "Dispensed $" + amount);
            return true;
        }

        Connection conn = null;
        try {
            conn = DatabaseConfig.getConnection();
            conn.setAutoCommit(false);

            // 1. Debit Account
            String debitSql = "UPDATE accounts SET balance = balance - ? WHERE account_number = ? AND balance >= ? AND status = 'ACTIVE'";
            try (PreparedStatement ps = conn.prepareStatement(debitSql)) {
                ps.setBigDecimal(1, withdrawalAmt);
                ps.setString(2, accountNum);
                ps.setBigDecimal(3, withdrawalAmt);
                int rows = ps.executeUpdate();
                if (rows == 0) {
                    conn.rollback();
                    return false;
                }
            }

            // 2. Fetch updated balance
            BigDecimal newBal;
            String selBalSql = "SELECT balance FROM accounts WHERE account_number = ?";
            try (PreparedStatement ps = conn.prepareStatement(selBalSql)) {
                ps.setString(1, accountNum);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        newBal = rs.getBigDecimal("balance");
                    } else {
                        conn.rollback();
                        return false;
                    }
                }
            }

            // 3. Decrement ATM cash inventory
            int b100 = dispensedBills.getOrDefault(100, 0);
            int b50 = dispensedBills.getOrDefault(50, 0);
            int b20 = dispensedBills.getOrDefault(20, 0);
            int b10 = dispensedBills.getOrDefault(10, 0);

            String invSql = "UPDATE atm_cash_inventory SET bills_100 = bills_100 - ?, bills_50 = bills_50 - ?, bills_20 = bills_20 - ?, bills_10 = bills_10 - ?, " +
                    "total_cash_reserve = total_cash_reserve - ?, last_replenished = NOW() " +
                    "WHERE atm_id = ? AND bills_100 >= ? AND bills_50 >= ? AND bills_20 >= ? AND bills_10 >= ?";
            try (PreparedStatement ps = conn.prepareStatement(invSql)) {
                ps.setInt(1, b100);
                ps.setInt(2, b50);
                ps.setInt(3, b20);
                ps.setInt(4, b10);
                ps.setBigDecimal(5, withdrawalAmt);
                ps.setString(6, atmId);
                ps.setInt(7, b100);
                ps.setInt(8, b50);
                ps.setInt(9, b20);
                ps.setInt(10, b10);

                int rows = ps.executeUpdate();
                if (rows == 0) {
                    conn.rollback();
                    return false;
                }
            }

            // 4. Record Transaction
            String txId = "TX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            Timestamp now = Timestamp.valueOf(LocalDateTime.now());
            String txSql = "INSERT INTO transactions (transaction_id, account_number, card_number, transaction_type, amount, balance_after, beneficiary_account, atm_id, status, remarks, created_at) " +
                    "VALUES (?, ?, ?, 'WITHDRAWAL', ?, ?, NULL, ?, 'SUCCESS', 'Cash Withdrawal dispensed', ?)";
            try (PreparedStatement ps = conn.prepareStatement(txSql)) {
                ps.setString(1, txId);
                ps.setString(2, accountNum);
                ps.setString(3, cardNum);
                ps.setBigDecimal(4, withdrawalAmt);
                ps.setBigDecimal(5, newBal);
                ps.setString(6, atmId);
                ps.setTimestamp(7, now);
                ps.executeUpdate();
            }

            // 5. Audit Log
            String auditSql = "INSERT INTO audit_logs (card_number, action, details, ip_or_terminal_id, timestamp) VALUES (?, 'WITHDRAW_SUCCESS', ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(auditSql)) {
                ps.setString(1, cardNum);
                ps.setString(2, "Dispensed $" + amount);
                ps.setString(3, atmId);
                ps.setTimestamp(4, now);
                ps.executeUpdate();
            }

            conn.commit();
            return true;

        } catch (SQLException e) {
            System.err.println("Transaction Rollback in executeWithdrawal: " + e.getMessage());
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignored) {}
            }
            return false;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignored) {}
            }
        }
    }

    /**
     * Executes an atomic cash deposit transaction.
     * Credits account balance, increments ATM hardware hopper inventory, writes transaction ledger and audit log.
     */
    public synchronized boolean executeDeposit(String accountNum, String cardNum, int b100, int b50, int b20, int b10, BigDecimal totalDeposit, String atmId) {
        if (!connectedToMySQL) {
            Account acc = fallbackAccounts.get(accountNum);
            CashInventory inv = fallbackCashInventory;
            if (acc == null || inv == null) return false;

            acc.credit(totalDeposit);
            inv.depositCash(b100, b50, b20, b10);
            String txId = "TX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            fallbackTransactions.put(txId, new Transaction(txId, accountNum, cardNum, "DEPOSIT", totalDeposit, acc.getBalance(), null, atmId, "SUCCESS", "Cash Deposit received", LocalDateTime.now()));
            addAuditLog(cardNum, "DEPOSIT_SUCCESS", "Deposited $" + totalDeposit);
            return true;
        }

        Connection conn = null;
        try {
            conn = DatabaseConfig.getConnection();
            conn.setAutoCommit(false);

            // 1. Credit Account
            String creditSql = "UPDATE accounts SET balance = balance + ? WHERE account_number = ? AND status = 'ACTIVE'";
            try (PreparedStatement ps = conn.prepareStatement(creditSql)) {
                ps.setBigDecimal(1, totalDeposit);
                ps.setString(2, accountNum);
                int rows = ps.executeUpdate();
                if (rows == 0) {
                    conn.rollback();
                    return false;
                }
            }

            // 2. Fetch new balance
            BigDecimal newBal;
            String selBalSql = "SELECT balance FROM accounts WHERE account_number = ?";
            try (PreparedStatement ps = conn.prepareStatement(selBalSql)) {
                ps.setString(1, accountNum);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        newBal = rs.getBigDecimal("balance");
                    } else {
                        conn.rollback();
                        return false;
                    }
                }
            }

            // 3. Increment ATM cash inventory
            String invSql = "UPDATE atm_cash_inventory SET bills_100 = bills_100 + ?, bills_50 = bills_50 + ?, bills_20 = bills_20 + ?, bills_10 = bills_10 + ?, " +
                    "total_cash_reserve = total_cash_reserve + ?, last_replenished = NOW() WHERE atm_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(invSql)) {
                ps.setInt(1, b100);
                ps.setInt(2, b50);
                ps.setInt(3, b20);
                ps.setInt(4, b10);
                ps.setBigDecimal(5, totalDeposit);
                ps.setString(6, atmId);
                int rows = ps.executeUpdate();
                if (rows == 0) {
                    conn.rollback();
                    return false;
                }
            }

            // 4. Record Transaction
            String txId = "TX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            Timestamp now = Timestamp.valueOf(LocalDateTime.now());
            String txSql = "INSERT INTO transactions (transaction_id, account_number, card_number, transaction_type, amount, balance_after, beneficiary_account, atm_id, status, remarks, created_at) " +
                    "VALUES (?, ?, ?, 'DEPOSIT', ?, ?, NULL, ?, 'SUCCESS', 'Cash Deposit received', ?)";
            try (PreparedStatement ps = conn.prepareStatement(txSql)) {
                ps.setString(1, txId);
                ps.setString(2, accountNum);
                ps.setString(3, cardNum);
                ps.setBigDecimal(4, totalDeposit);
                ps.setBigDecimal(5, newBal);
                ps.setString(6, atmId);
                ps.setTimestamp(7, now);
                ps.executeUpdate();
            }

            // 5. Audit Log
            String auditSql = "INSERT INTO audit_logs (card_number, action, details, ip_or_terminal_id, timestamp) VALUES (?, 'DEPOSIT_SUCCESS', ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(auditSql)) {
                ps.setString(1, cardNum);
                ps.setString(2, "Deposited $" + totalDeposit);
                ps.setString(3, atmId);
                ps.setTimestamp(4, now);
                ps.executeUpdate();
            }

            conn.commit();
            return true;

        } catch (SQLException e) {
            System.err.println("Transaction Rollback in executeDeposit: " + e.getMessage());
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignored) {}
            }
            return false;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignored) {}
            }
        }
    }
}
