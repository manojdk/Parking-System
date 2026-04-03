package com.parkingSystem.enums;

/**
 * Ticket status enum for parking tickets
 */
public enum TicketStatus {
    ACTIVE("Active"),
    CLOSED("Closed"),
    CANCELLED("Cancelled");

    private final String displayName;

    TicketStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

