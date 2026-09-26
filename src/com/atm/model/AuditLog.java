package com.atm.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AuditLog {
    private int logId;
    private String cardNumber;
    private String action;
    private String details;
    private String ipOrTerminalId;
    private LocalDateTime timestamp;

    public AuditLog(int logId, String cardNumber, String action, String details, String ipOrTerminalId, LocalDateTime timestamp) {
        this.logId = logId;
        this.cardNumber = cardNumber;
        this.action = action;
        this.details = details;
        this.ipOrTerminalId = ipOrTerminalId;
        this.timestamp = timestamp != null ? timestamp : LocalDateTime.now();
    }

    public int getLogId() { return logId; }
    public String getCardNumber() { return cardNumber; }
    public String getAction() { return action; }
    public String getDetails() { return details; }
    public String getIpOrTerminalId() { return ipOrTerminalId; }
    public LocalDateTime getTimestamp() { return timestamp; }

    public String getFormattedTimestamp() {
        return timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
