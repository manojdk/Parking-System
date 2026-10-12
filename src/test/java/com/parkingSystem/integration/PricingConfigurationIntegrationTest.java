package com.parkingSystem.integration;

import com.jayway.jsonpath.JsonPath;
import com.parkingSystem.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pricing settings take effect on the bill created at exit, without code changes.
 */
@TestPropertySource(properties = {
        "app.pricing.grace-period-minutes=5",
        "app.pricing.tax-rate=0.18",
        "app.pricing.daily-cap-hours=8"
})
class PricingConfigurationIntegrationTest extends IntegrationTestSupport {

    @Test
    void shortStayWithinTheGracePeriodIsFreeAndAlreadySettled() throws Exception {
        String reference = park();

        exit(reference)
                .andExpect(jsonPath("$.bill.totalAmount").value(0.0))
                .andExpect(jsonPath("$.bill.status").value("PAID"))
                .andExpect(jsonPath("$.bill.paymentMethod").value("NO_CHARGE"));
    }

    @Test
    void configuredTaxAndDailyCapApplyToLongStays() throws Exception {
        String reference = park();
        clock.advance(Duration.ofHours(20)); // 20 h at 10.00, capped at 8 h per day = 80.00, + 18% tax

        exit(reference)
                .andExpect(jsonPath("$.bill.durationHours").value(20))
                .andExpect(jsonPath("$.bill.baseAmount").value(80.0))
                .andExpect(jsonPath("$.bill.taxAmount").value(14.4))
                .andExpect(jsonPath("$.bill.totalAmount").value(94.4))
                .andExpect(jsonPath("$.bill.status").value("PENDING"));
    }

    private String park() throws Exception {
        String admin = createAdmin();
        Session alice = registerAndLogin();
        Long vehicleId = registerVehicle(alice);
        Long spaceId = createSpace(admin);
        this.owner = alice;
        return JsonPath.read(body(enter(alice, vehicleId, spaceId).andExpect(status().isCreated()).andReturn()),
                "$.ticket.ticketReference");
    }

    private Session owner;

    private org.springframework.test.web.servlet.ResultActions exit(String reference) throws Exception {
        return mockMvc.perform(post("/api/parking/exit").param("ticketReference", reference)
                        .header("Authorization", owner.bearer()))
                .andExpect(status().isOk());
    }
}
