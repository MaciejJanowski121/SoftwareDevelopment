package org.example.reservationsystem.DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Datenübertragungsobjekt (DTO) für Benutzeranmeldungen.
 *
 * <p>{@code UserLoginDTO} wird vom Frontend an den Server gesendet,
 * wenn sich ein Benutzer mit seiner E-Mail-Adresse und seinem Passwort anmeldet.
 * Beide Felder werden serverseitig validiert, bevor die Authentifizierung durchgeführt wird.</p>
 *
 * <p>Dieses DTO enthält ausschließlich Anmeldeinformationen und keine sensiblen
 * Benutzerdaten aus der {@code User}-Entität. Nach erfolgreicher Authentifizierung
 * wird ein {@link AuthUserDTO} zurückgegeben, und das Authentifizierungs-Cookie
 * wird im HTTP-Response gesetzt.</p>
 *
 * <p>Beispielhafte JSON-Anfrage:</p>
 * <pre>{@code
 * {
 *   "email": "user@example.com",
 *   "password": "geheimesPasswort123"
 * }
 * }</pre>
 *
 * <p><strong>Typische Verwendung im {@code AuthController}:</strong></p>
 * <pre>{@code
 * @PostMapping("/login")
 * public ResponseEntity<AuthUserDTO> login(
 *         @Valid @RequestBody UserLoginDTO dto,
 *         HttpServletResponse response
 * ) {
 *     String token = authService.login(dto);          // prüft Credentials und erstellt Token
 *     writeAuthCookie(response, token);               // setzt HttpOnly-Authentifizierungs-Cookie
 *
 *     User user = userRepository.findByEmail(dto.getEmail())
 *             .orElseThrow(() -> new IllegalStateException("User not found"));
 *
 *     return ResponseEntity.ok(AuthUserDTO.fromUser(user)); // gibt Benutzerdaten im sicheren DTO zurück
 * }
 * }</pre>
 *
 * @author Maciej Janowski
 */
public class UserLoginDTO {

    /** E-Mail-Adresse des Benutzers (Login-Identifier). */
    @NotBlank(message = "E-Mail darf nicht leer sein.")
    @Email(message = "Bitte geben Sie eine gültige E-Mail-Adresse ein.")
    private String email;

    /** Passwort des Benutzers im Klartext (wird serverseitig gehasht überprüft). */
    @NotBlank(message = "Passwort darf nicht leer sein.")
    private String password;

    /** Standardkonstruktor (erforderlich für Deserialisierung durch Jackson). */
    public UserLoginDTO() {}

    /** @return E-Mail-Adresse des Benutzers */
    public String getEmail() { return email; }

    public void setEmail(String v) { this.email = v; }

    /** @return eingegebenes Passwort */
    public String getPassword() { return password; }

    public void setPassword(String v) { this.password = v; }
}