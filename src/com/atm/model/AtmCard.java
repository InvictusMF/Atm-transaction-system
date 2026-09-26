package com.atm.model;

import java.time.LocalDateTime;

public class AtmCard {
    private String cardNumber;
    private String accountNumber;
    private String cardHolderName;
    private String pinHash;
    private String expiryDate;
    private String cvv;
    private String cardStatus; // ACTIVE, BLOCKED, EXPIRED
    private int failedPinAttempts;
    private LocalDateTime issuedAt;

    public AtmCard(String cardNumber, String accountNumber, String cardHolderName, String pinHash, String expiryDate, String cvv, String cardStatus, int failedPinAttempts, LocalDateTime issuedAt) {
        this.cardNumber = cardNumber;
        this.accountNumber = accountNumber;
        this.cardHolderName = cardHolderName;
        this.pinHash = pinHash;
        this.expiryDate = expiryDate;
        this.cvv = cvv;
        this.cardStatus = cardStatus;
        this.failedPinAttempts = failedPinAttempts;
        this.issuedAt = issuedAt != null ? issuedAt : LocalDateTime.now();
    }

    public String getCardNumber() { return cardNumber; }
    public String getMaskedCardNumber() {
        if (cardNumber.length() == 16) {
            return cardNumber.substring(0, 4) + " •••• •••• " + cardNumber.substring(12);
        }
        return cardNumber;
    }
    public String getAccountNumber() { return accountNumber; }
    public String getCardHolderName() { return cardHolderName; }
    public String getPinHash() { return pinHash; }
    public void setPinHash(String pinHash) { this.pinHash = pinHash; }
    public String getExpiryDate() { return expiryDate; }
    public String getCvv() { return cvv; }
    public String getCardStatus() { return cardStatus; }
    public void setCardStatus(String cardStatus) { this.cardStatus = cardStatus; }
    public int getFailedPinAttempts() { return failedPinAttempts; }
    public void setFailedPinAttempts(int failedPinAttempts) { this.failedPinAttempts = failedPinAttempts; }
    public void incrementFailedAttempts() { this.failedPinAttempts++; }
    public void resetFailedAttempts() { this.failedPinAttempts = 0; }
    public LocalDateTime getIssuedAt() { return issuedAt; }
}
