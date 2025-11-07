package org.example.reservationsystem.repository;

import org.example.reservationsystem.model.Reservation;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Repository für {@link Reservation}-Entitäten.
 *
 * <p>Erweitert {@link JpaRepository}, um Standard-CRUD-Operationen bereitzustellen,
 * und definiert zusätzlich Methoden zur Prüfung von Reservierungskonflikten
 * sowie zum Laden einer Reservierung anhand der Benutzer-E-Mail.</p>
 *
 * <p>Dieses Repository wird hauptsächlich im {@code ReservationService}
 * verwendet, um Zeitüberschneidungen zu prüfen und Benutzerreservierungen
 * effizient zu laden.</p>
 *
 * @author Maciej Janowski
 */
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    /**
     * Prüft, ob für einen bestimmten Tisch eine Reservierung existiert,
     * deren Zeitraum sich mit dem angegebenen Fenster überschneidet.
     *
     * <p>Formale Bedingung:
     * {@code existing.startTime < requested.endTime && existing.endTime > requested.startTime}</p>
     *
     * @param tableId ID des Tisches
     * @param end     Ende des gewünschten Zeitraums
     * @param start   Beginn des gewünschten Zeitraums
     * @return {@code true}, wenn eine Überschneidung besteht, sonst {@code false}
     */
    boolean existsByTable_IdAndStartTimeLessThanAndEndTimeGreaterThan(
            Long tableId,
            LocalDateTime end,
            LocalDateTime start
    );

    /**
     * Lädt eine Reservierung anhand der E-Mail-Adresse des Benutzers.
     *
     * <p>Durch die Annotation {@link EntityGraph} werden die zugehörigen
     * Entitäten {@code user} und {@code table} eager geladen, um
     * Lazy-Loading-Probleme (z. B. in REST-Antworten) zu vermeiden.</p>
     *
     * @param email E-Mail-Adresse des Benutzers (Login-Identifier)
     * @return Optional mit der gefundenen Reservierung oder leer, falls keine existiert
     */
    @EntityGraph(attributePaths = {"user", "table"})
    Optional<Reservation> findByUser_Email(String email);
}