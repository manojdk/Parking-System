package com.parkingSystem.enums;

/**
 * Status of one payment attempt. PENDING is the only open state.
 */
public enum PaymentStatus {
    /** Created at the provider, waiting for its confirmation webhook. */
    PENDING,
    /** The provider confirmed the money; the bill is PAID. */
    SUCCEEDED,
    /** The provider reported a failure; the bill stays PENDING and the user can try again. */
    FAILED,
    /** No confirmation arrived in time; a new attempt is allowed. */
    EXPIRED,
    /**
     * The provider took the money but the bill was already settled (or its amount changed) by then, for example
     * a late confirmation of an expired attempt. Needs a refund; never marks the bill paid twice.
     */
    REFUND_REQUIRED
}
