package com.atm.service;

import com.atm.db.DatabaseManager;
import com.atm.model.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AtmService {
    private final DatabaseManager db = DatabaseManager.getInstance();

    public static class AuthResult {
        private final boolean success;
        private final String message;
        private final AtmSession session;
        private final boolean cardBlocked;

        public AuthResult(boolean success, String message, AtmSession session, boolean cardBlocked) {
            this.success = success;
            this.message = message;
            this.session = session;
            this.cardBlocked = cardBlocked;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public AtmSession getSession() { return session; }
        public boolean isCardBlocked() { return cardBlocked; }
    }

    public static class OperationResult<T> {
        private final boolean success;
        private final String message;
        private final T data;

        public OperationResult(boolean success, String message, T data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public T getData() { return data; }
    }

    /**
     * Authenticates an ATM card and PIN.
     * Enforces the 3-attempt security rule and automatic card lockout.
     */
    public AuthResult authenticate(String cardNumber, String pin) {
        AtmCard card = db.getCard(cardNumber);
        if (card == null) {
            db.addAuditLog(cardNumber, "AUTH_FAILED", "Card number not recognized");
            return new AuthResult(false, "Invalid card number or card not recognized.", null, false);
        }

        if ("BLOCKED".equalsIgnoreCase(card.getCardStatus())) {
            db.addAuditLog(cardNumber, "AUTH_REJECTED", "Attempt to access locked/blocked card");
            return new AuthResult(false, "This card is BLOCKED due to excessive failed attempts. Please visit a branch.", null, true);
        }

        String hashedAttempt = DatabaseManager.hashPin(pin);
        if (card.getPinHash().equals(hashedAttempt)) {
            // Success
            Account account = db.getAccount(card.getAccountNumber());
            if (account == null) {
                db.addAuditLog(cardNumber, "AUTH_FAILED", "Linked account record missing");
                return new AuthResult(false, "Linked bank account was not found in the system.", null, false);
            }
            if ("CLOSED".equalsIgnoreCase(account.getStatus())) {
                db.addAuditLog(cardNumber, "AUTH_REJECTED", "Attempt to access closed account");
                return new AuthResult(false, "This bank account is CLOSED. Please contact customer service.", null, false);
            }

            card.resetFailedAttempts();
            db.updateCardFailedAttempts(card.getCardNumber(), 0, "ACTIVE");
            Customer customer = db.getCustomer(account.getCustomerId());

            db.addAuditLog(cardNumber, "LOGIN_SUCCESS", "Authenticated successfully as " + card.getCardHolderName());
            AtmSession session = new AtmSession(customer, account, card);
            return new AuthResult(true, "Authentication successful.", session, false);
        } else {
            // Failed attempt
            card.incrementFailedAttempts();
            int remaining = 3 - card.getFailedPinAttempts();

            if (card.getFailedPinAttempts() >= 3) {
                card.setCardStatus("BLOCKED");
                db.updateCardFailedAttempts(cardNumber, card.getFailedPinAttempts(), "BLOCKED");
                db.addAuditLog(cardNumber, "CARD_BLOCKED", "Card permanently blocked after 3 consecutive invalid PIN entries");
                return new AuthResult(false, "Incorrect PIN. Card has been BLOCKED for your security.", null, true);
            } else {
                db.updateCardFailedAttempts(cardNumber, card.getFailedPinAttempts(), "ACTIVE");
                db.addAuditLog(cardNumber, "PIN_FAILED", "Invalid PIN attempt. Remaining attempts: " + remaining);
                return new AuthResult(false, "Incorrect PIN. " + remaining + " attempt(s) remaining.", null, false);
            }
        }
    }

    /**
     * Executes Cash Withdrawal with Denomination breakdown and ATM Cash Inventory verification.
     * Transactional and atomic across account balance, cash inventory, ledger and audit log.
     */
    public OperationResult<Map<Integer, Integer>> withdraw(AtmSession session, int amount) {
        if (amount <= 0 || amount % 10 != 0) {
            return new OperationResult<>(false, "Invalid amount. Must be a multiple of $10.", null);
        }

        Account account = session.getAccount();
        BigDecimal withdrawalAmount = BigDecimal.valueOf(amount);

        if (withdrawalAmount.compareTo(account.getDailyWithdrawalLimit()) > 0) {
            return new OperationResult<>(false, "Amount exceeds daily withdrawal limit of $" + account.getDailyWithdrawalLimit(), null);
        }

        if (account.getBalance().compareTo(withdrawalAmount) < 0) {
            return new OperationResult<>(false, "Insufficient account balance. Available: $" + account.getBalance(), null);
        }

        CashInventory inventory = db.getCashInventory();
        Map<Integer, Integer> dispensedBills = inventory.dispenseCash(amount);
        if (dispensedBills == null) {
            return new OperationResult<>(false, "ATM cannot dispense the exact denomination notes. Please try another amount.", null);
        }

        // Execute persistent transactional withdrawal in database
        boolean success = db.executeWithdrawal(
                account.getAccountNumber(),
                session.getCard().getCardNumber(),
                amount,
                dispensedBills,
                inventory.getAtmId()
        );

        if (!success) {
            return new OperationResult<>(false, "Withdrawal failed. Insufficient funds or cash reserves.", null);
        }

        // Synchronize in-memory session account balance with database
        Account refreshedAccount = db.getAccount(account.getAccountNumber());
        if (refreshedAccount != null) {
            account.setBalance(refreshedAccount.getBalance());
        } else {
            account.debit(withdrawalAmount);
        }

        return new OperationResult<>(true, "Cash dispensed successfully.", dispensedBills);
    }

    /**
     * Executes Cash Deposit and updates Account balance & ATM hopper inventory.
     * Transactional and atomic across account balance, cash inventory, ledger and audit log.
     */
    public OperationResult<BigDecimal> deposit(AtmSession session, int bills100, int bills50, int bills20, int bills10) {
        int totalDeposit = (bills100 * 100) + (bills50 * 50) + (bills20 * 20) + (bills10 * 10);
        if (totalDeposit <= 0) {
            return new OperationResult<>(false, "Deposit amount must be greater than $0.", BigDecimal.ZERO);
        }

        BigDecimal depositAmount = BigDecimal.valueOf(totalDeposit);
        Account account = session.getAccount();
        CashInventory inventory = db.getCashInventory();

        // Execute persistent transactional deposit in database
        boolean success = db.executeDeposit(
                account.getAccountNumber(),
                session.getCard().getCardNumber(),
                bills100, bills50, bills20, bills10,
                depositAmount,
                inventory.getAtmId()
        );

        if (!success) {
            return new OperationResult<>(false, "Deposit transaction failed. Could not persist to database.", account.getBalance());
        }

        // Synchronize in-memory session account balance with database
        Account refreshedAccount = db.getAccount(account.getAccountNumber());
        if (refreshedAccount != null) {
            account.setBalance(refreshedAccount.getBalance());
        } else {
            account.credit(depositAmount);
        }

        return new OperationResult<>(true, "Deposit successful.", account.getBalance());
    }

    /**
     * Transfers funds to another account with ACID guarantees.
     */
    public OperationResult<BigDecimal> transfer(AtmSession session, String beneficiaryAccount, BigDecimal amount) {
        if (beneficiaryAccount == null || beneficiaryAccount.trim().isEmpty()) {
            return new OperationResult<>(false, "Beneficiary account number cannot be empty.", null);
        }
        if (session.getAccount().getAccountNumber().equalsIgnoreCase(beneficiaryAccount.trim())) {
            return new OperationResult<>(false, "Cannot transfer funds to the same source account.", null);
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return new OperationResult<>(false, "Transfer amount must be greater than $0.", null);
        }

        Account destAccount = db.getAccount(beneficiaryAccount.trim());
        if (destAccount == null) {
            return new OperationResult<>(false, "Target beneficiary account was not found in the system.", null);
        }

        boolean success = db.executeTransfer(
                session.getAccount().getAccountNumber(),
                beneficiaryAccount.trim(),
                amount,
                session.getCard().getCardNumber()
        );

        if (!success) {
            return new OperationResult<>(false, "Transfer failed due to insufficient funds or inactive status.", null);
        }

        // Synchronize source account balance with database
        Account refreshedAccount = db.getAccount(session.getAccount().getAccountNumber());
        if (refreshedAccount != null) {
            session.getAccount().setBalance(refreshedAccount.getBalance());
        } else {
            session.getAccount().debit(amount);
        }

        return new OperationResult<>(true, "Successfully transferred $" + amount + " to " + beneficiaryAccount, session.getAccount().getBalance());
    }

    /**
     * Changes card PIN securely with validation and persistent database update.
     */
    public OperationResult<Boolean> changePin(AtmSession session, String oldPin, String newPin, String confirmPin) {
        if (!newPin.equals(confirmPin)) {
            return new OperationResult<>(false, "New PIN and confirmation PIN do not match.", false);
        }
        if (!newPin.matches("\\d{4}")) {
            return new OperationResult<>(false, "PIN must consist of exactly 4 digits.", false);
        }

        String hashedOld = DatabaseManager.hashPin(oldPin);
        AtmCard card = session.getCard();
        if (!card.getPinHash().equals(hashedOld)) {
            db.addAuditLog(card.getCardNumber(), "PIN_CHANGE_FAIL", "Invalid old PIN entered during PIN change");
            return new OperationResult<>(false, "Current PIN is incorrect.", false);
        }

        String hashedNew = DatabaseManager.hashPin(newPin);
        card.setPinHash(hashedNew);
        db.updateCardPin(card.getCardNumber(), hashedNew);
        db.addAuditLog(card.getCardNumber(), "PIN_CHANGE_SUCCESS", "PIN updated successfully");

        // Record non-financial audit transaction
        String txId = "TX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Transaction tx = new Transaction(
                txId, session.getAccount().getAccountNumber(), card.getCardNumber(),
                "PIN_CHANGE", BigDecimal.ZERO, session.getAccount().getBalance(), null,
                "ATM-TERMINAL-01", "SUCCESS", "Card PIN changed at ATM", LocalDateTime.now()
        );
        db.addTransaction(tx);

        return new OperationResult<>(true, "PIN updated successfully.", true);
    }

    public List<Transaction> getMiniStatement(AtmSession session, int limit) {
        List<Transaction> txs = db.getTransactionsForAccount(session.getAccount().getAccountNumber());
        if (txs.size() > limit) {
            return txs.subList(0, limit);
        }
        return txs;
    }
}
