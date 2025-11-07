package org.example.reservationsystem.exceptions;

/**
 * Wird ausgelöst, wenn ein angeforderter Tisch nicht existiert oder nicht gefunden wurde.
 *
 * <p>Dieser Fehler tritt typischerweise auf, wenn eine ungültige Tischnummer
 * übergeben wird oder der entsprechende Datensatz in der Datenbank fehlt.</p>
 *
 * <p>Im {@link org.example.reservationsystem.exceptions.GlobalExceptionHandler}
 * wird dieser Fehler zu einem HTTP-Status {@code 404 Not Found} mit Typ
 * {@code /errors/not-found} gemappt.</p>
 *
 * @see org.example.reservationsystem.exceptions.GlobalExceptionHandler
 * @author Maciej Janowski
 */
public class TableNotFoundException extends RuntimeException {

    /**
     * Erstellt eine neue {@code TableNotFoundException} mit der angegebenen Fehlermeldung.
     *
     * @param message Beschreibung des Fehlers (z. B. "Table with number 5 does not exist.")
     */
    public TableNotFoundException(String message) {
        super(message);
    }
}