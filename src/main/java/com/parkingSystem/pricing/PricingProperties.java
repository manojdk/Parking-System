package com.parkingSystem.pricing;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Pricing settings ({@code app.pricing.*}). The defaults reproduce the original fixed prices: hourly billing,
 * 10% tax, no grace period, no daily cap, slabs 100% / 90% / 80% / 70%. {@link PricingCalculator} validates
 * the values at startup.
 */
@Component
@ConfigurationProperties(prefix = "app.pricing")
@Data
public class PricingProperties {

    /** Mode used for the bill created on vehicle exit. */
    private PricingMode mode = PricingMode.HOURLY;

    /** Tax as a fraction of the base amount (0.10 = 10%). */
    private BigDecimal taxRate = new BigDecimal("0.10");

    /** Stays of at most this many minutes are free; 0 turns the grace period off. */
    private int gracePeriodMinutes = 0;

    /**
     * Most hours charged per started 24 hours, so long stays are capped (e.g. 10 = at most 10 hours' rate per
     * day); 0 turns the cap off.
     */
    private int dailyCapHours = 0;

    /** SLAB mode bands, in order. The last band must be open-ended (no upToHours). */
    private List<Slab> slabs = new ArrayList<>(List.of(
            new Slab(1, new BigDecimal("1.00")),
            new Slab(3, new BigDecimal("0.90")),
            new Slab(8, new BigDecimal("0.80")),
            new Slab(null, new BigDecimal("0.70"))));

    /**
     * Hours up to {@code upToHours} (counted from the start of the stay) are charged at {@code factor} times
     * the hourly rate.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Slab {
        private Integer upToHours;
        private BigDecimal factor;
    }
}
