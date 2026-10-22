package com.atm.test;

import com.atm.db.DatabaseConfig;
import com.atm.db.DatabaseManager;
import com.atm.model.*;
import com.atm.service.AtmService;
import com.atm.service.AtmSession;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Automated end-to-end integration test suite verifying live MySQL persistence,
 * authentication, ACID transactional workflows, rollback on failure, and inventory updates.
 */
public class DatabaseIntegrationTest {

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("  RUNNING APEX BANK ATM MYSQL INTEGRATION TESTS  ");
        System.out.println("=================================================");

        int totalTests = 0;
        int passedTests = 0;

        // 1. Connection Test
        totalTests++;
        System.out.print("[TEST 1] Testing MySQL Connection... ");
        DatabaseConfig.ConnectionTestResult connRes = DatabaseConfig.testConnection();
        if (connRes.isSuccess()) {
            System.out.println("PASSED (" + connRes.getMessage() + ")");
            passedTests++;
        } else {
            System.out.println("FAILED (" + connRes.getMessage() + ")");
            System.err.println("Details: " + connRes.getDetails());
            System.exit(1);
        }

        DatabaseManager db = DatabaseManager.getInstance();
        AtmService service = new AtmService();

        // 2. Authentication Test
        totalTests++;
        System.out.print("[TEST 2] Testing Demo Card Authentication (Alexander Vance)... ");
        AtmService.AuthResult authRes = service.authenticate("4532110022334455", "1234");
        if (authRes.isSuccess() && authRes.getSession() != null) {
            System.out.println("PASSED");
            passedTests++;
        } else {
            System.out.println("FAILED: " + authRes.getMessage());
        }

        AtmSession session = authRes.getSession();

        // 3. Balance Inquiry Test
        totalTests++;
        System.out.print("[TEST 3] Testing Balance Inquiry... ");
        BigDecimal initialBalance = session.getAccount().getBalance();
        Account freshAcc = db.getAccount(session.getAccount().getAccountNumber());
        if (freshAcc != null && freshAcc.getBalance().compareTo(BigDecimal.ZERO) > 0) {
            System.out.println("PASSED (Balance: $" + freshAcc.getBalance() + ")");
            passedTests++;
        } else {
            System.out.println("FAILED");
        }

        // 4. Cash Withdrawal Test
        totalTests++;
        System.out.print("[TEST 4] Testing Cash Withdrawal ($60)... ");
        CashInventory invBefore = db.getCashInventory();
        BigDecimal reserveBefore = invBefore.getTotalCashReserve();
        BigDecimal balanceBeforeWithdraw = db.getAccount(session.getAccount().getAccountNumber()).getBalance();

        AtmService.OperationResult<Map<Integer, Integer>> withdrawRes = service.withdraw(session, 60);
        Account accAfterWithdraw = db.getAccount(session.getAccount().getAccountNumber());
        CashInventory invAfter = db.getCashInventory();

        if (withdrawRes.isSuccess()
                && accAfterWithdraw.getBalance().compareTo(balanceBeforeWithdraw.subtract(BigDecimal.valueOf(60))) == 0
                && invAfter.getTotalCashReserve().compareTo(reserveBefore.subtract(BigDecimal.valueOf(60))) == 0) {
            System.out.println("PASSED (Dispensed notes: " + withdrawRes.getData() + ", New Balance: $" + accAfterWithdraw.getBalance() + ")");
            passedTests++;
        } else {
            System.out.println("FAILED: " + withdrawRes.getMessage());
        }

        // 5. Cash Deposit Test
        totalTests++;
        System.out.print("[TEST 5] Testing Cash Deposit (1x$100, 1x$50 = $150)... ");
        BigDecimal balanceBeforeDeposit = db.getAccount(session.getAccount().getAccountNumber()).getBalance();
        BigDecimal reserveBeforeDeposit = db.getCashInventory().getTotalCashReserve();

        AtmService.OperationResult<BigDecimal> depositRes = service.deposit(session, 1, 1, 0, 0);
        Account accAfterDeposit = db.getAccount(session.getAccount().getAccountNumber());
        BigDecimal reserveAfterDeposit = db.getCashInventory().getTotalCashReserve();

        if (depositRes.isSuccess()
                && accAfterDeposit.getBalance().compareTo(balanceBeforeDeposit.add(BigDecimal.valueOf(150))) == 0
                && reserveAfterDeposit.compareTo(reserveBeforeDeposit.add(BigDecimal.valueOf(150))) == 0) {
            System.out.println("PASSED (New Balance: $" + accAfterDeposit.getBalance() + ")");
            passedTests++;
        } else {
            System.out.println("FAILED: " + depositRes.getMessage());
        }

