package com.parkingSystem.support;

import com.jayway.jsonpath.JsonPath;
import com.parkingSystem.config.JwtUtil;
import com.parkingSystem.model.User;
import com.parkingSystem.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base class for HTTP-level tests: full application on H2 (Flyway migrations, real security chain) with a
 * {@link MutableClock}, plus helpers to create users, vehicles and spaces through the API.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(IntegrationTestSupport.ClockConfig.class)
public abstract class IntegrationTestSupport {

    protected static final String PASSWORD = "password123";
    private static final AtomicInteger COUNTER = new AtomicInteger();

    @TestConfiguration
    static class ClockConfig {
        @Bean
        @Primary
        MutableClock mutableClock() {
            return new MutableClock(ZoneId.systemDefault());
        }
    }

    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected MutableClock clock;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtUtil jwtUtil;

    @AfterEach
    void resetClock() {
        clock.reset();
    }

    protected record Session(Long userId, String token) {
        public String bearer() {
            return "Bearer " + token;
        }
    }

    protected static String uniqueName() {
        return "it" + COUNTER.incrementAndGet() + "_" + System.nanoTime();
    }

    protected Session registerAndLogin() throws Exception {
        String name = uniqueName();
        String body = "{\"userName\":\"" + name + "\",\"userEmail\":\"" + name + "@example.com\","
                + "\"password\":\"" + PASSWORD + "\",\"licencePlate\":\"LP-" + name + "\"}";
        mockMvc.perform(post("/api/users/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        MvcResult result = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + name + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return new Session(longAt(result, "$.userId"), JsonPath.read(body(result), "$.token"));
    }

    protected String createAdmin() {
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

    protected Long registerVehicle(Session owner) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/vehicles/register").header("Authorization", owner.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"licensePlate\":\"" + uniqueName() + "\",\"vehicleType\":\"CAR\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return longAt(result, "$.vehicle.vehicleId");
    }

    protected Long createSpace(String adminBearer) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/parkingspace/register").header("Authorization", adminBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"spaceNumber\":\"" + uniqueName() + "\",\"location\":\"North\",\"floorNumber\":0,"
                                + "\"slotType\":\"NORMAL\",\"ratePerHour\":10.00}"))
                .andExpect(status().isCreated())
                .andReturn();
        return longAt(result, "$.parkingSpaceId");
    }

    protected ResultActions reserve(Session session, Long spaceId) throws Exception {
        return mockMvc.perform(post("/api/reservation/register").header("Authorization", session.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"parkingSpaceId\":" + spaceId + ",\"duration\":2}"));
    }

    protected ResultActions enter(Session session, Long vehicleId, Long spaceId) throws Exception {
        return mockMvc.perform(post("/api/parking/entry").header("Authorization", session.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"vehicleId\":" + vehicleId + ",\"parkingSpaceId\":" + spaceId + "}"));
    }

    protected ResultActions reservationDetails(Session session, Long reservationId) throws Exception {
        return mockMvc.perform(get("/api/reservation/details").param("reservationId", reservationId.toString())
                .header("Authorization", session.bearer()));
    }

    /** The space's status as seen by an admin. */
    protected String spaceStatus(String adminBearer, Long spaceId) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/parkingspace/details/id")
                        .param("parkingSpaceId", spaceId.toString()).header("Authorization", adminBearer))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(body(result), "$.availabilityStatus");
    }

    protected static String body(MvcResult result) throws Exception {
        return result.getResponse().getContentAsString();
    }

    protected static Long longAt(MvcResult result, String path) throws Exception {
        Number value = JsonPath.read(body(result), path);
        return value.longValue();
    }
}
