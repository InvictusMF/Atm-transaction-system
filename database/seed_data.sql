-- ============================================================================
-- ATM TRANSACTION SIMULATION SYSTEM - SEED DATA SCRIPT
-- ============================================================================

-- 1. Insert Customers
INSERT INTO customers (customer_id, first_name, last_name, email, phone, address) VALUES
(1, 'Alexander', 'Vance', 'a.vance@apexbank.com', '+1-555-019-2831', '742 Evergreen Terrace, Springfield, OR'),
(2, 'Sophia', 'Williams', 'sophia.w@globalmail.com', '+1-555-044-8821', '100 Broadway Ave, New York, NY'),
(3, 'David', 'Miller', 'd.miller@techcorp.io', '+1-555-082-9912', '450 Silicon Blvd, San Jose, CA'),
(4, 'Emma', 'Watson', 'emma.watson@horizon.org', '+1-555-091-7733', '12 Kensington High St, London, UK');

-- 2. Insert Accounts
-- Customer 1: Primary Checking ($5,420.50) & High-Yield Savings ($18,750.00)
INSERT INTO accounts (account_number, customer_id, account_type, balance, currency, status, daily_withdrawal_limit) VALUES
('ACC-1001-8842', 1, 'SAVINGS', 5420.50, 'USD', 'ACTIVE', 2000.00),
('ACC-1001-8843', 1, 'CHECKING', 18750.00, 'USD', 'ACTIVE', 5000.00),

-- Customer 2: Checking ($3,150.00)
('ACC-2002-4419', 2, 'CHECKING', 3150.00, 'USD', 'ACTIVE', 1500.00),

-- Customer 3: Savings ($820.00)
('ACC-3003-9120', 3, 'SAVINGS', 820.00, 'USD', 'ACTIVE', 1000.00),

-- Customer 4: Checking ($12,400.00)
('ACC-4004-7731', 4, 'CHECKING', 12400.00, 'USD', 'ACTIVE', 3000.00);

-- 3. Insert ATM Cards
-- PIN 1234 -> SHA-256: 03ac674216f3e15c761ee1a5e255f067953623c8b388b4459e13f978d7c846f4
-- PIN 4321 -> SHA-256: d74ff0ee8da3b9806b18c877dbf29bbde50b5bd8e4dad7a3a725000feb82e8f1
-- PIN 9999 -> SHA-256: fa547353f687e59e69f459735661b00154fa03746c12c0c8fa5ecd102b84ed3d
INSERT INTO atm_cards (card_number, account_number, card_holder_name, pin_hash, expiry_date, cvv, card_status, failed_pin_attempts) VALUES
('4532110022334455', 'ACC-1001-8842', 'ALEXANDER VANCE', '03ac674216f3e15c761ee1a5e255f067953623c8b388b4459e13f978d7c846f4', '12/2028', '418', 'ACTIVE', 0),
('5412750088991234', 'ACC-2002-4419', 'SOPHIA WILLIAMS', 'd74ff0ee8da3b9806b18c877dbf29bbde50b5bd8e4dad7a3a725000feb82e8f1', '08/2027', '892', 'ACTIVE', 0),
('4000123456789010', 'ACC-3003-9120', 'DAVID MILLER',    'fa547353f687e59e69f459735661b00154fa03746c12c0c8fa5ecd102b84ed3d', '05/2026', '109', 'BLOCKED', 3);

-- 4. Insert Initial ATM Cash Inventory
INSERT INTO atm_cash_inventory (inventory_id, atm_id, bills_100, bills_50, bills_20, bills_10, total_cash_reserve) VALUES
(1, 'ATM-TERMINAL-01', 500, 600, 1000, 1000, (500*100 + 600*50 + 1000*20 + 1000*10)); -- $110,000.00

-- 5. Insert Transaction Ledger History
INSERT INTO transactions (transaction_id, account_number, card_number, transaction_type, amount, balance_after, beneficiary_account, atm_id, status, remarks, created_at) VALUES
('TX-20260901-001', 'ACC-1001-8842', '4532110022334455', 'DEPOSIT',       2000.00, 5620.50, NULL,            'ATM-TERMINAL-01', 'SUCCESS', 'Salary Cash Deposit', '2026-09-01 10:15:20'),
('TX-20260905-014', 'ACC-1001-8842', '4532110022334455', 'WITHDRAWAL',     200.00, 5420.50, NULL,            'ATM-TERMINAL-01', 'SUCCESS', 'Fast Cash Withdrawal', '2026-09-05 14:32:10'),
('TX-20260910-089', 'ACC-2002-4419', '5412750088991234', 'WITHDRAWAL',     500.00, 3150.00, NULL,            'ATM-TERMINAL-01', 'SUCCESS', 'ATM Cash Dispense',   '2026-09-10 18:02:44'),
('TX-20260918-102', 'ACC-3003-9120', '4000123456789010', 'BALANCE_INQUIRY',  0.00,  820.00, NULL,            'ATM-TERMINAL-01', 'SUCCESS', 'Customer Balance Check', '2026-09-18 09:44:12');

-- 6. Insert Initial Audit Logs
INSERT INTO audit_logs (log_id, card_number, action, details, ip_or_terminal_id, timestamp) VALUES
(1, '4532110022334455', 'CARD_INSERTED', 'Card verified and session initialized successfully', 'ATM-TERMINAL-01', '2026-09-05 14:31:55'),
(2, '4532110022334455', 'PIN_SUCCESS',   'Valid PIN entered by customer Alexander Vance', 'ATM-TERMINAL-01', '2026-09-05 14:32:02'),
(3, '4000123456789010', 'CARD_BLOCKED', 'Exceeded maximum allowable attempts (3 consecutive failures)', 'ATM-TERMINAL-01', '2026-09-19 22:15:30');
