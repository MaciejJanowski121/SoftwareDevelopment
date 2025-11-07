package org.example.reservationsystem.exceptions;

/**
 * Wird ausgelöst, wenn eine Geschäftsregel innerhalb des Domain-Modells verletzt wird.
 *
 * <p>Beispiele:
 * <ul>
 *   <li>Ein Benutzer versucht, eine zweite Reservierung anzulegen, obwohl nur eine erlaubt ist.</li>
 *   <li>Eine Reservierung überschneidet sich zeitlich mit einer bestehenden.</li>
 * </ul>
 * </p>
 *
 * <p>Wird im {@link org.example.reservationsystem.exceptions.GlobalExceptionHandler}
 * zu einem HTTP-Status {@code 400 Bad Request} mit Typ
 * {@code /errors/business-rule} gemappt.</p>
 *
 * @see org.example.reservationsystem.exceptions.GlobalExceptionHandler
 * @author Maciej Janowski
 */
public class BusinessRuleViolationException extends RuntimeException {

    /**
     * Erstellt eine neue {@code BusinessRuleViolationException} mit der angegebenen Nachricht.
     *
     * @param message Beschreibung der Regelverletzung
     */
    public BusinessRuleViolationException(String message) {
        super(message);
    }
}