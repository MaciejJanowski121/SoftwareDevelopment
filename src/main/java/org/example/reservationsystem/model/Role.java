package org.example.reservationsystem.model;

/**
 * Rollen, die einem {@link User} im System zugewiesen werden können.
 *
 * <p>Das Enum {@code Role} definiert die Berechtigungsstufen innerhalb der Anwendung.
 * Die Zuordnung erfolgt pro Benutzerkonto, das sich über seine eindeutige E-Mail-Adresse anmeldet.</p>
 *
 * <p>Die Rollen werden von Spring Security verwendet, um den Zugriff auf bestimmte
 * Ressourcen (z. B. REST-Endpunkte oder Administratorbereiche im Frontend) zu steuern.</p>
 *
 * <ul>
 *   <li>{@link #ROLE_USER} – Standardrolle für registrierte Benutzer,
 *       die Reservierungen erstellen, anzeigen oder löschen können.</li>
 *   <li>{@link #ROLE_ADMIN} – Administratorrolle mit erweiterten Berechtigungen,
 *       z. B. Einsicht in alle Reservierungen, Verwaltung von Tischen oder Benutzerkonten.</li>
 * </ul>
 *
 * <p>Beispiel für die Verwendung in {@link User}:</p>
 * <pre>{@code
 * User admin = new User("passwort123", Role.ROLE_ADMIN,
 *     "Max Mustermann", "admin@example.com", "+49 176 12345678");
 *
 * // Zugriffskontrolle über Spring Security:
 * @PreAuthorize("hasRole('ADMIN')")
 * public void deleteReservation(Long id) { ... }
 * }</pre>
 *
 *
 * @author Maciej Janowski
 */
public enum Role {

    /** Standardrolle für normale Benutzer, die sich mit ihrer E-Mail-Adresse anmelden. */
    ROLE_USER,

    /** Administratorrolle mit erweiterten System- und Verwaltungsrechten. */
    ROLE_ADMIN
}