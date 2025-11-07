
package org.example.reservationsystem.service;

import jakarta.persistence.EntityNotFoundException;
import org.example.reservationsystem.DTO.TableViewDTO;
import org.example.reservationsystem.exceptions.TableAlreadyReservedException;
import org.example.reservationsystem.exceptions.TableNotFoundException;
import org.example.reservationsystem.exceptions.UserAlreadyHasReservationException;
import org.example.reservationsystem.exceptions.UserNotFoundException;
import org.example.reservationsystem.model.Reservation;
import org.example.reservationsystem.model.RestaurantTable;
import org.example.reservationsystem.model.User;
import org.example.reservationsystem.repository.ReservationRepository;
import org.example.reservationsystem.repository.TableRepository;
import org.example.reservationsystem.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service für das Erstellen, Abfragen und Löschen von Reservierungen
 * sowie zur Ermittlung freier Tische.
 *
 * <p>Der Service kapselt die Geschäftslogik rund um Zeitvalidierung,
 * Kollisionserkennung und 1:1-Beziehung zwischen Benutzer und Reservierung.
 * Öffnungszeiten- und Dauergrenzen sind hier zentral hinterlegt.</p>
 *
 * <p>Transaktionen:
 * <ul>
 *   <li>Die Klasse ist als Ganzes transaktional. Lesezugriffe sind bei den
 *       mit {@code readOnly = true} markierten Methoden optimiert.</li>
 * </ul>
 * </p>
 *
 * <p>Ausnahmen:
 * <ul>
 *   <li>{@link UserNotFoundException}, wenn ein Benutzer zur angegebenen E-Mail nicht existiert.</li>
 *   <li>{@link UserAlreadyHasReservationException}, wenn der Benutzer bereits eine Reservierung besitzt.</li>
 *   <li>{@link TableNotFoundException}, wenn der gewünschte Tisch nicht existiert.</li>
 *   <li>{@link TableAlreadyReservedException}, wenn sich der Zeitraum mit einer bestehenden Reservierung überschneidet.</li>
 *   <li>{@link EntityNotFoundException}, wenn eine zu löschende Reservierung nicht existiert.</li>
 *   <li>{@link IllegalArgumentException}, wenn Eingaben (Zeiten/Dauer) ungültig sind.</li>
 * </ul>
 * </p>
 *
 * <p>Hinweis: Der Login-Identifier ist die E-Mail des Benutzers und wird
 * für Abfragen normalisiert (trim + lower case).</p>
 *
 * @author Maciej Janowski
 */
@Service
@Transactional
public class ReservationService {

    private static final Duration MIN_DURATION     = Duration.ofMinutes(30);
    private static final Duration MAX_DURATION     = Duration.ofHours(5);
    private static final Duration DEFAULT_DURATION = Duration.ofHours(2);

    private static final int MIN_MINUTES = 30;
    private static final int MAX_MINUTES = 300;

    private final ReservationRepository reservationRepository;
    private final TableRepository tableRepository;
    private final UserRepository userRepository;

    /**
     * Erstellt einen neuen {@code ReservationService}.
     *
     * @param reservationRepository Repository für Reservierungen
     * @param tableRepository       Repository für Restauranttische
     * @param userRepository        Repository für Benutzer
     */
    public ReservationService(ReservationRepository reservationRepository,
                              TableRepository tableRepository,
                              UserRepository userRepository) {
        this.reservationRepository = reservationRepository;
        this.tableRepository = tableRepository;
        this.userRepository = userRepository;
    }

