package org.example.reservationsystem.exceptions;

/**
 * Wird ausgelöst, wenn ein Tisch im gewünschten Zeitraum bereits reserviert ist.
 *
 * <p>Dieser Fehler tritt auf, wenn eine neue Reservierung für einen Tisch angelegt werden soll,
 * dessen Zeitfenster sich mit einer bestehenden Reservierung überschneidet.</p>
 *
 * <p>Im {@link org.example.reservationsystem.exceptions.GlobalExceptionHandler}
 * wird dieser Fehler zu einem HTTP-Status {@code 409 Conflict} mit Typ
 * {@code /errors/table-already-reserved} gemappt.</p>
 *
 * @see org.example.reservationsystem.exceptions.GlobalExceptionHandler
 * @author Maciej Janowski
 */
public class TableAlreadyReservedException extends RuntimeException {

    /**
     * Erstellt eine neue {@code TableAlreadyReservedException} für den angegebenen Tisch.
     *
     * @param tableNumber Nummer des Tisches, der bereits reserviert ist
     */
    public TableAlreadyReservedException(int tableNumber) {
        super("Tisch " + tableNumber + " ist im gewählten Zeitraum bereits reserviert.");
    }
}