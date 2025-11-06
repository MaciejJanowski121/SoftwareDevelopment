package org.example.reservationsystem;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.reservationsystem.JWTServices.JwtService;
import org.example.reservationsystem.model.Reservation;
import org.example.reservationsystem.model.RestaurantTable;
import org.example.reservationsystem.model.Role;
import org.example.reservationsystem.model.User;
import org.example.reservationsystem.repository.ReservationRepository;
import org.example.reservationsystem.repository.TableRepository;
import org.example.reservationsystem.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockCookie;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class ReservationIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Autowired private UserRepository userRepository;
    @Autowired private TableRepository tableRepository;
    @Autowired private ReservationRepository reservationRepository;

    @Autowired private JwtService jwtService;

    private String jwtToken;
    private User testUser;
    private RestaurantTable testTable;

    private String email; // e-mail jako tożsamość

    @BeforeEach
    void setup() {
        // Kolejność czyszczenia (najpierw zależne encje)
        reservationRepository.deleteAll();
        userRepository.deleteAll();
        tableRepository.deleteAll();

        // Użytkownik – u Was e-mail jest tożsamością, więc username = email
        email = "testuser@example.com";
        testUser = new User(
                email,                 // username (traktujemy jako e-mail)
                "{noop}password123",   // hasło nie jest tu weryfikowane
                Role.ROLE_USER,
                "Test User",           // fullName
                email,                 // email
                "+49 170 0000000"      // phone
        );
        testUser = userRepository.save(testUser);

        // Stolik nr 5
        testTable = new RestaurantTable();
        testTable.setTableNumber(5);
        testTable.setNumberOfSeats(4);
        testTable = tableRepository.save(testTable);

        // JWT, który w payloadzie niesie „username” = e-mail
        jwtToken = jwtService.generateToken(testUser);
    }

    private Map<String, Object> validReservationPayload() {
        // Jutro 18:00–20:00
        LocalDateTime start = LocalDateTime.now().plusDays(1)
                .withHour(18).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime end = start.plusHours(2);

        Map<String, Object> payload = new HashMap<>();
        payload.put("tableNumber", testTable.getTableNumber());
        payload.put("startTime", start.toString()); // ISO_LOCAL_DATE_TIME z sekundami
        payload.put("endTime", end.toString());
        return payload;
    }

    @Test
    void createReservation_shouldReturnOk_withDtoResponse() throws Exception {
        Map<String, Object> body = validReservationPayload();

        mockMvc.perform(post("/api/reservations")
                        .cookie(new MockCookie("token", jwtToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(email))               // <- e-mail
                .andExpect(jsonPath("$.tableNumber").value(testTable.getTableNumber()))
                .andExpect(jsonPath("$.startTime").exists())
                .andExpect(jsonPath("$.endTime").exists());
    }

    @Test
    void getUserReservation_shouldReturnDto_afterCreation() throws Exception {
        // Najpierw utwórz rezerwację
        Map<String, Object> body = validReservationPayload();
        mockMvc.perform(post("/api/reservations")
                        .cookie(new MockCookie("token", jwtToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        // Pobierz własną rezerwację
        mockMvc.perform(get("/api/reservations/userReservations")
                        .cookie(new MockCookie("token", jwtToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(email))               // <- e-mail
                .andExpect(jsonPath("$.tableNumber").value(testTable.getTableNumber()))
                .andExpect(jsonPath("$.startTime").exists())
                .andExpect(jsonPath("$.endTime").exists());
    }

    @Test
    void deleteReservation_shouldReturnNoContent() throws Exception {
        // Utwórz rezerwację
        Map<String, Object> body = validReservationPayload();
        mockMvc.perform(post("/api/reservations")
                        .cookie(new MockCookie("token", jwtToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        // Usuń utworzoną rezerwację
        Reservation created = reservationRepository.findAll().get(0);
        mockMvc.perform(delete("/api/reservations/" + created.getId())
                        .cookie(new MockCookie("token", jwtToken)))
                .andExpect(status().isNoContent());
    }
}