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

class ReservationControllerTest {

    private ReservationService reservationService;
    private ReservationController reservationController;

    @BeforeEach
    void setUp() {
        reservationService = mock(ReservationService.class);
        reservationController = new ReservationController(reservationService);
        // domyślnie „zalogowany” użytkownik; testy 401 same wyczyszczą kontekst
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("testuser@example.com", null, List.of())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createReservation_shouldReturnUnauthorized_whenNoAuthentication() {
        // brak zalogowanego użytkownika
        SecurityContextHolder.clearContext();

        ReservationRequestDTO dto = new ReservationRequestDTO();
        dto.setTableNumber(1);
        dto.setStartTime(LocalDateTime.of(2025, 10, 30, 18, 0));
        dto.setEndTime(LocalDateTime.of(2025, 10, 30, 20, 0));

        ResponseEntity<?> resp = reservationController.createReservation(dto);

        assertEquals(401, resp.getStatusCodeValue());
        assertNull(resp.getBody());
        verifyNoInteractions(reservationService);
    }

    @Test
    void createReservation_shouldReturnOk_whenAuthenticatedAndPayloadValid() {
        // w setUp ustawiliśmy e-mail = testuser@example.com
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

    @Test
    void deleteReservation_shouldReturnNoContent() {
        doNothing().when(reservationService).deleteReservation(42L);

        ResponseEntity<Void> resp = reservationController.deleteReservation(42L);

        assertEquals(204, resp.getStatusCodeValue());
        verify(reservationService).deleteReservation(42L);
    }

    @Test
    void getUserReservation_shouldReturnUnauthorized_whenNoAuthentication() {
        SecurityContextHolder.clearContext();

        ResponseEntity<?> resp = reservationController.getUserReservation();

        assertEquals(401, resp.getStatusCodeValue());
        verifyNoInteractions(reservationService);
    }

    @Test
    void getUserReservation_shouldReturnOk_whenReservationExists() {
        // e-mail z setUp: testuser@example.com
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

    @Test
    void getUserReservation_shouldReturnNoContent_whenReservationMissing() {
        when(reservationService.getUserReservation("testuser@example.com")).thenReturn(null);

        ResponseEntity<?> resp = reservationController.getUserReservation();

        assertEquals(204, resp.getStatusCodeValue());
    }

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