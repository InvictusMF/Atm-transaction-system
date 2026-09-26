package com.atm.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

public class CashInventory {
    private int inventoryId;
    private String atmId;
    private int bills100;
    private int bills50;
    private int bills20;
    private int bills10;
    private BigDecimal totalCashReserve;
    private LocalDateTime lastReplenished;

    public CashInventory(int inventoryId, String atmId, int bills100, int bills50, int bills20, int bills10, LocalDateTime lastReplenished) {
        this.inventoryId = inventoryId;
        this.atmId = atmId;
        this.bills100 = bills100;
        this.bills50 = bills50;
        this.bills20 = bills20;
        this.bills10 = bills10;
        this.lastReplenished = lastReplenished != null ? lastReplenished : LocalDateTime.now();
        recalculateTotal();
    }

    public synchronized void recalculateTotal() {
        long total = (bills100 * 100L) + (bills50 * 50L) + (bills20 * 20L) + (bills10 * 10L);
        this.totalCashReserve = BigDecimal.valueOf(total).setScale(2);
    }

    public int getInventoryId() { return inventoryId; }
    public String getAtmId() { return atmId; }
    public int getBills100() { return bills100; }
    public int getBills50() { return bills50; }
    public int getBills20() { return bills20; }
    public int getBills10() { return bills10; }
    public BigDecimal getTotalCashReserve() { return totalCashReserve; }
    public LocalDateTime getLastReplenished() { return lastReplenished; }

    /**
     * Attempts to dispense cash using greedy denomination calculation.
     * Returns a map of Denomination -> Count if feasible, or null if insufficient cash.
     */
    public synchronized Map<Integer, Integer> dispenseCash(int amount) {
        if (amount <= 0 || amount % 10 != 0) return null;
        if (BigDecimal.valueOf(amount).compareTo(totalCashReserve) > 0) return null;

        int rem = amount;
        int take100 = Math.min(rem / 100, bills100);
        rem -= take100 * 100;

        int take50 = Math.min(rem / 50, bills50);
        rem -= take50 * 50;

        int take20 = Math.min(rem / 20, bills20);
        rem -= take20 * 20;

        int take10 = Math.min(rem / 10, bills10);
        rem -= take10 * 10;

        if (rem == 0) {
            // Commit decrement
            bills100 -= take100;
            bills50 -= take50;
            bills20 -= take20;
            bills10 -= take10;
            recalculateTotal();

            Map<Integer, Integer> result = new LinkedHashMap<>();
            if (take100 > 0) result.put(100, take100);
            if (take50 > 0) result.put(50, take50);
            if (take20 > 0) result.put(20, take20);
            if (take10 > 0) result.put(10, take10);
            return result;
        }

        return null; // Unable to satisfy exact notes
    }

    public synchronized void depositCash(int n100, int n50, int n20, int n10) {
        if (n100 > 0) bills100 += n100;
        if (n50 > 0) bills50 += n50;
        if (n20 > 0) bills20 += n20;
        if (n10 > 0) bills10 += n10;
        recalculateTotal();
    }
}
