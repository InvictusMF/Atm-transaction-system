package com.atm.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Transaction {
    private String transactionId;
    private String accountNumber;
    private String cardNumber;
    private String transactionType; // WITHDRAWAL, DEPOSIT, TRANSFER_OUT, TRANSFER_IN, BALANCE_INQUIRY, PIN_CHANGE
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private String beneficiaryAccount;
    private String atmId;
    private String status;          // SUCCESS, FAILED, REVERSED
    private String remarks;
    private LocalDateTime createdAt;

    public Transaction(String transactionId, String accountNumber, String cardNumber, String transactionType, BigDecimal amount, BigDecimal balanceAfter, String beneficiaryAccount, String atmId, String status, String remarks, LocalDateTime createdAt) {
        this.transactionId = transactionId;
        this.accountNumber = accountNumber;
        this.cardNumber = cardNumber;
        this.transactionType = transactionType;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.beneficiaryAccount = beneficiaryAccount;
        this.atmId = atmId;
        this.status = status;
        this.remarks = remarks;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    public String getTransactionId() { return transactionId; }
    public String getAccountNumber() { return accountNumber; }
    public String getCardNumber() { return cardNumber; }
    public String getTransactionType() { return transactionType; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public String getBeneficiaryAccount() { return beneficiaryAccount; }
    public String getAtmId() { return atmId; }
    public String getStatus() { return status; }
    public String getRemarks() { return remarks; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public String getFormattedDate() {
        return createdAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
