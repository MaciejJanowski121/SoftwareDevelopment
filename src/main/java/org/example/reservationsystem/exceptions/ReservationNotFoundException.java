package org.example.reservationsystem.exceptions;

/**
 * Wird ausgelöst, wenn eine angeforderte Reservierung nicht gefunden wurde.
 *
 * <p>Dies tritt typischerweise auf, wenn eine ungültige ID übergeben wird oder
 * der Benutzer versucht, auf eine nicht vorhandene Reservierung zuzugreifen.</p>
 *
 * <p>Im {@link org.example.reservationsystem.exceptions.GlobalExceptionHandler}
 * wird dieser Fehler zu einem HTTP-Status {@code 404 Not Found} mit Typ
 * {@code /errors/reservation-not-found} gemappt.</p>
 *
 * @see org.example.reservationsystem.exceptions.GlobalExceptionHandler
 * @author Maciej Janowski
 */
public class ReservationNotFoundException extends RuntimeException {

    /**
     * Erstellt eine neue {@code ReservationNotFoundException} mit der angegebenen Fehlermeldung.
     *
     * @param message Beschreibung des Fehlers (z. B. "Reservation not found.")
     */
    public ReservationNotFoundException(String message) {
        super(message);
    }
}