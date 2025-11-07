package org.example.reservationsystem.DTO;

/**
 * Datenübertragungsobjekt (DTO) zur Darstellung von Restauranttischen.
 *
 * <p>{@code TableViewDTO} wird verwendet, um Informationen über verfügbare
 * oder bestehende Tische an das Frontend zu übertragen – z. B. bei der Auswahl
 * eines Tisches im Reservierungsformular.</p>
 *
 * <p>Das DTO enthält nur grundlegende, nicht-vertrauliche Daten aus der
 * Entität {@code RestaurantTable} und ist vollständig unveränderlich
 * (alle Felder sind {@code final}).</p>
 *
 * <p>Beispielhafte JSON-Antwort:</p>
 * <pre>{@code
 * {
 *   "id": 3,
 *   "tableNumber": 7,
 *   "numberOfSeats": 4
 * }
 * }</pre>
 *
 * <p><strong>Typische Verwendung im {@code ReservationService}:</strong></p>
 * <pre>{@code
 * // verfügbare Tische für ein Zeitfenster bestimmen
 * List<TableViewDTO> tables = reservationService.findAvailableTables(start, minutes);
 * return ResponseEntity.ok(tables);
 * }</pre>
 *
 * @author Maciej Janowski
 */
public class TableViewDTO {

    /** Eindeutige ID des Tisches. */
    private final Long id;

    /** Nummer des Tisches im Restaurant. */
    private final Integer tableNumber;

    /** Anzahl der Sitzplätze an diesem Tisch. */
    private final Integer numberOfSeats;

    /**
     * Erstellt ein neues {@code TableViewDTO}.
     *
     * @param id            eindeutige Tisch-ID
     * @param tableNumber   Tischnummer
     * @param numberOfSeats Anzahl der Sitzplätze
     */
    public TableViewDTO(Long id, Integer tableNumber, Integer numberOfSeats) {
        this.id = id;
        this.tableNumber = tableNumber;
        this.numberOfSeats = numberOfSeats;
    }

    /** @return eindeutige Tisch-ID */
    public Long getId() { return id; }

    /** @return Tischnummer */
    public Integer getTableNumber() { return tableNumber; }

    /** @return Anzahl der Sitzplätze */
    public Integer getNumberOfSeats() { return numberOfSeats; }
}