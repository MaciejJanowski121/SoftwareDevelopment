package org.example.reservationsystem.DTO;

import java.time.LocalDateTime;

/**
 * Datenübertragungsobjekt (DTO) für eingehende Reservierungsanfragen.
 *
 * <p>Dieses Objekt wird vom Frontend an den Server gesendet, um eine neue
 * Tischreservierung anzulegen. Es enthält die Nummer des gewünschten Tisches
 * sowie Start- und Endzeit der Reservierung.</p>
 *
 * <p>Die Validierung des Zeitraums (Öffnungszeiten, Mindest-/Maximaldauer, Überschneidungen)
 * erfolgt serverseitig im {@code ReservationService}.</p>
 *
 * <p>Beispiel für eine typische JSON-Anfrage:</p>
 * <pre>{@code
 * {
 *   "tableNumber": 7,
 *   "startTime": "2025-06-09T18:00:00",
 *   "endTime":   "2025-06-09T20:00:00"
 * }
 * }</pre>
 *
 * <p><strong>Typische Verwendung im {@code ReservationController}:</strong></p>
 * <pre>{@code
 * @RestController
 * @RequestMapping("/api/reservations")
 * class ReservationController {
 *
 *   @PostMapping
 *   public ResponseEntity<ReservationViewDTO> createReservation(
 *           @Valid @RequestBody ReservationRequestDTO dto) {
 *     String email = currentEmail(); // aus SecurityContext (Subject = E-Mail)
 *     Reservation reservation = new Reservation(dto.getStartTime(), dto.getEndTime());
 *     Reservation saved = reservationService.addReservation(reservation, dto.getTableNumber(), email);
 *     return ResponseEntity.ok(toDto(saved));
 *   }
 * }
 * }</pre>
 *
 * <p>Hinweis: Der Endpunkt zur Abfrage freier Tische lautet
 * {@code GET /api/reservations/available?start=...&minutes=...} und verwendet dieses DTO nicht.</p>
 *
 * @author Maciej Janowski
 */
public class ReservationRequestDTO {

    /** Nummer des Tisches, der reserviert werden soll. */
    private int tableNumber;

    /** Beginn der gewünschten Reservierung (lokale Zeit). */
    private LocalDateTime startTime;

    /** Ende der gewünschten Reservierung (lokale Zeit). */
    private LocalDateTime endTime;

    /** @return die Tischnummer */
    public int getTableNumber() { return tableNumber; }
    public void setTableNumber(int tableNumber) { this.tableNumber = tableNumber; }

    /** @return Startzeitpunkt der Reservierung */
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    /** @return Endzeitpunkt der Reservierung */
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
}