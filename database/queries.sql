-- ============================================================================
-- ATM TRANSACTION SIMULATION SYSTEM - VERIFICATION & BUSINESS QUERIES
-- ============================================================================

-- Query 1: Verify Customer Portfolio & Linked Card Status
SELECT 
    c.customer_id,
    c.first_name || ' ' || c.last_name AS customer_name,
    a.account_number,
    a.account_type,
    printf('$%.2f', a.balance) AS current_balance,
    card.card_number,
    card.card_status,
    card.failed_pin_attempts
FROM customers c
JOIN accounts a ON c.customer_id = a.customer_id
LEFT JOIN atm_cards card ON a.account_number = card.account_number
ORDER BY c.customer_id;

-- Query 2: Mini-Statement for a Specific Account (Last 5 Transactions)
SELECT 
    t.transaction_id,
    t.created_at,
    t.transaction_type,
    printf('$%.2f', t.amount) AS amount,
    printf('$%.2f', t.balance_after) AS balance_after,
    COALESCE(t.beneficiary_account, 'N/A') AS recipient,
    t.status,
    t.remarks
FROM transactions t
WHERE t.account_number = 'ACC-1001-8842'
ORDER BY t.created_at DESC
LIMIT 5;

-- Query 3: ATM Cash Inventory Level Check
SELECT 
    atm_id,
    bills_100 AS "$100 Bills",
    bills_50  AS "$50 Bills",
    bills_20  AS "$20 Bills",
    bills_10  AS "$10 Bills",
    printf('$%.2f', total_cash_reserve) AS total_reserve,
    last_replenished
FROM atm_cash_inventory;

-- Query 4: Daily Total Dispensed by ATM
SELECT 
    atm_id,
    DATE(created_at) AS tx_date,
    COUNT(*) AS total_withdrawals,
    printf('$%.2f', SUM(amount)) AS total_dispensed_amount
FROM transactions
WHERE transaction_type = 'WITHDRAWAL' AND status = 'SUCCESS'
GROUP BY atm_id, DATE(created_at);

-- Query 5: Security & Fraud Audit Log Review
SELECT 
    log_id,
    card_number,
    action,
    details,
    timestamp
FROM audit_logs
ORDER BY timestamp DESC;
