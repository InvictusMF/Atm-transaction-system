-- ============================================================================
-- ATM TRANSACTION SIMULATION SYSTEM - DATABASE SCHEMA (DDL)
-- ============================================================================
-- Target RDBMS: Relational Database (SQLite / PostgreSQL / MySQL compliant)
-- Version: 1.0.0
-- Description: Core relational schema governing customers, accounts, ATM cards,
--              financial transactions, ATM hardware inventory, and security audit logs.
-- ============================================================================

-- Drop existing tables in reverse dependency order
DROP TABLE IF EXISTS audit_logs;
DROP TABLE IF EXISTS transactions;
DROP TABLE IF EXISTS atm_cash_inventory;
DROP TABLE IF EXISTS atm_cards;
DROP TABLE IF EXISTS accounts;
DROP TABLE IF EXISTS customers;

-- ----------------------------------------------------------------------------
-- 1. TABLE: customers
-- Stores primary biographical information for bank account holders.
-- ----------------------------------------------------------------------------
CREATE TABLE customers (
    customer_id         INTEGER PRIMARY KEY AUTOINCREMENT,
    first_name          VARCHAR(50)  NOT NULL,
    last_name           VARCHAR(50)  NOT NULL,
    email               VARCHAR(100) NOT NULL UNIQUE,
    phone               VARCHAR(20)  NOT NULL UNIQUE,
    address             VARCHAR(255),
    created_at          DATETIME     DEFAULT CURRENT_TIMESTAMP
);

-- ----------------------------------------------------------------------------
-- 2. TABLE: accounts
-- Stores bank account details linked to a specific customer.
-- ----------------------------------------------------------------------------
CREATE TABLE accounts (
    account_number          VARCHAR(20)    PRIMARY KEY,
    customer_id             INTEGER        NOT NULL,
    account_type            VARCHAR(20)    NOT NULL CHECK (account_type IN ('SAVINGS', 'CHECKING', 'CURRENT')),
    balance                 DECIMAL(12, 2) NOT NULL DEFAULT 0.00 CHECK (balance >= 0.00),
    currency                VARCHAR(3)     NOT NULL DEFAULT 'USD',
    status                  VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'FROZEN', 'CLOSED')),
    daily_withdrawal_limit  DECIMAL(10, 2) NOT NULL DEFAULT 1000.00,
    created_at              DATETIME       DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE RESTRICT
);

-- Index on customer_id for fast account lookup
CREATE INDEX idx_accounts_customer ON accounts(customer_id);

-- ----------------------------------------------------------------------------
-- 3. TABLE: atm_cards
-- Stores debit/ATM card credentials, hashed PINs, and security locks.
-- ----------------------------------------------------------------------------
CREATE TABLE atm_cards (
    card_number             VARCHAR(16) PRIMARY KEY,
    account_number          VARCHAR(20) NOT NULL,
    card_holder_name        VARCHAR(100) NOT NULL,
    pin_hash                VARCHAR(64)  NOT NULL, -- SHA-256 hashed 4-digit PIN
    expiry_date             VARCHAR(7)   NOT NULL, -- Format MM/YYYY
    cvv                     VARCHAR(3)   NOT NULL,
    card_status             VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' CHECK (card_status IN ('ACTIVE', 'BLOCKED', 'EXPIRED')),
    failed_pin_attempts     INTEGER      NOT NULL DEFAULT 0 CHECK (failed_pin_attempts >= 0),
    issued_at               DATETIME     DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (account_number) REFERENCES accounts(account_number) ON DELETE CASCADE
);

-- Index on account_number for fast card lookup
CREATE INDEX idx_cards_account ON atm_cards(account_number);

-- ----------------------------------------------------------------------------
-- 4. TABLE: transactions
-- Immutable ledger recording every ATM operation (withdrawal, deposit, transfer, etc.)
-- ----------------------------------------------------------------------------
CREATE TABLE transactions (
    transaction_id          VARCHAR(36)    PRIMARY KEY, -- UUID v4
    account_number          VARCHAR(20)    NOT NULL,
    card_number             VARCHAR(16),
    transaction_type        VARCHAR(30)    NOT NULL CHECK (
        transaction_type IN ('WITHDRAWAL', 'DEPOSIT', 'TRANSFER_OUT', 'TRANSFER_IN', 'BALANCE_INQUIRY', 'PIN_CHANGE')
    ),
    amount                  DECIMAL(12, 2) NOT NULL DEFAULT 0.00 CHECK (amount >= 0.00),
    balance_after           DECIMAL(12, 2) NOT NULL,
    beneficiary_account     VARCHAR(20),
    atm_id                  VARCHAR(20)    NOT NULL DEFAULT 'ATM-TERMINAL-01',
    status                  VARCHAR(20)    NOT NULL DEFAULT 'SUCCESS' CHECK (status IN ('SUCCESS', 'FAILED', 'REVERSED')),
    remarks                 VARCHAR(255),
    created_at              DATETIME       DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (account_number) REFERENCES accounts(account_number) ON DELETE RESTRICT,
    FOREIGN KEY (card_number) REFERENCES atm_cards(card_number) ON DELETE SET NULL
);

-- Index on account_number and timestamp for instant mini-statement retrieval
CREATE INDEX idx_tx_account_date ON transactions(account_number, created_at DESC);

-- ----------------------------------------------------------------------------
-- 5. TABLE: atm_cash_inventory
-- Monitors cash cassette levels inside the physical ATM kiosk (denominated notes)
-- ----------------------------------------------------------------------------
CREATE TABLE atm_cash_inventory (
    inventory_id            INTEGER        PRIMARY KEY AUTOINCREMENT,
    atm_id                  VARCHAR(20)    NOT NULL UNIQUE,
    bills_100               INTEGER        NOT NULL DEFAULT 500 CHECK (bills_100 >= 0),
    bills_50                INTEGER        NOT NULL DEFAULT 500 CHECK (bills_50 >= 0),
    bills_20                INTEGER        NOT NULL DEFAULT 500 CHECK (bills_20 >= 0),
    bills_10                INTEGER        NOT NULL DEFAULT 500 CHECK (bills_10 >= 0),
    total_cash_reserve      DECIMAL(12, 2) NOT NULL,
    last_replenished        DATETIME       DEFAULT CURRENT_TIMESTAMP
);

-- ----------------------------------------------------------------------------
-- 6. TABLE: audit_logs
-- Security audit trail logging authentication failures, card lockouts, and critical events.
-- ----------------------------------------------------------------------------
CREATE TABLE audit_logs (
    log_id                  INTEGER      PRIMARY KEY AUTOINCREMENT,
    card_number             VARCHAR(16),
    action                  VARCHAR(50)  NOT NULL,
    details                 VARCHAR(255) NOT NULL,
    ip_or_terminal_id       VARCHAR(30)  DEFAULT 'ATM-TERMINAL-01',
    timestamp               DATETIME     DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_card ON audit_logs(card_number);
