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

/**
 * Integrationsnahe Komponententests für den {@link ReservationService}.
 *
 * <p>Die Tests führen echte Datenbankoperationen gegen die Testkonfiguration aus
 * (durch {@link SpringBootTest} und {@link Transactional}), wodurch persistente
 * Beziehungen zwischen {@link User}, {@link RestaurantTable} und {@link Reservation}
 * überprüft werden können.</p>
 *
 * <p>Die Tests validieren insbesondere die Geschäftslogik zum Hinzufügen und
 * Löschen von Reservierungen, einschließlich der Datenkonsistenz zwischen
 * den beteiligten Entitäten:</p>
 *
 * <ul>
 *   <li>Eine neue Reservierung wird korrekt gespeichert und mit Benutzer und Tisch verknüpft</li>
 *   <li>Beim Löschen einer Reservierung werden alle bidirektionalen Beziehungen korrekt aufgelöst</li>
 * </ul>
 *
 * <p>Jeder Test läuft in einer eigenen Transaktion, die nach Testende automatisch
 * zurückgerollt wird, sodass keine Seiteneffekte entstehen.</p>
 *
 * author Maciej Janowski
 */
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

    /**
     * Hilfsmethode, um eine Datum-/Uhrzeitkombination für „morgen um HH:MM Uhr“
     * zu generieren. Sekunden und Nanosekunden werden auf 0 gesetzt.
     *
     * @param hour   Stunde (24h-Format)
     * @param minute Minute
     * @return {@link LocalDateTime} für den Folgetag
     */
    private static LocalDateTime tomorrowAt(int hour, int minute) {
        return LocalDateTime.now()
                .plusDays(1)
                .withHour(hour).withMinute(minute)
                .withSecond(0).withNano(0);
    }

    /**
     * Testet den erfolgreichen Ablauf von {@link ReservationService#addReservation(Reservation, int, String)}.
     *
     * <p>Überprüft wird:</p>
     * <ul>
     *   <li>Die Reservierung wird in der Datenbank gespeichert (ID generiert)</li>
     *   <li>Die Entität {@link User} wird korrekt verknüpft</li>
     *   <li>Der zugehörige {@link RestaurantTable} wird korrekt zugewiesen</li>
     *   <li>Die Beziehungen sind auch nach Neuladen aus der Datenbank konsistent</li>
     * </ul>
     */
    @Test
    void testAddReservation_success() {

        String email = "john@example.com";
        User user = new User(
                "password",
                Role.ROLE_USER,
                "John Doe",
                email,
                "+49 170 1234567"
        );
        userRepository.saveAndFlush(user);

        RestaurantTable table = new RestaurantTable(4, 10);
        tableRepository.saveAndFlush(table);

        LocalDateTime start = tomorrowAt(18, 0);
        LocalDateTime end   = tomorrowAt(20, 0);
        Reservation reservation = new Reservation(start, end);


        Reservation saved = reservationService.addReservation(reservation, 10, email);


        assertNotNull(saved.getId(), "Reservation ID should be generated");
        assertNotNull(saved.getUser());
        assertEquals(email, saved.getUser().getEmail());
        assertNotNull(saved.getTable());
        assertEquals(10, saved.getTable().getTableNumber());


        User reloadedUser = userRepository.findByEmail(email).orElseThrow();
        assertNotNull(reloadedUser.getReservation());
        assertEquals(saved.getId(), reloadedUser.getReservation().getId());


        RestaurantTable reloadedTable = tableRepository.findTableByTableNumber(10).orElseThrow();
        assertTrue(
                reloadedTable.getReservations() != null &&
                        reloadedTable.getReservations().stream().anyMatch(r -> r.getId().equals(saved.getId())),
                "Table should contain the saved reservation"
        );
    }

    /**
     * Testet den Löschvorgang einer Reservierung über {@link ReservationService#deleteReservation(Long)}.
     *
     * <p>Überprüft wird:</p>
     * <ul>
     *   <li>Die Reservierung wird aus der Datenbank entfernt</li>
     *   <li>Der Benutzer verliert die Referenz auf seine Reservierung</li>
     *   <li>Der Tisch enthält die gelöschte Reservierung nicht mehr in seiner Liste</li>
     * </ul>
     */
    @Test
    void testDeleteReservation() {

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


        reservationService.deleteReservation(reservationId);
        reservationRepository.flush();


        assertTrue(reservationRepository.findById(reservationId).isEmpty());


        User reloadedUser = userRepository.findByEmail(email).orElseThrow();
        assertNull(reloadedUser.getReservation(), "User should not reference a reservation anymore");


        RestaurantTable reloadedTable = tableRepository.findTableByTableNumber(20).orElseThrow();
        assertTrue(
                reloadedTable.getReservations() == null ||
                        reloadedTable.getReservations().stream().noneMatch(r -> r.getId().equals(reservationId)),
                "Table reservations should not contain the deleted reservation"
        );
    }
}