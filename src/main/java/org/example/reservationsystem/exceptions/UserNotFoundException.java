package org.example.reservationsystem.exceptions;

/**
 * Wird ausgelöst, wenn ein Benutzer mit der angegebenen E-Mail-Adresse
 * nicht gefunden wurde.
 *
 * <p>Diese Ausnahme tritt typischerweise bei Authentifizierungs- oder
 * Reservierungsvorgängen auf, wenn die E-Mail-Adresse nicht existiert
 * oder der Benutzer aus der Datenbank gelöscht wurde.</p>
 *
 * <p>Im {@link org.example.reservationsystem.exceptions.GlobalExceptionHandler}
 * wird dieser Fehler zu einem HTTP-Status {@code 404 Not Found} mit Typ
 * {@code /errors/not-found} gemappt.</p>
 *
 * @see org.example.reservationsystem.exceptions.GlobalExceptionHandler
 * @author Maciej Janowski
 */
public class UserNotFoundException extends RuntimeException {

    /**
     * Erstellt eine neue {@code UserNotFoundException} mit der angegebenen Fehlermeldung.
     *
     * @param message Beschreibung des Fehlers (z. B. "User not found")
     */
    public UserNotFoundException(String message) {
        super(message);
    }
}