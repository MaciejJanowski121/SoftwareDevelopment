// src/test/java/org/example/reservationsystem/ReservationServiceTest.java
package org.example.reservationsystem;

import jakarta.transaction.Transactional;
import org.example.reservationsystem.model.Reservation;
import org.example.reservationsystem.model.RestaurantTable;
import org.example.reservationsystem.model.Role;
import org.example.reservationsystem.model.User;
import org.example.reservationsystem.repository.ReservationRepository;
import org.example.reservationsystem.repository.TableRepository;
import org.example.reservationsystem.repository.UserRepository;
import org.example.reservationsystem.service.ReservationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class ReservationServiceTest {

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private TableRepository tableRepository;

    @Autowired
    private UserRepository userRepository;

    private static LocalDateTime tomorrowAt(int hour, int minute) {
        return LocalDateTime.now()
                .plusDays(1)
                .withHour(hour).withMinute(minute)
                .withSecond(0).withNano(0);
    }

    @Test
    void testAddReservation_success() {
        // given: user + table (UWAGA: przekazujemy poprawny e-mail)
        String email = "john@example.com";
        User user = new User(
                /* password */ "password",
                /* role     */ Role.ROLE_USER,
                /* fullName */ "John Doe",
                /* email    */ email,
                /* phone    */ "+49 170 1234567"
        );
        userRepository.saveAndFlush(user);

        RestaurantTable table = new RestaurantTable(4, 10);
        tableRepository.saveAndFlush(table);

        LocalDateTime start = tomorrowAt(18, 0);
        LocalDateTime end   = tomorrowAt(20, 0);
        Reservation reservation = new Reservation(start, end);

        // when: przekazujemy email do addReservation (ReservationService szuka po emailu)
        Reservation saved = reservationService.addReservation(reservation, 10, email);

        // then
        assertNotNull(saved.getId(), "Reservation ID should be generated");
        assertNotNull(saved.getUser());
        assertEquals(email, saved.getUser().getEmail());       // źródłowy e-mail
        assertNotNull(saved.getTable());
        assertEquals(10, saved.getTable().getTableNumber());

        // użytkownik ma powiązaną rezerwację
        User reloadedUser = userRepository.findByEmail(email).orElseThrow();
        assertNotNull(reloadedUser.getReservation());
        assertEquals(saved.getId(), reloadedUser.getReservation().getId());

        // stolik zawiera rezerwację (jeśli kolekcja jest inicjalizowana)
        RestaurantTable reloadedTable = tableRepository.findTableByTableNumber(10).orElseThrow();
        assertTrue(
                reloadedTable.getReservations() != null &&
                        reloadedTable.getReservations().stream().anyMatch(r -> r.getId().equals(saved.getId())),
                "Table should contain the saved reservation"
        );
    }

    @Test
    void testDeleteReservation() {
        // given
        String email = "max@example.com";
        User user = new User(
                "password",
                Role.ROLE_USER,
                "Max Mustermann",
                email,
                "+49 160 0000000"
        );
        userRepository.saveAndFlush(user);

        RestaurantTable table = new RestaurantTable(2, 20);
        tableRepository.saveAndFlush(table);

        LocalDateTime start = tomorrowAt(18, 0);
        LocalDateTime end   = tomorrowAt(19, 0);
        Reservation toSave  = new Reservation(start, end);

        Reservation saved = reservationService.addReservation(toSave, 20, email);
        Long reservationId = saved.getId();
        assertNotNull(reservationRepository.findById(reservationId).orElse(null));

        // when
        reservationService.deleteReservation(reservationId);
        reservationRepository.flush();

        // then: rezerwacja usunięta
        assertTrue(reservationRepository.findById(reservationId).isEmpty());

        // user odpięty od rezerwacji
        User reloadedUser = userRepository.findByEmail(email).orElseThrow();
        assertNull(reloadedUser.getReservation(), "User should not reference a reservation anymore");

        // stolik nie zawiera usuniętej rezerwacji
        RestaurantTable reloadedTable = tableRepository.findTableByTableNumber(20).orElseThrow();
        assertTrue(
                reloadedTable.getReservations() == null ||
                        reloadedTable.getReservations().stream().noneMatch(r -> r.getId().equals(reservationId)),
                "Table reservations should not contain the deleted reservation"
        );
    }
}