package com.parkingSystem.integration;

import com.parkingSystem.scheduling.ReservationExpiryJob;
import com.parkingSystem.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Reservation life cycle over HTTP: reserve, drive in, cancel, move, expire, and how reservations interact
 * with vehicle entry, feedback and admin changes to the space. The default hold is 30 minutes.
 */
class ReservationLifecycleIntegrationTest extends IntegrationTestSupport {

    private static final Duration PAST_THE_HOLD = Duration.ofMinutes(31);

    @Autowired
    private ReservationExpiryJob expiryJob;

    // ---------------------------------------------------------------- the original bug

    @Test
    void ownerCanDriveIntoTheSpaceTheyReservedAndTheReservationIsFulfilled() throws Exception {
        String admin = createAdmin();
        Session alice = registerAndLogin();
        Long vehicleId = registerVehicle(alice);
        Long spaceId = createSpace(admin);

        MvcResult reserved = reserve(alice, spaceId)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reservation.status").value("ACTIVE"))
                .andExpect(jsonPath("$.reservation.expiresAt").exists())
                .andReturn();
        Long reservationId = longAt(reserved, "$.reservation.reservationId");
        assertThat(spaceStatus(admin, spaceId)).isEqualTo("RESERVED");

        // Before the fix this was a 409: the reservation had marked the space "occupied"
        MvcResult entry = enter(alice, vehicleId, spaceId).andExpect(status().isCreated()).andReturn();
        Long ticketId = longAt(entry, "$.ticket.ticketId");

        reservationDetails(alice, reservationId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FULFILLED"))
                .andExpect(jsonPath("$.fulfilledAt").exists())
                .andExpect(jsonPath("$.ticketId").value(ticketId));
        assertThat(spaceStatus(admin, spaceId)).isEqualTo("OCCUPIED");
    }

    @Test
    void anotherDriverCannotTakeAReservedSpaceAndItIsNotListedAsFree() throws Exception {
        String admin = createAdmin();
        Session alice = registerAndLogin();
        Session bob = registerAndLogin();
        Long bobsCar = registerVehicle(bob);
        Long spaceId = createSpace(admin);
        reserve(alice, spaceId).andExpect(status().isCreated());

        enter(bob, bobsCar, spaceId)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Parking space is reserved for another driver"));
        reserve(bob, spaceId).andExpect(status().isConflict());

        mockMvc.perform(get("/api/slots/check-availability/" + spaceId))
                .andExpect(jsonPath("$.isAvailable").value(false));
        mockMvc.perform(get("/api/parkingspace/available"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.parkingSpaceId == " + spaceId + ")]").isEmpty());
        mockMvc.perform(get("/api/parkingspace/available").param("availabilityStatus", "reserved"))
                .andExpect(jsonPath("$[?(@.parkingSpaceId == " + spaceId + ")]").isNotEmpty());
    }

    // ---------------------------------------------------------------- expiry

    @Test
    void noShowIsExpiredByTheJobAndTheSpaceIsFreed() throws Exception {
        String admin = createAdmin();
        Session alice = registerAndLogin();
        Long spaceId = createSpace(admin);
        Long reservationId = longAt(reserve(alice, spaceId).andReturn(), "$.reservation.reservationId");

        assertThat(expiryJob.expireOverdueReservations()).as("nothing is overdue yet").isZero();
        reservationDetails(alice, reservationId).andExpect(jsonPath("$.status").value("ACTIVE"));

        clock.advance(PAST_THE_HOLD);
        assertThat(expiryJob.expireOverdueReservations()).isGreaterThanOrEqualTo(1);

        reservationDetails(alice, reservationId).andExpect(jsonPath("$.status").value("EXPIRED"));
        assertThat(spaceStatus(admin, spaceId)).isEqualTo("AVAILABLE");
        assertThat(expiryJob.expireOverdueReservations()).as("running again changes nothing").isZero();
    }

    @Test
    void holdThatRanOutDoesNotBlockOtherDriversEvenBeforeTheJobRuns() throws Exception {
        String admin = createAdmin();
        Session alice = registerAndLogin();
        Session bob = registerAndLogin();
        Long bobsCar = registerVehicle(bob);
        Long spaceId = createSpace(admin);
        Long reservationId = longAt(reserve(alice, spaceId).andReturn(), "$.reservation.reservationId");

        clock.advance(PAST_THE_HOLD);

        enter(bob, bobsCar, spaceId).andExpect(status().isCreated());
        reservationDetails(alice, reservationId).andExpect(jsonPath("$.status").value("EXPIRED"));
    }

    @Test
    void ownerArrivingAfterTheHoldStillGetsInIfTheSpaceIsFree() throws Exception {
        String admin = createAdmin();
        Session alice = registerAndLogin();
        Long vehicleId = registerVehicle(alice);
        Long spaceId = createSpace(admin);
        Long reservationId = longAt(reserve(alice, spaceId).andReturn(), "$.reservation.reservationId");

        clock.advance(PAST_THE_HOLD);

        enter(alice, vehicleId, spaceId).andExpect(status().isCreated());
        reservationDetails(alice, reservationId).andExpect(jsonPath("$.status").value("EXPIRED"));
    }

    // ---------------------------------------------------------------- one active reservation per user

    @Test
    void aUserHoldsAtMostOneSpaceButAnExpiredHoldDoesNotBlockTheNextOne() throws Exception {
        String admin = createAdmin();
        Session alice = registerAndLogin();
        Long first = createSpace(admin);
        Long second = createSpace(admin);
        Long firstReservation = longAt(reserve(alice, first).andReturn(), "$.reservation.reservationId");

        reserve(alice, second)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("already have an active reservation")));

