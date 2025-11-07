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

/**
 * Integrations test für die Reservierungs-REST-API.
 *
 * <p>Dieser Test startet den Spring-Kontext mit echter Webschicht ({@link MockMvc})
 * und prüft den End-to-End-Fluss für authentifizierte Benutzer via JWT-Cookie.
 * Abgedeckt sind:</p>
 *
 * <ul>
 *   <li><b>POST /api/reservations</b> – Reservierung anlegen, 200 + View-DTO</li>
 *   <li><b>GET  /api/reservations/userReservations</b> – eigene Reservierung abrufen, 200 + View-DTO</li>
 *   <li><b>DELETE /api/reservations/{id}</b> – Reservierung löschen, 204</li>
 * </ul>
 *
 * <p>Der Login-Identifier ist die E-Mail-Adresse. Für die Tests wird ein Benutzer,
 * ein Tisch sowie ein gültiges JWT erzeugt; das JWT wird als {@code token}-Cookie
 * an die Requests gehängt, sodass die Security-Konfiguration greift.</p>
 *
 * <p><strong>Hinweis:</strong> Der Test bereinigt vor jedem Lauf die relevanten
 * Repositories (Reihenfolge: Reservierungen → Benutzer → Tische), um Seiteneffekte
 * zwischen Testfällen zu vermeiden.</p>
 *
 * author Maciej Janowski
 */
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
    private String email;

    /**
     * Initialisiert Testdaten vor jedem Testfall:
     * <ol>
     *   <li>Leert abhängige Tabellen (Reservierungen → Benutzer → Tische)</li>
     *   <li>Erzeugt und speichert einen Benutzer (E-Mail als Benutzername)</li>
     *   <li>Erzeugt und speichert einen Tisch</li>
     *   <li>Erzeugt ein gültiges JWT und speichert es für nachfolgende Requests</li>
     * </ol>
     */
    @BeforeEach
    void setup() {

        reservationRepository.deleteAll();
        userRepository.deleteAll();
        tableRepository.deleteAll();


        email = "testuser@example.com";
        testUser = new User(
                "{noop}password123",
                Role.ROLE_USER,
                "Test User",
                email,
                "+49 170 0000000"
        );
        testUser = userRepository.save(testUser);


        testTable = new RestaurantTable();
        testTable.setTableNumber(5);
        testTable.setNumberOfSeats(4);
        testTable = tableRepository.save(testTable);


        jwtToken = jwtService.generateToken(testUser);
    }

    /**
     * Liefert ein gültiges Request-Body für eine 2-Stunden-Reservierung am Folgetag ab 18:00.
     * Zeiten werden als ISO_LOCAL_DATE_TIME (mit Sekunden) serialisiert.
     *
     * @return Map für JSON-Serialisierung
     */
    private Map<String, Object> validReservationPayload() {
        LocalDateTime start = LocalDateTime.now().plusDays(1)
                .withHour(18).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime end = start.plusHours(2);

        Map<String, Object> payload = new HashMap<>();
        payload.put("tableNumber", testTable.getTableNumber());
        payload.put("startTime", start.toString());
        payload.put("endTime", end.toString());
        return payload;
    }

    /**
     * End-to-End: Anlegen einer Reservierung.
     *
     * <p>Erwartet wird {@code 200 OK} sowie ein {@code ReservationViewDTO} mit:
     * E-Mail des Benutzers, Tischnummer, Start- und Endzeit. Die Authentifizierung
     * erfolgt durch Mitgabe des {@code token}-Cookies.</p>
     */
    @Test
    void createReservation_shouldReturnOk_withDtoResponse() throws Exception {
        Map<String, Object> body = validReservationPayload();

        mockMvc.perform(post("/api/reservations")
                        .cookie(new MockCookie("token", jwtToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.tableNumber").value(testTable.getTableNumber()))
                .andExpect(jsonPath("$.startTime").exists())
                .andExpect(jsonPath("$.endTime").exists());
    }

    /**
     * End-to-End: Nach dem Anlegen der Reservierung kann der eingeloggte Benutzer
     * seine eigene Reservierung über {@code GET /api/reservations/userReservations} abrufen.
     *
     * <p>Erwartet wird {@code 200 OK} und ein View-DTO mit E-Mail, Tischnummer,
     * Start- und Endzeit.</p>
     */
    @Test
    void getUserReservation_shouldReturnDto_afterCreation() throws Exception {
        Map<String, Object> body = validReservationPayload();

        mockMvc.perform(post("/api/reservations")
                        .cookie(new MockCookie("token", jwtToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/reservations/userReservations")
                        .cookie(new MockCookie("token", jwtToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.tableNumber").value(testTable.getTableNumber()))
                .andExpect(jsonPath("$.startTime").exists())
                .andExpect(jsonPath("$.endTime").exists());
    }

    /**
     * End-to-End: Löscht eine zuvor angelegte Reservierung.
     *
     * <p>Ablauf:
     * <ol>
     *   <li>Reservierung anlegen (POST)</li>
     *   <li>Reservierung aus dem Repository laden</li>
     *   <li>Reservierung mit {@code DELETE /api/reservations/{id}} löschen (erwartet: 204)</li>
     * </ol>
     * </p>
     */
    @Test
    void deleteReservation_shouldReturnNoContent() throws Exception {

        Map<String, Object> body = validReservationPayload();
        mockMvc.perform(post("/api/reservations")
                        .cookie(new MockCookie("token", jwtToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());


        Reservation created = reservationRepository.findAll().get(0);
        mockMvc.perform(delete("/api/reservations/" + created.getId())
                        .cookie(new MockCookie("token", jwtToken)))
                .andExpect(status().isNoContent());
    }
}