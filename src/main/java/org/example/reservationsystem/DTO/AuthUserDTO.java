package org.example.reservationsystem.DTO;

import org.example.reservationsystem.model.User;

/**
 * Datenübertragungsobjekt (DTO) für authentifizierte Benutzer.
 *
 * <p>{@code AuthUserDTO} repräsentiert die wichtigsten Informationen
 * eines angemeldeten Benutzers, die an das Frontend zurückgegeben werden –
 * beispielsweise nach erfolgreicher Registrierung oder Anmeldung.</p>
 *
 * <p>Dieses DTO enthält keine sensiblen Daten wie Passwörter, sondern nur
 * öffentlich sichtbare Benutzerattribute wie E-Mail, Rolle, Name und Telefonnummer.</p>
 *
 * <p>Das Objekt wird typischerweise aus der {@link User}-Entität
 * über die statische Fabrikmethode {@link #fromUser(User)} erzeugt.</p>
 *
 * <p>Beispielhafte JSON-Antwort:</p>
 * <pre>{@code
 * {
 *   "email": "max.mustermann@example.com",
 *   "role": "ROLE_USER",
 *   "fullName": "Max Mustermann",
 *   "phone": "+49 176 12345678"
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
 *     String token = authService.login(dto);
 *     writeAuthCookie(response, token);
 *
 *     User user = userRepository.findByEmail(dto.getEmail())
 *             .orElseThrow(() -> new IllegalStateException("User not found"));
 *
 *     return ResponseEntity.ok(AuthUserDTO.fromUser(user));
 * }
 * }</pre>
 *
 * @author Maciej Janowski
 */
public record AuthUserDTO(
        String email,
        String role,
        String fullName,
        String phone
) {
    /**
     * Erstellt ein neues {@code AuthUserDTO} aus einer {@link User}-Entität.
     *
     * @param user die Benutzerentität aus der Datenbank
     * @return ein neues {@code AuthUserDTO} mit E-Mail, Rolle, Name und Telefonnummer
     */
    public static AuthUserDTO fromUser(User user) {
        return new AuthUserDTO(
                user.getEmail(),
                user.getRole().name(),
                user.getFullName(),
                user.getPhone()
        );
    }
}