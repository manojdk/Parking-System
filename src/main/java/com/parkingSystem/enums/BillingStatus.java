package com.parkingSystem.enums;

/**
 * Billing status enum
 */
public enum BillingStatus {
    PENDING("Pending"),
    PAID("Paid"),
    CANCELLED("Cancelled");

    private final String displayName;

    BillingStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

