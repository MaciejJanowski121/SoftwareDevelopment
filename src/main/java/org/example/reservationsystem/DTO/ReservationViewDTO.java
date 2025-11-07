package org.example.reservationsystem.DTO;

import java.time.LocalDateTime;

/**
 * Datenübertragungsobjekt (DTO) zur Darstellung von Reservierungsinformationen.
 *
 * <p>{@code ReservationViewDTO} wird verwendet, um Reservierungsdaten an das Frontend
 * zurückzugeben – z. B. bei der Anzeige eigener Reservierungen eines Benutzers
 * oder in der Administrationsansicht aller Reservierungen.</p>
 *
 * <p>Das DTO enthält nur nicht-vertrauliche Informationen und keine direkten
 * Entitätsreferenzen (z. B. {@code User}, {@code RestaurantTable}).</p>
 *
 * <p>Beispielhafte JSON-Antwort:</p>
 * <pre>{@code
 * {
 *   "id": 42,
 *   "email": "user@example.com",
 *   "fullName": "Max Mustermann",
 *   "tableNumber": 5,
 *   "startTime": "2025-06-09T18:00:00",
 *   "endTime":   "2025-06-09T20:00:00"
 * }
 * }</pre>
 *
 * <p><strong>Typische Verwendung im {@code ReservationController}:</strong></p>
 * <pre>{@code
 * @GetMapping("/userReservations")
 * public ResponseEntity<ReservationViewDTO> getUserReservation() {
 *     String email = currentEmail();
 *     Reservation r = reservationService.getUserReservation(email);
 *     if (r == null) return ResponseEntity.noContent().build();
 *     return ResponseEntity.ok(toDto(r));
 * }
 *
 * private ReservationViewDTO toDto(Reservation r) {
 *     return new ReservationViewDTO(
 *             r.getId(),
 *             r.getUser() != null ? r.getUser().getEmail() : null,
 *             r.getUser() != null ? r.getUser().getFullName() : null,
 *             r.getTable() != null ? r.getTable().getTableNumber() : null,
 *             r.getStartTime(),
 *             r.getEndTime()
 *     );
 * }
 * }</pre>
 *
 * <p>Dieses DTO wird typischerweise im {@code ReservationService} oder
 * {@code ReservationController} erstellt, um eine flache, JSON-kompatible
 * Struktur für REST-Antworten zu liefern.</p>
 *
 * @author Maciej Janowski
 */
public record ReservationViewDTO(
        /** Eindeutige ID der Reservierung. */
        Long id,

        /** E-Mail-Adresse des Benutzers, der die Reservierung erstellt hat. */
        String email,

        /** Vollständiger Name des Benutzers. */
        String fullName,

        /** Nummer des reservierten Tisches. */
        Integer tableNumber,

        /** Beginn der Reservierung (lokale Zeit). */
        LocalDateTime startTime,

        /** Ende der Reservierung (lokale Zeit). */
        LocalDateTime endTime
) {}