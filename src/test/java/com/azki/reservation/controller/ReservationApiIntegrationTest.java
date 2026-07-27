package com.azki.reservation.controller;

import com.azki.reservation.entity.AvailableSlot;
import com.azki.reservation.repository.AvailableSlotRepository;
import com.azki.reservation.repository.ReservationRepository;
import com.azki.reservation.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class ReservationApiIntegrationTest {

    private static final String USERNAME = "api_test_user";
    private static final String PASSWORD = "SecurePass123";

    @Container
    static MySQLContainer mysql = new MySQLContainer("mysql:8.0")
            .withDatabaseName("azki_reservation_api")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AvailableSlotRepository slotRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private UserRepository userRepository;

    private String token;

    @BeforeEach
    void setUp() throws Exception {
        reservationRepository.deleteAll();
        slotRepository.deleteAll();
        userRepository.deleteAll();

        createSlots(3);
        registerUser();
        token = login();
    }

    @Test
    @DisplayName("Reserving returns the nearest available slot")
    void reserveReturnsNearestSlot() throws Exception {
        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slotId").isNumber())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("Cancelling an own reservation returns no content")
    void cancelOwnReservation() throws Exception {
        MvcResult reserved = mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated())
                .andReturn();

        long reservationId = extractReservationId(reserved);

        mockMvc.perform(delete("/api/reservations/" + reservationId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Cancelling a non-existent reservation returns not found")
    void cancelMissingReservation() throws Exception {
        mockMvc.perform(delete("/api/reservations/999999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Reserving without a token is rejected")
    void reserveWithoutTokenIsRejected() throws Exception {
        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Registering with an invalid email returns field-level errors")
    void registerWithInvalidEmail() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"someone","email":"not-an-email","password":"SecurePass123"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("email"));
    }

    @Test
    @DisplayName("Listing available slots returns the seeded slots")
    void listAvailableSlots() throws Exception {
        mockMvc.perform(get("/api/reservations/available")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").isNumber())
                .andExpect(jsonPath("$[0].startTime").exists());
    }

    @Test
    @DisplayName("Listing available slots without a token is rejected")
    void listAvailableSlotsWithoutTokenIsRejected() throws Exception {
        mockMvc.perform(get("/api/reservations/available"))
                .andExpect(status().isUnauthorized());
    }
    
    private void createSlots(int count) {
        LocalDateTime base = LocalDateTime.now().plusHours(1);
        for (int i = 0; i < count; i++) {
            AvailableSlot slot = new AvailableSlot();
            slot.setStartTime(base.plusHours(i));
            slot.setEndTime(base.plusHours(i + 1));
            slot.setReserved(false);
            slotRepository.save(slot);
        }
    }

    private void registerUser() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","email":"api_test@example.com","password":"%s"}"""
                                .formatted(USERNAME, PASSWORD)))
                .andExpect(status().isCreated());
    }

    private String login() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}""".formatted(USERNAME, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();

        return extractJsonValue(result.getResponse().getContentAsString(), "token");
    }

    private long extractReservationId(MvcResult result) throws Exception {
        return Long.parseLong(extractJsonValue(result.getResponse().getContentAsString(), "reservationId"));
    }

    private String extractJsonValue(String json, String key) {
        int start = json.indexOf("\"" + key + "\":") + key.length() + 3;
        if (json.charAt(start) == '"') {
            start++;
            return json.substring(start, json.indexOf('"', start));
        }
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)))) {
            end++;
        }
        return json.substring(start, end);
    }
}