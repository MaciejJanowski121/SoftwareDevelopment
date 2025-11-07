package org.example.reservationsystem.DTO;

/**
 * Datenübertragungsobjekt (DTO) zur Anzeige oder Aktualisierung
 * von Benutzerprofildaten.
 *
 * <p>{@code UserProfileDTO} enthält grundlegende persönliche Informationen
 * eines Benutzers, die im Frontend angezeigt oder geändert werden können.
 * Es werden keine sensiblen Daten wie Passwort oder Rolleninformationen übertragen.</p>
 *
 * <p>Beispielhafte JSON-Antwort (GET /user/me):</p>
 * <pre>{@code
 * {
 *   "fullName": "Max Mustermann",
 *   "email": "max.mustermann@example.com",
 *   "phone": "+49 176 12345678"
 * }
 * }</pre>
 *
 * <p>Beispielhafte JSON-Anfrage (PUT /user/me):</p>
 * <pre>{@code
 * {
 *   "fullName": "Maximilian Mustermann",
 *   "email": "max.m@example.com",
 *   "phone": "+49 151 987654321"
 * }
 * }</pre>
 *
 * <p><strong>Typische Verwendung im {@code UserController}:</strong></p>
 * <pre>{@code
 * @GetMapping("/user/me")
 * public ResponseEntity<UserProfileDTO> me(@AuthenticationPrincipal User currentUser) {
 *     if (currentUser == null) return ResponseEntity.status(401).build();
 *     return ResponseEntity.ok(userService.getProfile(currentUser.getUsername()));
 * }
 *
 * @PutMapping("/user/me")
 * public ResponseEntity<Void> updateMe(@AuthenticationPrincipal User currentUser,
 *                                      @RequestBody UserProfileDTO dto) {
 *     if (currentUser == null) return ResponseEntity.status(401).build();
 *     userService.updateProfile(currentUser.getUsername(), dto);
 *     return ResponseEntity.noContent().build();
 * }
 * }</pre>
 *
 * <p>Hinweis: In diesem Projekt dient die E-Mail-Adresse als Login-Identifier
 * (siehe {@code User.getUsername()}).</p>
 *
 * @author Maciej Janowski
 */
public class UserProfileDTO {

    /** Vollständiger Name des Benutzers. */
    private String fullName;

    /** E-Mail-Adresse des Benutzers (Login-Identifier). */
    private String email;

    /** Optionale Telefonnummer des Benutzers. */
    private String phone;

    /** @return vollständiger Name des Benutzers */
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    /** @return E-Mail-Adresse des Benutzers */
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    /** @return Telefonnummer (optional) */
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}