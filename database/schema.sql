-- ============================================================================
-- APEX NATIONAL BANK - ATM TRANSACTION SIMULATION SYSTEM
-- TARGET RDBMS: MySQL 8.0+ (InnoDB Engine)
-- DATABASE: atm_simulation
-- ============================================================================

CREATE DATABASE IF NOT EXISTS atm_simulation
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE atm_simulation;

-- Disable foreign key checks during schema rebuild
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS audit_logs;
DROP TABLE IF EXISTS transactions;
DROP TABLE IF EXISTS atm_cash_inventory;
DROP TABLE IF EXISTS atm_cards;
DROP TABLE IF EXISTS accounts;
DROP TABLE IF EXISTS customers;

SET FOREIGN_KEY_CHECKS = 1;

-- ----------------------------------------------------------------------------
-- 1. TABLE: customers
-- Stores primary biographical information for bank account holders.
-- ----------------------------------------------------------------------------
CREATE TABLE customers (
    customer_id         INT AUTO_INCREMENT PRIMARY KEY,
    first_name          VARCHAR(50)  NOT NULL,
    last_name           VARCHAR(50)  NOT NULL,
    email               VARCHAR(100) NOT NULL UNIQUE,
    phone               VARCHAR(20)  NOT NULL UNIQUE,
    address             VARCHAR(255),
    created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- ----------------------------------------------------------------------------
-- 2. TABLE: accounts
-- Stores bank account details linked to a specific customer.
-- ----------------------------------------------------------------------------
CREATE TABLE accounts (
    account_number          VARCHAR(20)    PRIMARY KEY,
    customer_id             INT            NOT NULL,
    account_type            VARCHAR(20)    NOT NULL,
    balance                 DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    currency                VARCHAR(3)     NOT NULL DEFAULT 'USD',
    status                  VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE',
    daily_withdrawal_limit  DECIMAL(10, 2) NOT NULL DEFAULT 1000.00,
    created_at              DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_account_type CHECK (account_type IN ('SAVINGS', 'CHECKING', 'CURRENT')),
    CONSTRAINT chk_account_status CHECK (status IN ('ACTIVE', 'FROZEN', 'CLOSED')),
    CONSTRAINT chk_account_balance CHECK (balance >= 0.00),
    CONSTRAINT fk_accounts_customer FOREIGN KEY (customer_id) 
        REFERENCES customers(customer_id) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB;

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
    card_status             VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    failed_pin_attempts     INT          NOT NULL DEFAULT 0,
    issued_at               DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_card_status CHECK (card_status IN ('ACTIVE', 'BLOCKED', 'EXPIRED')),
    CONSTRAINT chk_card_failed_attempts CHECK (failed_pin_attempts >= 0),
    CONSTRAINT fk_cards_account FOREIGN KEY (account_number) 
        REFERENCES accounts(account_number) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

CREATE INDEX idx_cards_account ON atm_cards(account_number);

-- ----------------------------------------------------------------------------
-- 4. TABLE: transactions
-- Immutable ledger recording every ATM operation (withdrawal, deposit, transfer, etc.)
-- ----------------------------------------------------------------------------
CREATE TABLE transactions (
    transaction_id          VARCHAR(36)    PRIMARY KEY,
    account_number          VARCHAR(20)    NOT NULL,
    card_number             VARCHAR(16),
    transaction_type        VARCHAR(30)    NOT NULL,
    amount                  DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    balance_after           DECIMAL(12, 2) NOT NULL,
    beneficiary_account     VARCHAR(20),
    atm_id                  VARCHAR(20)    NOT NULL DEFAULT 'ATM-TERMINAL-01',
    status                  VARCHAR(20)    NOT NULL DEFAULT 'SUCCESS',
    remarks                 VARCHAR(255),
    created_at              DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_tx_type CHECK (
        transaction_type IN ('WITHDRAWAL', 'DEPOSIT', 'TRANSFER_OUT', 'TRANSFER_IN', 'BALANCE_INQUIRY', 'PIN_CHANGE')
    ),
    CONSTRAINT chk_tx_status CHECK (status IN ('SUCCESS', 'FAILED', 'REVERSED')),
    CONSTRAINT chk_tx_amount CHECK (amount >= 0.00),
    CONSTRAINT fk_tx_account FOREIGN KEY (account_number) 
        REFERENCES accounts(account_number) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_tx_card FOREIGN KEY (card_number) 
        REFERENCES atm_cards(card_number) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB;

CREATE INDEX idx_tx_account_date ON transactions(account_number, created_at DESC);

-- ----------------------------------------------------------------------------
-- 5. TABLE: atm_cash_inventory
-- Monitors cash cassette levels inside the physical ATM kiosk (denominated notes)
-- ----------------------------------------------------------------------------
CREATE TABLE atm_cash_inventory (
    inventory_id            INT            AUTO_INCREMENT PRIMARY KEY,
    atm_id                  VARCHAR(20)    NOT NULL UNIQUE,
    bills_100               INT            NOT NULL DEFAULT 500,
    bills_50                INT            NOT NULL DEFAULT 500,
    bills_20                INT            NOT NULL DEFAULT 500,
    bills_10                INT            NOT NULL DEFAULT 500,
    total_cash_reserve      DECIMAL(12, 2) NOT NULL,
    last_replenished        DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_bills_100 CHECK (bills_100 >= 0),
    CONSTRAINT chk_bills_50  CHECK (bills_50 >= 0),
    CONSTRAINT chk_bills_20  CHECK (bills_20 >= 0),
    CONSTRAINT chk_bills_10  CHECK (bills_10 >= 0),
    CONSTRAINT chk_cash_reserve CHECK (total_cash_reserve >= 0.00)
) ENGINE=InnoDB;

-- ----------------------------------------------------------------------------
-- 6. TABLE: audit_logs
-- Security audit trail logging authentication failures, card lockouts, and critical events.
-- ----------------------------------------------------------------------------
CREATE TABLE audit_logs (
    log_id                  INT          AUTO_INCREMENT PRIMARY KEY,
    card_number             VARCHAR(16),
    action                  VARCHAR(50)  NOT NULL,
    details                 VARCHAR(255) NOT NULL,
    ip_or_terminal_id       VARCHAR(30)  DEFAULT 'ATM-TERMINAL-01',
    timestamp               DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE INDEX idx_audit_card ON audit_logs(card_number);
