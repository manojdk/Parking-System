package com.parkingSystem.pricing;

import com.parkingSystem.pricing.PricingCalculator.Quote;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PricingCalculatorTest {

    private static final BigDecimal RATE = new BigDecimal("10.00");

    private static PricingCalculator calculator(Consumer<PricingProperties> settings) {
        PricingProperties properties = new PricingProperties();
        settings.accept(properties);
        return new PricingCalculator(properties);
    }

    private static final PricingCalculator DEFAULTS = calculator(p -> { });

    // ---------------------------------------------------------------- defaults = the original prices

    @ParameterizedTest
    @CsvSource({ "0,1", "1,1", "59,1", "60,1", "61,2", "180,3", "-5,1" })
    void billableHoursHasAMinimumOfOneAndRoundsUp(long minutes, long expectedHours) {
        assertThat(PricingCalculator.billableHours(minutes)).isEqualTo(expectedHours);
    }

    @Test
    void hourlyByDefaultWithTenPercentTax() {
        Quote quote = DEFAULTS.quote(150, RATE, PricingMode.HOURLY); // 3 started hours

        assertThat(quote.billableHours()).isEqualByComparingTo("3");
        assertThat(quote.baseAmount()).isEqualByComparingTo("30.00");
        assertThat(quote.taxAmount()).isEqualByComparingTo("3.00");
        assertThat(quote.totalAmount()).isEqualByComparingTo("33.00");
        assertThat(quote.freeStay()).isFalse();
        assertThat(quote.notes()).isNull();
        assertThat(DEFAULTS.defaultMode()).isEqualTo(PricingMode.HOURLY);
    }

    @ParameterizedTest
    @CsvSource({
            "1, 10.00",   // first hour full rate
            "3, 28.00",   // 10 + 2 * 9
            "5, 44.00",   // 10 + 18 + 2 * 8
            "10, 82.00"   // 10 + 18 + 40 + 2 * 7
    })
    void defaultSlabsMatchTheOriginalPrices(long hours, String expected) {
        assertThat(DEFAULTS.slabAmount(hours, RATE)).isEqualByComparingTo(expected);
        assertThat(DEFAULTS.quote(hours * 60, RATE, PricingMode.SLAB).baseAmount()).isEqualByComparingTo(expected);
    }

    @Test
    void zeroMinuteStayWithoutGracePeriodIsChargedOneHour() {
        assertThat(DEFAULTS.quote(0, RATE, PricingMode.HOURLY).totalAmount()).isEqualByComparingTo("11.00");
    }

    // ---------------------------------------------------------------- configurable rules

    @Test
    void taxRateIsConfigurable() {
        Quote quote = calculator(p -> p.setTaxRate(new BigDecimal("0.18"))).quote(60, RATE, PricingMode.HOURLY);

        assertThat(quote.taxAmount()).isEqualByComparingTo("1.80");
        assertThat(quote.totalAmount()).isEqualByComparingTo("11.80");
    }

    @Test
    void taxIsRoundedHalfUpToTwoDecimals() {
        Quote quote = calculator(p -> p.setTaxRate(new BigDecimal("0.05"))).quote(60, new BigDecimal("0.30"),
                PricingMode.HOURLY);

        assertThat(quote.taxAmount()).isEqualByComparingTo("0.02"); // 0.015 -> 0.02
    }

    @ParameterizedTest
    @CsvSource({ "0,true", "10,true", "11,false" })
    void staysWithinTheGracePeriodAreFree(long minutes, boolean free) {
        Quote quote = calculator(p -> p.setGracePeriodMinutes(10)).quote(minutes, RATE, PricingMode.HOURLY);

        assertThat(quote.freeStay()).isEqualTo(free);
        if (free) {
            assertThat(quote.totalAmount()).isEqualByComparingTo("0");
            assertThat(quote.billableHours()).isEqualByComparingTo("0");
        } else {
            assertThat(quote.totalAmount()).isEqualByComparingTo("11.00");
        }
    }

    @Test
    void dailyCapLimitsEachStartedDay() {
        PricingCalculator capped = calculator(p -> p.setDailyCapHours(10));

        Quote shortStay = capped.quote(5 * 60, RATE, PricingMode.HOURLY);
        Quote fullDay = capped.quote(20 * 60, RATE, PricingMode.HOURLY);
        Quote dayAndAHalf = capped.quote(36 * 60, RATE, PricingMode.HOURLY);

        assertThat(shortStay.baseAmount()).isEqualByComparingTo("50.00");
        assertThat(shortStay.notes()).isNull();
        assertThat(fullDay.baseAmount()).isEqualByComparingTo("100.00");   // 20 h capped at 10 h
        assertThat(fullDay.notes()).contains("Daily cap");
        assertThat(dayAndAHalf.baseAmount()).isEqualByComparingTo("200.00"); // 2 started days x 10 h
        assertThat(dayAndAHalf.billableHours()).isEqualByComparingTo("36");
    }

    @Test
    void dailyCapAlsoAppliesToSlabPricing() {
        Quote quote = calculator(p -> p.setDailyCapHours(5)).quote(10 * 60, RATE, PricingMode.SLAB);

        assertThat(quote.baseAmount()).isEqualByComparingTo("50.00"); // slab 82.00 capped at 5 h x 10
        assertThat(quote.notes()).contains("Slab").contains("Daily cap");
    }

    @Test
    void slabsAreConfigurable() {
        PricingCalculator custom = calculator(p -> p.setSlabs(List.of(
                new PricingProperties.Slab(2, new BigDecimal("1.00")),
                new PricingProperties.Slab(null, new BigDecimal("0.50")))));

        assertThat(custom.slabAmount(4, RATE)).isEqualByComparingTo("30.00"); // 2 x 10 + 2 x 5
    }

    // ---------------------------------------------------------------- invalid settings fail at startup

    @Test
    void invalidSettingsAreRejectedAtStartup() {
        assertThatThrownBy(() -> calculator(p -> p.setTaxRate(new BigDecimal("1.5"))))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("tax-rate");
        assertThatThrownBy(() -> calculator(p -> p.setTaxRate(new BigDecimal("-0.1"))))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> calculator(p -> p.setGracePeriodMinutes(-1)))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> calculator(p -> p.setDailyCapHours(25)))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> calculator(p -> p.setSlabs(List.of())))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> calculator(p -> p.setSlabs(List.of(
                new PricingProperties.Slab(3, BigDecimal.ONE),
                new PricingProperties.Slab(2, BigDecimal.ONE),
                new PricingProperties.Slab(null, BigDecimal.ONE)))))
                .as("bands must increase").isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> calculator(p -> p.setSlabs(List.of(new PricingProperties.Slab(5, BigDecimal.ONE)))))
                .as("last band must be open-ended").isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> calculator(p -> p.setSlabs(List.of(new PricingProperties.Slab(null, new BigDecimal("1.2"))))))
                .as("no surcharge factors").isInstanceOf(IllegalStateException.class);
    }

    @Test
    void negativeDurationIsAProgrammingError() {
        assertThatThrownBy(() -> DEFAULTS.quote(-1, RATE, PricingMode.HOURLY))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
