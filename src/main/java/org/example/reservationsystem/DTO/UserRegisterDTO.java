package org.example.reservationsystem.DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datenübertragungsobjekt (DTO) für die Benutzerregistrierung.
 *
 * <p>{@code UserRegisterDTO} wird vom Frontend an den Server gesendet,
 * wenn ein neuer Benutzer ein Konto erstellt. Es enthält die erforderlichen
 * Felder für die Erstellung eines neuen Benutzerkontos, einschließlich
 * E-Mail-Adresse, Passwort und vollständigem Namen.</p>
 *
 * <p>Die Validierung erfolgt serverseitig über Jakarta Bean Validation
 * (z. B. {@code @NotBlank}, {@code @Email}, {@code @Size}).</p>
 *
 * <p>Nach erfolgreicher Registrierung wird in der Regel ein {@link AuthUserDTO}
 * als Antwort zurückgegeben und ein JWT-Token im HTTP-Cookie gespeichert.</p>
 *
 * <p>Beispielhafte JSON-Anfrage:</p>
 * <pre>{@code
 * {
 *   "email": "max.mustermann@example.com",
 *   "password": "geheimesPasswort123",
 *   "fullName": "Max Mustermann",
 *   "phone": "+49 176 12345678"
 * }
 * }</pre>
 *
 * <p><strong>Typische Verwendung im {@code AuthController}:</strong></p>
 * <pre>{@code
 * @PostMapping("/register")
 * public ResponseEntity<AuthUserDTO> register(
 *         @Valid @RequestBody UserRegisterDTO dto,
 *         HttpServletResponse response
 * ) {
 *     String token = authService.register(dto);
 *     writeAuthCookie(response, token);
 *
 *     User saved = userRepository.findByEmail(dto.getEmail())
 *             .orElseThrow(() -> new IllegalStateException("User not found after registration"));
 *
 *     return ResponseEntity.ok(AuthUserDTO.fromUser(saved));
 * }
 * }</pre>
 *
 * @author Maciej Janowski
 */
public class UserRegisterDTO {

    /** E-Mail-Adresse des Benutzers (Login-Identifier). */
    @NotBlank(message = "E-Mail darf nicht leer sein.")
    @Email(message = "Bitte geben Sie eine gültige E-Mail-Adresse ein.")
    private String email;

    /** Passwort im Klartext, das serverseitig gehasht gespeichert wird. */
    @NotBlank(message = "Passwort darf nicht leer sein.")
    @Size(min = 6, message = "Das Passwort muss mindestens 6 Zeichen lang sein.")
    private String password;

    /** Vollständiger Name des Benutzers. */
    @NotBlank(message = "Vollständiger Name ist erforderlich.")
    private String fullName;

    /** Optionale Telefonnummer (kann leer bleiben). */
    private String phone;

    /** Standardkonstruktor (erforderlich für Deserialisierung durch Jackson). */
    public UserRegisterDTO() {}

    /** @return E-Mail-Adresse des neuen Benutzers */
    public String getEmail() { return email; }
    public void setEmail(String v) { this.email = v; }

    /** @return Klartext-Passwort (wird serverseitig gehasht) */
    public String getPassword() { return password; }
    public void setPassword(String v) { this.password = v; }

    /** @return vollständiger Name des Benutzers */
    public String getFullName() { return fullName; }
    public void setFullName(String v) { this.fullName = v; }

    /** @return Telefonnummer (optional) */
    public String getPhone() { return phone; }
    public void setPhone(String v) { this.phone = v; }
}