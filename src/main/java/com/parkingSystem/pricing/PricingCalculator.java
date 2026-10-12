package com.parkingSystem.pricing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.stereotype.Component;

/**
 * Turns a stay (minutes at an hourly rate) into a price. Pure calculation, no database: all the pricing
 * rules live here and are unit-tested on their own.
 * <ol>
 * <li><b>Grace period</b>: a stay of at most {@code gracePeriodMinutes} is free.</li>
 * <li><b>Billable hours</b>: every started hour counts, minimum one hour.</li>
 * <li><b>Base amount</b>: hours x rate (HOURLY), or by slab bands with decreasing rates (SLAB).</li>
 * <li><b>Daily cap</b>: the base never exceeds {@code dailyCapHours} x rate per started 24 hours.</li>
 * <li><b>Tax</b>: {@code taxRate} x base. Amounts are rounded to 2 decimals, half up.</li>
 * </ol>
 */
@Component
public class PricingCalculator {

    private static final int HOURS_PER_DAY = 24;

    private final PricingProperties properties;

    public PricingCalculator(PricingProperties properties) {
        validate(properties);
        this.properties = properties;
    }

    /**
     * Price of a stay.
     *
     * @param billableHours hours charged (0 for a free stay)
     * @param freeStay      true if the grace period made the stay free
     * @param notes         human-readable remarks for the bill, or null
     */
    public record Quote(BigDecimal billableHours, BigDecimal baseAmount, BigDecimal taxAmount, BigDecimal totalAmount,
            boolean freeStay, String notes) {
    }

    public PricingMode defaultMode() {
        return properties.getMode();
    }

    public Quote quote(long durationMinutes, BigDecimal ratePerHour, PricingMode mode) {
        if (durationMinutes < 0) {
            throw new IllegalArgumentException("Duration cannot be negative: " + durationMinutes);
        }
        int grace = properties.getGracePeriodMinutes();
        if (grace > 0 && durationMinutes <= grace) {
            BigDecimal zero = money(BigDecimal.ZERO);
            return new Quote(BigDecimal.ZERO, zero, zero, zero, true,
                    "Free: stay within the " + grace + "-minute grace period");
        }

        long hours = billableHours(durationMinutes);
        BigDecimal base = mode == PricingMode.SLAB
                ? slabAmount(hours, ratePerHour)
                : ratePerHour.multiply(BigDecimal.valueOf(hours));
        String notes = mode == PricingMode.SLAB ? "Slab-based pricing applied" : null;

        int capHours = properties.getDailyCapHours();
        if (capHours > 0) {
            long days = (hours + HOURS_PER_DAY - 1) / HOURS_PER_DAY;
            BigDecimal cap = ratePerHour.multiply(BigDecimal.valueOf(capHours * days));
            if (base.compareTo(cap) > 0) {
                base = cap;
                String capNote = "Daily cap of " + capHours + " hours applied";
                notes = notes == null ? capNote : notes + "; " + capNote;
            }
        }

        BigDecimal baseAmount = money(base);
        BigDecimal taxAmount = money(baseAmount.multiply(properties.getTaxRate()));
        return new Quote(BigDecimal.valueOf(hours), baseAmount, taxAmount, baseAmount.add(taxAmount), false, notes);
    }

    /** Every started hour counts, with a minimum of one hour. */
    static long billableHours(long durationMinutes) {
        return Math.max(1, (durationMinutes + 59) / 60);
    }

    /** Charges each hour at the factor of the band it falls in, counted from the start of the stay. */
    BigDecimal slabAmount(long hours, BigDecimal ratePerHour) {
        BigDecimal amount = BigDecimal.ZERO;
        long charged = 0;
        for (PricingProperties.Slab slab : properties.getSlabs()) {
            long bandEnd = slab.getUpToHours() == null ? Long.MAX_VALUE : slab.getUpToHours();
            long inBand = Math.min(hours, bandEnd) - charged;
            if (inBand > 0) {
                amount = amount.add(ratePerHour.multiply(slab.getFactor()).multiply(BigDecimal.valueOf(inBand)));
                charged += inBand;
            }
            if (charged >= hours) {
                break;
            }
        }
        return amount;
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    /** Fails startup on settings that would produce wrong prices, instead of billing customers wrongly. */
    static void validate(PricingProperties p) {
        if (p.getMode() == null) {
            throw new IllegalStateException("app.pricing.mode must be HOURLY or SLAB");
        }
        if (p.getTaxRate() == null || p.getTaxRate().signum() < 0 || p.getTaxRate().compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalStateException("app.pricing.tax-rate must be between 0 and 1, was " + p.getTaxRate());
        }
        if (p.getGracePeriodMinutes() < 0) {
            throw new IllegalStateException("app.pricing.grace-period-minutes cannot be negative");
        }
        if (p.getDailyCapHours() < 0 || p.getDailyCapHours() > HOURS_PER_DAY) {
            throw new IllegalStateException("app.pricing.daily-cap-hours must be between 0 (off) and 24");
        }
        List<PricingProperties.Slab> slabs = p.getSlabs();
        if (slabs == null || slabs.isEmpty()) {
            throw new IllegalStateException("app.pricing.slabs needs at least one band");
        }
        int previous = 0;
        for (int i = 0; i < slabs.size(); i++) {
            PricingProperties.Slab slab = slabs.get(i);
            boolean last = i == slabs.size() - 1;
            BigDecimal factor = slab.getFactor();
            if (factor == null || factor.signum() <= 0 || factor.compareTo(BigDecimal.ONE) > 0) {
                throw new IllegalStateException("app.pricing.slabs[" + i + "].factor must be in (0, 1]");
            }
            if (last && slab.getUpToHours() != null) {
                throw new IllegalStateException("The last app.pricing.slabs band must be open-ended (no up-to-hours)");
            }
            if (!last && (slab.getUpToHours() == null || slab.getUpToHours() <= previous)) {
                throw new IllegalStateException("app.pricing.slabs[" + i + "].up-to-hours must be set and increasing");
            }
            if (!last) {
                previous = slab.getUpToHours();
            }
        }
    }
}
