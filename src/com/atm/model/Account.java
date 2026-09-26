package com.atm.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Account {
    private String accountNumber;
    private int customerId;
    private String accountType; // SAVINGS, CHECKING, CURRENT
    private BigDecimal balance;
    private String currency;
    private String status;      // ACTIVE, FROZEN, CLOSED
    private BigDecimal dailyWithdrawalLimit;
    private LocalDateTime createdAt;

    public Account(String accountNumber, int customerId, String accountType, BigDecimal balance, String currency, String status, BigDecimal dailyWithdrawalLimit, LocalDateTime createdAt) {
        this.accountNumber = accountNumber;
        this.customerId = customerId;
        this.accountType = accountType;
        this.balance = balance;
        this.currency = currency;
        this.status = status;
        this.dailyWithdrawalLimit = dailyWithdrawalLimit;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    public String getAccountNumber() { return accountNumber; }
    public int getCustomerId() { return customerId; }
    public String getAccountType() { return accountType; }
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
    public String getCurrency() { return currency; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public BigDecimal getDailyWithdrawalLimit() { return dailyWithdrawalLimit; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public synchronized boolean debit(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) return false;
        if (this.balance.compareTo(amount) >= 0) {
            this.balance = this.balance.subtract(amount);
            return true;
        }
        return false;
    }

    public synchronized void credit(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) > 0) {
            this.balance = this.balance.add(amount);
        }
    }
}
