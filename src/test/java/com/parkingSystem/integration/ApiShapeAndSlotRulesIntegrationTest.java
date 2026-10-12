package com.parkingSystem.integration;

import com.jayway.jsonpath.JsonPath;
import com.parkingSystem.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Step 6 (paginated lists, response DTOs, revenue) and step 7 (which vehicle fits which slot) over HTTP.
 */
class ApiShapeAndSlotRulesIntegrationTest extends IntegrationTestSupport {

    // ---------------------------------------------------------------- pagination

    @Test
    void listsArePagedInAStableOrderWithoutOverlapOrGaps() throws Exception {
        String admin = createAdmin();
        String location = uniqueName();
        List<Long> created = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            created.add(createSpace(admin, location, "NORMAL", i % 2));
        }

        Set<Long> seen = new HashSet<>();
        for (int page = 0; page < 3; page++) {
            MvcResult result = mockMvc.perform(get("/api/slots/available-by-location").param("location", location)
                            .param("page", String.valueOf(page)).param("size", "2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.page").value(page))
                    .andExpect(jsonPath("$.size").value(2))
                    .andExpect(jsonPath("$.totalElements").value(5))
                    .andExpect(jsonPath("$.totalPages").value(3))
                    .andExpect(jsonPath("$.last").value(page == 2))
                    .andReturn();
            List<Number> ids = JsonPath.read(body(result), "$.content[*].parkingSpaceId");
            List<Integer> floors = JsonPath.read(body(result), "$.content[*].floorNumber");
            assertThat(floors).isSorted();
            ids.forEach(id -> assertThat(seen.add(id.longValue())).as("no space on two pages").isTrue());
        }
        assertThat(seen).containsExactlyInAnyOrderElementsOf(created);
    }

    @Test
    void pageParametersAreValidated() throws Exception {
        mockMvc.perform(get("/api/slots/available-all").param("size", "0")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/slots/available-all").param("size", "101")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/slots/available-all").param("page", "-1")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/slots/available-all").param("page", "x")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/slots/available-all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(20));
    }

    @Test
    void userListsArePagedToo() throws Exception {
        Session alice = registerAndLogin();
        registerVehicle(alice);
        registerVehicle(alice);
        registerVehicle(alice);

        mockMvc.perform(get("/api/vehicles/user/" + alice.userId()).param("size", "2")
                        .header("Authorization", alice.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content[0].vehicleId").isNumber());
    }

    // ---------------------------------------------------------------- DTOs and revenue

    @Test
    void spacesAndUsersAreReturnedAsDtos() throws Exception {
        String admin = createAdmin();
        Session alice = registerAndLogin();
        Long spaceId = createSpace(admin);

        mockMvc.perform(get("/api/parkingspace/details/id").param("parkingSpaceId", spaceId.toString())
                        .header("Authorization", admin))
                .andExpect(jsonPath("$.parkingSpaceId").value(spaceId))
                .andExpect(jsonPath("$.slotType").value("NORMAL"))
                .andExpect(jsonPath("$.availabilityStatus").value("AVAILABLE"))
                .andExpect(jsonPath("$.ratePerHour").value(10.0))
                .andExpect(jsonPath("$.isActive").value(true));

        mockMvc.perform(get("/api/users/details").param("userId", alice.userId().toString())
                        .header("Authorization", alice.bearer()))
                .andExpect(jsonPath("$.userId").value(alice.userId()))
                .andExpect(jsonPath("$.licensePlate").isString())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.userPassword").doesNotExist());
    }

    @Test
    void revenueIsTheSumOfPaidBills() throws Exception {
        String admin = createAdmin();
        BigDecimal before = revenue(admin);

        Session alice = registerAndLogin();
        Long vehicleId = registerVehicle(alice);
        Long spaceId = createSpace(admin);
        String reference = JsonPath.read(body(enter(alice, vehicleId, spaceId).andReturn()), "$.ticket.ticketReference");
        Number billId = JsonPath.read(body(mockMvc.perform(post("/api/parking/exit").param("ticketReference", reference)
                .header("Authorization", alice.bearer())).andReturn()), "$.bill.billId");
        assertThat(revenue(admin)).as("pending bills are not revenue").isEqualByComparingTo(before);

        mockMvc.perform(put("/api/billing/" + billId + "/pay").param("paymentMethod", "CASH")
                .header("Authorization", admin)).andExpect(status().isOk());

        assertThat(revenue(admin)).isEqualByComparingTo(before.add(new BigDecimal("11.00")));
    }

    // ---------------------------------------------------------------- vehicle type vs slot type

    @Test
    void vehiclesOnlyParkInSlotsTheyFit() throws Exception {
        String admin = createAdmin();
        Session rider = registerAndLogin();
        Long bike = registerVehicle(rider, "BIKE");
        Long normal = createSpace(admin);
        Long bikeBay = createSpace(admin, "North", "BIKE", 0);
        Long charger = createSpace(admin, "North", "EV_CHARGING", 0);

        enter(rider, bike, normal)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("A BIKE cannot park in a NORMAL space"));
        enter(rider, bike, charger).andExpect(status().isBadRequest());
        enter(rider, bike, bikeBay).andExpect(status().isCreated());
    }

    @Test
    void reservingAChargingBayNeedsAnEv() throws Exception {
        String admin = createAdmin();
        Session alice = driver(); // owns a car only
        Long charger = createSpace(admin, "North", "EV_CHARGING", 0);

        reserve(alice, charger)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("no registered vehicle that can park in a EV_CHARGING space")));
        assertThat(spaceStatus(admin, charger)).isEqualTo("AVAILABLE");

        registerVehicle(alice, "EV");
        reserve(alice, charger).andExpect(status().isCreated());
    }

    @Test
    void recommendationDependsOnTheVehicleType() throws Exception {
        String admin = createAdmin();
        String location = uniqueName();
        Long normal = createSpace(admin, location, "NORMAL", 0);
        Long charger = createSpace(admin, location, "EV_CHARGING", 1);

        mockMvc.perform(get("/api/slots/recommend").param("location", location).param("vehicleType", "ev"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parkingSpaceId").value(charger));
        mockMvc.perform(get("/api/slots/recommend").param("location", location).param("vehicleType", "CAR"))
                .andExpect(jsonPath("$.parkingSpaceId").value(normal));
        mockMvc.perform(get("/api/slots/recommend").param("location", location).param("vehicleType", "BIKE"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/slots/recommend").param("location", location).param("vehicleType", "TRUCK"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void availabilitySummaryCoversEverySlotType() throws Exception {
        mockMvc.perform(get("/api/slots/availability-summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.totalAvailable").isNumber())
                .andExpect(jsonPath("$.summary.availableBySlotType.NORMAL").isNumber())
                .andExpect(jsonPath("$.summary.availableBySlotType.EV_CHARGING").isNumber())
                .andExpect(jsonPath("$.summary.availableBySlotType.BIKE").isNumber());
    }

    // ---------------------------------------------------------------- helpers

    private BigDecimal revenue(String admin) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/billing/revenue-total").header("Authorization", admin))
                .andExpect(status().isOk()).andReturn();
        Object value = JsonPath.read(body(result), "$.totalRevenue");
        return new BigDecimal(value.toString());
    }
}
