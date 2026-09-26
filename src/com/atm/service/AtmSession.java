package com.atm.service;

import com.atm.model.Account;
import com.atm.model.AtmCard;
import com.atm.model.Customer;

import java.time.LocalDateTime;

public class AtmSession {
    private final Customer customer;
    private final Account account;
    private final AtmCard card;
    private final LocalDateTime loginTime;

    public AtmSession(Customer customer, Account account, AtmCard card) {
        this.customer = customer;
        this.account = account;
        this.card = card;
        this.loginTime = LocalDateTime.now();
    }

    public Customer getCustomer() { return customer; }
    public Account getAccount() { return account; }
    public AtmCard getCard() { return card; }
    public LocalDateTime getLoginTime() { return loginTime; }
}