        clock.advance(PAST_THE_HOLD); // no expiry job run: the create call itself handles the stale hold
        reserve(alice, second).andExpect(status().isCreated());

        reservationDetails(alice, firstReservation).andExpect(jsonPath("$.status").value("EXPIRED"));
        assertThat(spaceStatus(admin, first)).isEqualTo("AVAILABLE");
        assertThat(spaceStatus(admin, second)).isEqualTo("RESERVED");
    }

    // ---------------------------------------------------------------- cancel and move

    @Test
    void cancellingReleasesTheSpaceAndKeepsTheRecord() throws Exception {
        String admin = createAdmin();
        Session alice = registerAndLogin();
        Session bob = registerAndLogin();
        Long spaceId = createSpace(admin);
        Long reservationId = longAt(reserve(alice, spaceId).andReturn(), "$.reservation.reservationId");

        mockMvc.perform(delete("/api/reservation/remove").param("reservationId", reservationId.toString())
                        .header("Authorization", bob.bearer()))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/reservation/remove").param("reservationId", reservationId.toString())
                        .header("Authorization", alice.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservation.status").value("CANCELLED"))
                .andExpect(jsonPath("$.reservation.cancelledAt").exists());

        assertThat(spaceStatus(admin, spaceId)).isEqualTo("AVAILABLE");
        reservationDetails(alice, reservationId).andExpect(status().isOk());
        mockMvc.perform(delete("/api/reservation/remove").param("reservationId", reservationId.toString())
                        .header("Authorization", alice.bearer()))
                .andExpect(status().isConflict());
    }

    @Test
    void movingAReservationSwapsTheSpacesButDoesNotExtendTheHold() throws Exception {
        String admin = createAdmin();
        Session alice = registerAndLogin();
        Session bob = registerAndLogin();
        Long bobsCar = registerVehicle(bob);
        Long first = createSpace(admin);
        Long second = createSpace(admin);
        Long taken = createSpace(admin);
        enter(bob, bobsCar, taken).andExpect(status().isCreated());

        MvcResult created = reserve(alice, first).andReturn();
        Long reservationId = longAt(created, "$.reservation.reservationId");
        String expiresAt = com.jayway.jsonpath.JsonPath.read(body(created), "$.reservation.expiresAt");

        update(alice, reservationId, second, 3)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parkingSpaceId").value(second))
                .andExpect(jsonPath("$.duration").value(3))
                .andExpect(jsonPath("$.expiresAt").value(expiresAt))
                .andExpect(jsonPath("$.updatedAt").exists());
        assertThat(spaceStatus(admin, first)).isEqualTo("AVAILABLE");
        assertThat(spaceStatus(admin, second)).isEqualTo("RESERVED");

        update(alice, reservationId, taken, null).andExpect(status().isConflict());
        assertThat(spaceStatus(admin, second)).as("failed move leaves the hold in place").isEqualTo("RESERVED");

        clock.advance(PAST_THE_HOLD);
        update(alice, reservationId, first, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("expired")));
    }

    @Test
    void durationIsLimitedToOneDay() throws Exception {
        String admin = createAdmin();
        Session alice = registerAndLogin();
        Long spaceId = createSpace(admin);

        mockMvc.perform(post("/api/reservation/register").header("Authorization", alice.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"parkingSpaceId\":" + spaceId + ",\"duration\":25}"))
                .andExpect(status().isBadRequest());
        assertThat(spaceStatus(admin, spaceId)).isEqualTo("AVAILABLE");
    }

    // ---------------------------------------------------------------- feedback

    @Test
    void feedbackIsOnlyAcceptedOnceForAReservationThatWasUsed() throws Exception {
        String admin = createAdmin();
        Session alice = registerAndLogin();
        Long vehicleId = registerVehicle(alice);
        Long spaceId = createSpace(admin);
        Long reservationId = longAt(reserve(alice, spaceId).andReturn(), "$.reservation.reservationId");

        feedback(alice, reservationId).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("ACTIVE")));

        enter(alice, vehicleId, spaceId).andExpect(status().isCreated());
        feedback(alice, reservationId).andExpect(status().isCreated());
        feedback(alice, reservationId).andExpect(status().isConflict());
    }

    @Test
    void overlongFeedbackCommentIsA400NotADatabaseError() throws Exception {
        String admin = createAdmin();
        Session alice = registerAndLogin();
        Long vehicleId = registerVehicle(alice);
        Long spaceId = createSpace(admin);
        Long reservationId = longAt(reserve(alice, spaceId).andReturn(), "$.reservation.reservationId");
        enter(alice, vehicleId, spaceId).andExpect(status().isCreated());

        mockMvc.perform(post("/api/feedback/register").header("Authorization", alice.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reservationId\":" + reservationId + ",\"rating\":4,\"comments\":\""
                                + "x".repeat(256) + "\"}"))
                .andExpect(status().isBadRequest());
    }

    // ---------------------------------------------------------------- admin and the space status

    @Test
    void adminCanOnlyToggleAvailableAndOutOfServiceAndNotWhileTheSpaceIsInUse() throws Exception {
        String admin = createAdmin();
        Session alice = registerAndLogin();
        Long vehicleId = registerVehicle(alice);
        Long spaceId = createSpace(admin);

        setStatus(admin, spaceId, "OCCUPIED").andExpect(status().isBadRequest());
        setStatus(admin, spaceId, "BROKEN").andExpect(status().isBadRequest());

        Long reservationId = longAt(reserve(alice, spaceId).andReturn(), "$.reservation.reservationId");
        setStatus(admin, spaceId, "OUT_OF_SERVICE").andExpect(status().isConflict());
        mockMvc.perform(delete("/api/reservation/remove").param("reservationId", reservationId.toString())
                .header("Authorization", alice.bearer())).andExpect(status().isOk());

        setStatus(admin, spaceId, "out_of_service")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availabilityStatus").value("OUT_OF_SERVICE"));
        enter(alice, vehicleId, spaceId).andExpect(status().isConflict());
        reserve(alice, spaceId).andExpect(status().isConflict());

        setStatus(admin, spaceId, "AVAILABLE").andExpect(status().isOk());
        enter(alice, vehicleId, spaceId).andExpect(status().isCreated());
        setStatus(admin, spaceId, "OUT_OF_SERVICE").andExpect(status().isConflict());
    }

    @Test
    void removingASpaceWithHistoryDeactivatesItAndAnUnusedSpaceIsDeleted() throws Exception {
        String admin = createAdmin();
        Session alice = registerAndLogin();
        Long vehicleId = registerVehicle(alice);
        Long used = createSpace(admin);
        Long unused = createSpace(admin);

        Long reservationId = longAt(reserve(alice, used).andReturn(), "$.reservation.reservationId");
        removeSpace(admin, used).andExpect(status().isConflict());
        mockMvc.perform(delete("/api/reservation/remove").param("reservationId", reservationId.toString())
                .header("Authorization", alice.bearer())).andExpect(status().isOk());

        removeSpace(admin, used).andExpect(status().isOk());
        assertThat(spaceStatus(admin, used)).isEqualTo("OUT_OF_SERVICE");
        enter(alice, vehicleId, used).andExpect(status().isBadRequest());
        reservationDetails(alice, reservationId).andExpect(status().isOk());

        removeSpace(admin, unused).andExpect(status().isOk());
        mockMvc.perform(get("/api/parkingspace/details/id").param("parkingSpaceId", unused.toString())
                .header("Authorization", admin)).andExpect(status().isNotFound());
    }

    @Test
    void unknownStatusFilterIsA400() throws Exception {
        mockMvc.perform(get("/api/parkingspace/available").param("availabilityStatus", "bogus"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void reservationExpiryUsesTheConfiguredHold() throws Exception {
        String admin = createAdmin();
        Session alice = registerAndLogin();
        Long spaceId = createSpace(admin);
        LocalDateTime before = LocalDateTime.now(clock).withNano(0);

        String expiresAt = com.jayway.jsonpath.JsonPath.read(body(reserve(alice, spaceId).andReturn()),
                "$.reservation.expiresAt");

        assertThat(LocalDateTime.parse(expiresAt))
                .isAfterOrEqualTo(before.plusMinutes(30))
                .isBefore(before.plusMinutes(31));
    }

    // ---------------------------------------------------------------- helpers

    private org.springframework.test.web.servlet.ResultActions update(Session session, Long reservationId,
            Long spaceId, Integer duration) throws Exception {
        String json = "{\"reservationId\":" + reservationId + ",\"parkingSpaceId\":" + spaceId
                + (duration == null ? "" : ",\"duration\":" + duration) + "}";
        return mockMvc.perform(put("/api/reservation/update").header("Authorization", session.bearer())
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private org.springframework.test.web.servlet.ResultActions feedback(Session session, Long reservationId)
            throws Exception {
        return mockMvc.perform(post("/api/feedback/register").header("Authorization", session.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reservationId\":" + reservationId + ",\"rating\":5,\"comments\":\"Smooth\"}"));
    }

    private org.springframework.test.web.servlet.ResultActions setStatus(String adminBearer, Long spaceId,
            String status) throws Exception {
        return mockMvc.perform(put("/api/parkingspace/update").header("Authorization", adminBearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"parkingSpaceId\":" + spaceId + ",\"status\":\"" + status + "\"}"));
    }

    private org.springframework.test.web.servlet.ResultActions removeSpace(String adminBearer, Long spaceId)
            throws Exception {
        return mockMvc.perform(delete("/api/parkingspace/remove").param("parkingSpaceId", spaceId.toString())
                .header("Authorization", adminBearer));
    }
}