        // 6. Fund Transfer Test (Sender -> Recipient)
        totalTests++;
        System.out.print("[TEST 6] Testing Fund Transfer ($100 to ACC-1001-8843)... ");
        String destAccountNum = "ACC-1001-8843";
        BigDecimal senderBeforeTransfer = db.getAccount(session.getAccount().getAccountNumber()).getBalance();
        BigDecimal recipientBeforeTransfer = db.getAccount(destAccountNum).getBalance();

        AtmService.OperationResult<BigDecimal> transferRes = service.transfer(session, destAccountNum, BigDecimal.valueOf(100));
        BigDecimal senderAfterTransfer = db.getAccount(session.getAccount().getAccountNumber()).getBalance();
        BigDecimal recipientAfterTransfer = db.getAccount(destAccountNum).getBalance();

        if (transferRes.isSuccess()
                && senderAfterTransfer.compareTo(senderBeforeTransfer.subtract(BigDecimal.valueOf(100))) == 0
                && recipientAfterTransfer.compareTo(recipientBeforeTransfer.add(BigDecimal.valueOf(100))) == 0) {
            System.out.println("PASSED (Sender: $" + senderAfterTransfer + ", Recipient: $" + recipientAfterTransfer + ")");
            passedTests++;
        } else {
            System.out.println("FAILED: " + transferRes.getMessage());
        }

        // 7. ACID Rollback on Insufficient Balance
        totalTests++;
        System.out.print("[TEST 7] Testing Transaction Rollback on Overdraft ($999,999.00)... ");
        BigDecimal senderBeforeFail = db.getAccount(session.getAccount().getAccountNumber()).getBalance();
        BigDecimal recipientBeforeFail = db.getAccount(destAccountNum).getBalance();

        AtmService.OperationResult<BigDecimal> failTransferRes = service.transfer(session, destAccountNum, new BigDecimal("999999.00"));
        BigDecimal senderAfterFail = db.getAccount(session.getAccount().getAccountNumber()).getBalance();
        BigDecimal recipientAfterFail = db.getAccount(destAccountNum).getBalance();

        if (!failTransferRes.isSuccess()
                && senderAfterFail.compareTo(senderBeforeFail) == 0
                && recipientAfterFail.compareTo(recipientBeforeFail) == 0) {
            System.out.println("PASSED (Atomic rollback verified; balances unchanged)");
            passedTests++;
        } else {
            System.out.println("FAILED: Transaction did not roll back properly!");
        }

        // 8. Mini Statement Test
        totalTests++;
        System.out.print("[TEST 8] Testing Mini Statement retrieval... ");
        List<Transaction> miniStatement = service.getMiniStatement(session, 5);
        if (miniStatement != null && !miniStatement.isEmpty()) {
            System.out.println("PASSED (Retrieved " + miniStatement.size() + " latest transactions from MySQL)");
            passedTests++;
        } else {
            System.out.println("FAILED");
        }

        // 9. PIN Change & Security Verification
        totalTests++;
        System.out.print("[TEST 9] Testing PIN Change (1234 -> 7890 -> 1234)... ");
        AtmService.OperationResult<Boolean> changePinRes = service.changePin(session, "1234", "7890", "7890");
        AtmCard cardWithNewPin = db.getCard(session.getCard().getCardNumber());
        boolean pinChangedInDb = cardWithNewPin.getPinHash().equals(DatabaseManager.hashPin("7890"));

        // Revert PIN back to 1234
        AtmService.OperationResult<Boolean> revertPinRes = service.changePin(session, "7890", "1234", "1234");
        AtmCard cardWithRevertedPin = db.getCard(session.getCard().getCardNumber());
        boolean pinRevertedInDb = cardWithRevertedPin.getPinHash().equals(DatabaseManager.hashPin("1234"));

        if (changePinRes.isSuccess() && pinChangedInDb && revertPinRes.isSuccess() && pinRevertedInDb) {
            System.out.println("PASSED (Persistent SHA-256 PIN hash update confirmed in MySQL)");
            passedTests++;
        } else {
            System.out.println("FAILED");
        }

        // 10. Persistence Across Restarts Test
        totalTests++;
        System.out.print("[TEST 10] Testing Persistence Verification (Fresh connection query)... ");
        Account reloadedAcc = db.getAccount(session.getAccount().getAccountNumber());
        if (reloadedAcc != null && reloadedAcc.getBalance().compareTo(session.getAccount().getBalance()) == 0) {
            System.out.println("PASSED (Balance $" + reloadedAcc.getBalance() + " persisted in MySQL database)");
            passedTests++;
        } else {
            System.out.println("FAILED");
        }

        System.out.println("=================================================");
        System.out.println("  SUMMARY: " + passedTests + " / " + totalTests + " TESTS PASSED");
        System.out.println("=================================================");

        if (passedTests == totalTests) {
            System.out.println("All MySQL JDBC database integration tests completed successfully!");
        } else {
            System.exit(1);
        }
    }
}