    /**
     * Legt eine Reservierung für den Benutzer mit der angegebenen E-Mail an.
     *
     * <p>Ablauf:
     * <ol>
     *   <li>Benutzer anhand E-Mail laden und 1:1-Beziehung prüfen.</li>
     *   <li>Tisch anhand Tischnummer laden.</li>
     *   <li>Fehlende Endzeit ggf. auf Startzeit + 2h setzen.</li>
     *   <li>Eingaben und Zeitfenster validieren (Gegenwart, Dauer, Öffnungszeiten).</li>
     *   <li>Kollisionen gegen bestehende Reservierungen prüfen.</li>
     *   <li>Beziehungen setzen und Reservierung speichern.</li>
     * </ol>
     * </p>
     *
     * @param reservation  Reservierungsobjekt mit Start-/Endzeit
     * @param tableNumber  Tischnummer
     * @param email        E-Mail des Benutzers (Login-Identifier)
     * @return gespeicherte Reservierung
     *
     * @throws UserNotFoundException               wenn kein Benutzer existiert
     * @throws UserAlreadyHasReservationException  wenn der Benutzer bereits eine Reservierung hat
     * @throws TableNotFoundException              wenn der Tisch nicht existiert
     * @throws TableAlreadyReservedException       bei Überschneidung mit bestehender Reservierung
     * @throws IllegalArgumentException            bei ungültigen Zeiten oder Dauer
     */
    public Reservation addReservation(Reservation reservation, int tableNumber, String email) {
        User user = userRepository.findByEmail(normalize(email))
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (user.getReservation() != null) {
            throw new UserAlreadyHasReservationException();
        }

        RestaurantTable table = tableRepository.findTableByTableNumber(tableNumber)
                .orElseThrow(() -> new TableNotFoundException("Table with number " + tableNumber + " does not exist."));

        if (reservation.getStartTime() != null && reservation.getEndTime() == null) {
            reservation.setEndTime(reservation.getStartTime().plus(DEFAULT_DURATION));
        }

        validateReservationInput(reservation);

        boolean overlaps = reservationRepository
                .existsByTable_IdAndStartTimeLessThanAndEndTimeGreaterThan(
                        table.getId(),
                        reservation.getEndTime(),
                        reservation.getStartTime()
                );
        if (overlaps) {
            throw new TableAlreadyReservedException(tableNumber);
        }

        reservation.setUser(user);
        reservation.setTable(table);
        user.setReservation(reservation);
        if (table.getReservations() != null) {
            table.getReservations().add(reservation);
        }

        Reservation saved = reservationRepository.save(reservation);
        userRepository.save(user);
        return saved;
    }

    /**
     * Liefert die Reservierung des Benutzers mit der angegebenen E-Mail,
     * oder {@code null}, wenn keine existiert.
     *
     * @param email E-Mail des Benutzers (Login-Identifier)
     * @return Reservierung oder {@code null}, wenn nicht vorhanden
     */
    @Transactional(readOnly = true)
    public Reservation getUserReservation(String email) {
        return reservationRepository.findByUser_Email(normalize(email)).orElse(null);
    }

    /**
     * Löscht eine Reservierung anhand ihrer ID.
     *
     * <p>Vor dem Löschen werden die bidirektionalen Beziehungen zu
     * Tisch und Benutzer konsistent aufgelöst.</p>
     *
     * @param id Primärschlüssel der Reservierung
     * @throws EntityNotFoundException wenn keine Reservierung mit der ID existiert
     */
    public void deleteReservation(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Reservation not found"));

        RestaurantTable table = reservation.getTable();
        if (table != null && table.getReservations() != null) {
            table.getReservations().remove(reservation);
        }
        reservation.setTable(null);

        User user = reservation.getUser();
        if (user != null) {
            user.setReservation(null);
            reservation.setUser(null);
            userRepository.save(user);
        }

        reservationRepository.delete(reservation);
    }

    /**
     * Liefert alle Reservierungen (ohne Filter).
     *
     * @return Liste aller Reservierungen
     */
    @Transactional(readOnly = true)
    public List<Reservation> getAllReservations() {
        return reservationRepository.findAll();
    }

