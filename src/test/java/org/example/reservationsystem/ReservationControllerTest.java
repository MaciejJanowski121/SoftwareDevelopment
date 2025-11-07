package org.example.reservationsystem;

import org.example.reservationsystem.DTO.ReservationRequestDTO;
import org.example.reservationsystem.DTO.TableViewDTO;
import org.example.reservationsystem.controller.ReservationController;
import org.example.reservationsystem.model.Reservation;
import org.example.reservationsystem.service.ReservationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Komponententest für den {@link ReservationController}.
 *
 * <p>Die Webschicht (Controller) wird isoliert mit einem gemockten
 * {@link ReservationService} getestet. Die Authentifizierung des
 * aufrufenden Benutzers wird über den {@link SecurityContextHolder}
 * simuliert. Dadurch können sowohl positive als auch negative
 * Pfade (z. B. 401 Unauthorized) reproduziert werden, ohne
 * den kompletten Spring-Web-Stack zu starten.</p>
 *
 * <p>Abgedeckte Szenarien:</p>
 * <ul>
 *   <li>Erstellen einer Reservierung – ohne Authentifizierung → 401</li>
 *   <li>Erstellen einer Reservierung – mit Authentifizierung → 200 und Übergabe an Service</li>
 *   <li>Löschen einer Reservierung → 204</li>
 *   <li>Eigene Reservierung abrufen – ohne Authentifizierung → 401</li>
 *   <li>Eigene Reservierung abrufen – vorhanden → 200</li>
 *   <li>Eigene Reservierung abrufen – nicht vorhanden → 204</li>
 *   <li>Verfügbare Tische abrufen → 200 + erwartete Liste</li>
 * </ul>
 *
 * <p>Wichtig: Der Login-Identifier ist die E-Mail des Benutzers.
 * In diesen Tests wird im Setup ein SecurityContext mit der E-Mail
 * {@code testuser@example.com} gesetzt.</p>
 *
 * author Maciej Janowski
 */
class ReservationControllerTest {

    private ReservationService reservationService;
    private ReservationController reservationController;

    /**
     * Setzt vor jedem Test einen gemockten {@link ReservationService} und
     * einen „eingeloggten“ Benutzer in den {@link SecurityContextHolder}.
     */
    @BeforeEach
    void setUp() {
        reservationService = mock(ReservationService.class);
        reservationController = new ReservationController(reservationService);
        // Standard: authentifizierter Benutzer; Tests für 401 leeren den Kontext selbst
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("testuser@example.com", null, List.of())
        );
    }

    /**
     * Räumt den SecurityContext nach jedem Test auf.
     */
    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    /**
     * createReservation: Ohne Authentifizierung muss der Controller 401 liefern
     * und den Service nicht aufrufen.
     */
    @Test
    void createReservation_shouldReturnUnauthorized_whenNoAuthentication() {
        SecurityContextHolder.clearContext(); // keine Authentifizierung

        ReservationRequestDTO dto = new ReservationRequestDTO();
        dto.setTableNumber(1);
        dto.setStartTime(LocalDateTime.of(2025, 10, 30, 18, 0));
        dto.setEndTime(LocalDateTime.of(2025, 10, 30, 20, 0));

        ResponseEntity<?> resp = reservationController.createReservation(dto);

        assertEquals(401, resp.getStatusCodeValue());
        assertNull(resp.getBody());
        verifyNoInteractions(reservationService);
    }

    /**
     * createReservation: Mit gültiger Authentifizierung wird die Reservierung
     * an den Service weitergereicht und 200 OK zurückgegeben.
     */
    @Test
    void createReservation_shouldReturnOk_whenAuthenticatedAndPayloadValid() {
        // E-Mail aus setUp: testuser@example.com
        ReservationRequestDTO dto = new ReservationRequestDTO();
        dto.setTableNumber(5);
        LocalDateTime start = LocalDateTime.of(2025, 10, 30, 18, 0);
        LocalDateTime end   = LocalDateTime.of(2025, 10, 30, 20, 0);
        dto.setStartTime(start);
        dto.setEndTime(end);

        Reservation saved = new Reservation(start, end);
        saved.setId(123L);

        when(reservationService.addReservation(any(Reservation.class), eq(5), eq("testuser@example.com")))
                .thenReturn(saved);

        ResponseEntity<?> resp = reservationController.createReservation(dto);

        assertEquals(200, resp.getStatusCodeValue());
        assertNotNull(resp.getBody());
        verify(reservationService).addReservation(any(Reservation.class), eq(5), eq("testuser@example.com"));
    }

    /**
     * deleteReservation: Es wird 204 No Content erwartet und der Service
     * muss mit der richtigen ID aufgerufen werden.
     */
    @Test
    void deleteReservation_shouldReturnNoContent() {
        doNothing().when(reservationService).deleteReservation(42L);

        ResponseEntity<Void> resp = reservationController.deleteReservation(42L);

        assertEquals(204, resp.getStatusCodeValue());
        verify(reservationService).deleteReservation(42L);
    }

    /**
     * getUserReservation: Ohne Authentifizierung → 401 Unauthorized.
     */
    @Test
    void getUserReservation_shouldReturnUnauthorized_whenNoAuthentication() {
        SecurityContextHolder.clearContext();

        ResponseEntity<?> resp = reservationController.getUserReservation();

        assertEquals(401, resp.getStatusCodeValue());
        verifyNoInteractions(reservationService);
    }

    /**
     * getUserReservation: Es existiert eine Reservierung → 200 OK
     * und Übergabe der E-Mail aus dem SecurityContext an den Service.
     */
    @Test
    void getUserReservation_shouldReturnOk_whenReservationExists() {
        // E-Mail aus setUp: testuser@example.com
        Reservation r = new Reservation(
                LocalDateTime.of(2025, 6, 8, 18, 0),
                LocalDateTime.of(2025, 6, 8, 20, 0)
        );
        r.setId(7L);

        when(reservationService.getUserReservation("testuser@example.com")).thenReturn(r);

        ResponseEntity<?> resp = reservationController.getUserReservation();

        assertEquals(200, resp.getStatusCodeValue());
        assertNotNull(resp.getBody());
        verify(reservationService).getUserReservation("testuser@example.com");
    }

    /**
     * getUserReservation: Keine Reservierung vorhanden → 204 No Content.
     */
    @Test
    void getUserReservation_shouldReturnNoContent_whenReservationMissing() {
        when(reservationService.getUserReservation("testuser@example.com")).thenReturn(null);

        ResponseEntity<?> resp = reservationController.getUserReservation();

        assertEquals(204, resp.getStatusCodeValue());
    }

    /**
     * getAvailableTables: Der Controller parst Eingaben und gibt die vom Service
     * ermittelte Liste verfügbarer Tische (als DTOs) zurück.
     */
    @Test
    void getAvailableTables_shouldReturnList() {
        LocalDateTime start = LocalDateTime.of(2025, 10, 30, 18, 0);
        when(reservationService.findAvailableTables(start, 120))
                .thenReturn(List.of(new TableViewDTO(10L, 3, 2)));

        ResponseEntity<List<TableViewDTO>> resp =
                reservationController.getAvailableTables("2025-10-30T18:00:00", 120);

        assertEquals(200, resp.getStatusCodeValue());
        assertNotNull(resp.getBody());
        assertEquals(1, resp.getBody().size());
        assertEquals(3, resp.getBody().get(0).getTableNumber());
        assertEquals(2, resp.getBody().get(0).getNumberOfSeats());
    }
}