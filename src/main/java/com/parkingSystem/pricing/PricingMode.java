package com.parkingSystem.pricing;

/**
 * How the base amount of a stay is calculated.
 */
public enum PricingMode {
    /** Every billable hour at the space's full hourly rate. */
    HOURLY,
    /** Hours are charged in bands with decreasing rates (see {@code app.pricing.slabs}). */
    SLAB
}
