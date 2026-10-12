package com.parkingSystem.controller;

import com.jayway.jsonpath.JsonPath;
import com.parkingSystem.config.JwtUtil;
import com.parkingSystem.model.User;
import com.parkingSystem.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end HTTP tests against the full application context (H2 database, real security chain).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiSecurityIntegrationTest {

    private static final AtomicInteger COUNTER = new AtomicInteger();
    private static final String PASSWORD = "password123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    // ---------------------------------------------------------------- register / login

    @Test
    void registerThenLoginReturnsToken() throws Exception {
        String name = uniqueName();
        register(name).andExpect(status().isCreated()).andExpect(jsonPath("$.userId").isNumber());

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(name, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.userId").isNumber());
    }

    @Test
    void loginWithWrongPasswordReturns401() throws Exception {
        String name = uniqueName();
        register(name).andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(name, "wrong-password")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.path").value("/api/auth/login"));
    }

    @Test
    void duplicateRegistrationReturns409() throws Exception {
        String name = uniqueName();
        register(name).andExpect(status().isCreated());
        register(name).andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("Conflict"));
    }

    @Test
    void invalidRegistrationReturns400WithConsistentBody() throws Exception {
        mockMvc.perform(post("/api/users/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userName\":\"\",\"userEmail\":\"not-an-email\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.path").value("/api/users/register"));
    }

    // ---------------------------------------------------------------- 401

    @Test
    void protectedEndpointWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/vehicles/user/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Authentication required"));
    }

    @Test
    void malformedTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/vehicles/user/1").header("Authorization", "Bearer not.a.jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid token"));
    }

    @Test
    void expiredTokenReturns401() throws Exception {
        JwtUtil expiredIssuer = new JwtUtil("test-only-jwt-secret-key-0123456789abcdef", -60_000);
        String expired = expiredIssuer.generateToken("someone", "USER");

        mockMvc.perform(get("/api/vehicles/user/1").header("Authorization", "Bearer " + expired))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Token has expired"));
    }

    // ---------------------------------------------------------------- 403 / ownership

    @Test
    void userCannotReadAnotherUsersDetails() throws Exception {
        Session alice = registerAndLogin();
        Session bob = registerAndLogin();

        mockMvc.perform(get("/api/users/details").param("userId", bob.userId.toString()).header("Authorization", alice.bearer()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        mockMvc.perform(get("/api/users/details").param("userId", alice.userId.toString()).header("Authorization", alice.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(alice.userId))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void userCannotReadAnotherUsersVehicle() throws Exception {
        Session alice = registerAndLogin();
        Session bob = registerAndLogin();
        Long vehicleId = registerVehicle(bob);

        mockMvc.perform(get("/api/vehicles/" + vehicleId).header("Authorization", alice.bearer()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/vehicles/" + vehicleId).header("Authorization", bob.bearer()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/billing/user/" + bob.userId).header("Authorization", alice.bearer()))
                .andExpect(status().isForbidden());
    }

    @Test
    void userCannotChangeAnotherUsersPassword() throws Exception {
        Session alice = registerAndLogin();
        Session bob = registerAndLogin();

        mockMvc.perform(put("/api/users/update").header("Authorization", alice.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":" + bob.userId + ",\"password\":\"hijacked-password\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void userCannotEscalateOwnRole() throws Exception {
        Session alice = registerAndLogin();

        mockMvc.perform(put("/api/users/update").header("Authorization", alice.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":" + alice.userId + ",\"role\":\"ADMIN\",\"licensePlate\":\"NEW-1\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/users/details").param("userId", alice.userId.toString()).header("Authorization", alice.bearer()))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void adminOnlyEndpointsRejectUsersAndAllowAdmins() throws Exception {
        Session alice = registerAndLogin();
        String adminBearer = createAdmin();

        mockMvc.perform(get("/api/billing/pending-all").header("Authorization", alice.bearer()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/billing/pending-all").header("Authorization", adminBearer))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/users/details").param("userId", alice.userId.toString()).header("Authorization", adminBearer))
                .andExpect(status().isOk());
    }

    // ---------------------------------------------------------------- public endpoints

    @Test
    void availabilityEndpointsArePublic() throws Exception {
        mockMvc.perform(get("/api/slots/availability-summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary").exists());
        mockMvc.perform(get("/api/parking/occupancy")).andExpect(status().isOk());
        mockMvc.perform(get("/api/parkingspace/available").param("availabilityStatus", "available"))
                .andExpect(status().isOk());
    }

    @Test
    void apiDocsArePublic() throws Exception {
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
    }

    // ---------------------------------------------------------------- full parking flow

    @Test
    void entryAndExitGeneratesABill() throws Exception {
        String adminBearer = createAdmin();
        Session alice = registerAndLogin();
        Long vehicleId = registerVehicle(alice);

        MvcResult spaceResult = mockMvc.perform(post("/api/parkingspace/register").header("Authorization", adminBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"spaceNumber\":\"" + uniqueName() + "\",\"location\":\"North\",\"floorNumber\":0,"
                                + "\"slotType\":\"NORMAL\",\"ratePerHour\":10.00}"))
                .andExpect(status().isCreated())
                .andReturn();
        Number spaceId = JsonPath.read(spaceResult.getResponse().getContentAsString(), "$.parkingSpaceId");

        // a normal user cannot create parking spaces
        mockMvc.perform(post("/api/parkingspace/register").header("Authorization", alice.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"spaceNumber\":\"X\",\"location\":\"North\",\"slotType\":\"NORMAL\",\"ratePerHour\":1}"))
                .andExpect(status().isForbidden());

        MvcResult entry = mockMvc.perform(post("/api/parking/entry").header("Authorization", alice.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"vehicleId\":" + vehicleId + ",\"parkingSpaceId\":" + spaceId + "}"))
                .andExpect(status().isCreated())
                .andReturn();
        String reference = JsonPath.read(entry.getResponse().getContentAsString(), "$.ticket.ticketReference");

        // the space is now taken
        mockMvc.perform(post("/api/parking/entry").header("Authorization", alice.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"vehicleId\":" + vehicleId + ",\"parkingSpaceId\":" + spaceId + "}"))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/parking/exit").param("ticketReference", reference).header("Authorization", alice.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticket.status").value("CLOSED"))
                .andExpect(jsonPath("$.bill.invoiceNumber", startsWith("INV-")))
                .andExpect(jsonPath("$.bill.durationHours").value(1))
                .andExpect(jsonPath("$.bill.totalAmount").value(11.0));

        mockMvc.perform(get("/api/billing/user/" + alice.userId + "/pending").header("Authorization", alice.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        // the hourly bill already exists, so a second (slab) bill for the same ticket is rejected
        Number ticketId = JsonPath.read(entry.getResponse().getContentAsString(), "$.ticket.ticketId");
        mockMvc.perform(post("/api/billing/generate-slab/" + ticketId).header("Authorization", adminBearer))
                .andExpect(status().isConflict());
    }

    @Test
    void bulkParkingSpaceCreationValidatesEachElement() throws Exception {
        String adminBearer = createAdmin();

        mockMvc.perform(post("/api/parkingspace/register/multiple").header("Authorization", adminBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"spaceNumber\":\"\",\"location\":\"North\",\"slotType\":\"NORMAL\",\"ratePerHour\":1}]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    // ---------------------------------------------------------------- helpers

    private record Session(Long userId, String token) {
        String bearer() {
            return "Bearer " + token;
        }
    }

    private static String uniqueName() {
        return "user" + COUNTER.incrementAndGet() + "_" + System.nanoTime();
    }

    private org.springframework.test.web.servlet.ResultActions register(String name) throws Exception {
        String body = "{\"userName\":\"" + name + "\",\"userEmail\":\"" + name + "@example.com\","
                + "\"password\":\"" + PASSWORD + "\",\"licencePlate\":\"LP-" + name + "\"}";
        return mockMvc.perform(post("/api/users/register").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private static String loginJson(String username, String password) {
        return "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
    }

    private Session registerAndLogin() throws Exception {
        String name = uniqueName();
        register(name).andExpect(status().isCreated());
        MvcResult result = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(name, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();
        String json = result.getResponse().getContentAsString();
        Number userId = JsonPath.read(json, "$.userId");
        return new Session(userId.longValue(), JsonPath.read(json, "$.token"));
    }

    private Long registerVehicle(Session owner) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/vehicles/register").header("Authorization", owner.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"licensePlate\":\"" + uniqueName() + "\",\"vehicleType\":\"CAR\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        Number vehicleId = JsonPath.read(result.getResponse().getContentAsString(), "$.vehicle.vehicleId");
        return vehicleId.longValue();
    }

    private String createAdmin() {
        String name = uniqueName();
        User admin = new User();
        admin.setUserName(name);
        admin.setUserEmail(name + "@example.com");
        admin.setPassword(passwordEncoder.encode(PASSWORD));
        admin.setLicencePlate("N/A");
        admin.setRole("ADMIN");
        admin.setCreatedDate(LocalDateTime.now());
        userRepository.save(admin);
        return "Bearer " + jwtUtil.generateToken(name, "ADMIN");
    }
}
