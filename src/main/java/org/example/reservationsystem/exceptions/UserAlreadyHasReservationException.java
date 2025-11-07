package org.example.reservationsystem.exceptions;

/**
 * Wird ausgelöst, wenn ein Benutzer versucht, eine neue Reservierung anzulegen,
 * obwohl bereits eine aktive Reservierung für ihn existiert.
 *
 * <p>Diese Ausnahme stellt eine Geschäftsregelverletzung dar (1:1-Beziehung
 * zwischen Benutzer und Reservierung).</p>
 *
 * <p>Im {@link org.example.reservationsystem.exceptions.GlobalExceptionHandler}
 * wird dieser Fehler zu einem HTTP-Status {@code 409 Conflict} mit Typ
 * {@code /errors/user-has-reservation} gemappt.</p>
 *
 * @see org.example.reservationsystem.exceptions.GlobalExceptionHandler
 * @author Maciej Janowski
 */
public class UserAlreadyHasReservationException extends RuntimeException {

    /**
     * Erstellt eine neue {@code UserAlreadyHasReservationException}
     * mit einer vordefinierten Fehlermeldung.
     */
    public UserAlreadyHasReservationException() {
        super("Du hast bereits eine aktive Reservierung.");
    }
}