    /**
     * Ermittelt alle freien Tische für ein gewünschtes Zeitfenster.
     *
     * <p>Die gewünschte Dauer wird auf einen zulässigen Bereich geklammert
     * ({@code 30}–{@code 300} Minuten). Ein Tisch gilt als belegt, wenn bereits
     * eine Reservierung existiert, deren Zeitraum sich mit dem angefragten
     * Zeitfenster überschneidet
     * ({@code existing.start < requested.end && existing.end > requested.start}).</p>
     *
     * @param start   Beginn des gewünschten Zeitfensters (lokale Zeit)
     * @param minutes gewünschte Dauer in Minuten; {@code null} entspricht {@code 30}
     * @return Liste freier Tische als {@link TableViewDTO}
     */
    @Transactional(readOnly = true)
    public List<TableViewDTO> findAvailableTables(LocalDateTime start, Integer minutes) {
        int clamped = clampMinutes(minutes);
        LocalDateTime end = start.plusMinutes(clamped);

        return tableRepository.findAll().stream()
                .filter(table -> !reservationRepository
                        .existsByTable_IdAndStartTimeLessThanAndEndTimeGreaterThan(
                                table.getId(), end, start))
                .map(t -> new TableViewDTO(t.getId(), t.getTableNumber(), t.getNumberOfSeats()))
                .toList();
    }

    /**
     * Klammerfunktion für Dauerangaben in Minuten.
     *
     * @param minutes gewünschte Dauer in Minuten (kann {@code null} sein)
     * @return Wert im Bereich [{@value #MIN_MINUTES}, {@value #MAX_MINUTES}]
     */
    private static int clampMinutes(Integer minutes) {
        if (minutes == null) return MIN_MINUTES;
        if (minutes < MIN_MINUTES) return MIN_MINUTES;
        if (minutes > MAX_MINUTES) return MAX_MINUTES;
        return minutes;
    }

    /**
     * Validiert Start- und Endzeit einer Reservierung inklusive Dauer- und
     * Öffnungszeitenregeln.
     *
     * <ul>
     *   <li>Start/Ende dürfen nicht {@code null} sein und Ende muss nach Start liegen.</li>
     *   <li>Start darf nicht in der Vergangenheit liegen.</li>
     *   <li>Dauer im Bereich 30 Minuten bis 5 Stunden.</li>
     *   <li>Ende maximal bis 22:00 Uhr desselben Tages.</li>
     * </ul>
     *
     * @param r Reservierung mit Zeitwerten
     * @throws IllegalArgumentException bei Regelverstößen
     */
    private void validateReservationInput(Reservation r) {
        if (r == null) throw new IllegalArgumentException("Reservation cannot be null.");
        if (r.getStartTime() == null) throw new IllegalArgumentException("Reservation must have start time.");
        if (r.getEndTime() == null) throw new IllegalArgumentException("Reservation must have end time.");
        if (!r.getEndTime().isAfter(r.getStartTime())) {
            throw new IllegalArgumentException("End time must be after start time.");
        }
        LocalDateTime now = LocalDateTime.now();
        if (r.getStartTime().isBefore(now)) {
            throw new IllegalArgumentException("Reservation start time cannot be in the past.");
        }
        long minutes = Duration.between(r.getStartTime(), r.getEndTime()).toMinutes();
        if (minutes < MIN_DURATION.toMinutes() || minutes > MAX_DURATION.toMinutes()) {
            throw new IllegalArgumentException("Reservation must be between 30 minutes and 5 hours.");
        }
        LocalDateTime latestAllowed = r.getStartTime().toLocalDate().atTime(22, 0);
        if (r.getEndTime().isAfter(latestAllowed)) {
            throw new IllegalArgumentException("Reservations are only allowed until 22:00.");
        }
    }

    /**
     * Normalisiert Zeichenketten (Trim + Kleinschreibung); lässt {@code null} zu.
     *
     * @param s Eingabewert oder {@code null}
     * @return normalisierte Zeichenkette oder {@code null}
     */
    private static String normalize(String s) {
        return s == null ? null : s.trim().toLowerCase();
    }
}