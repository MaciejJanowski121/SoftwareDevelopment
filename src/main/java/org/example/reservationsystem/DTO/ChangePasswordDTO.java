package org.example.reservationsystem.DTO;

/**
 * Datenübertragungsobjekt (DTO) für Passwortänderungen.
 *
 * <p>Dieses Objekt wird verwendet, wenn ein angemeldeter Benutzer sein Passwort ändern möchte.
 * Es enthält das bisherige Passwort zur Verifizierung und das neue Passwort,
 * das anschließend serverseitig gehasht und gespeichert wird.</p>
 *
 * <p>Sicherheits-Hinweis: Die Felder in diesem DTO sollten niemals geloggt oder serialisiert
 * werden, da sie Klartext-Passwörter enthalten.</p>
 *
 * <p><strong>Typische Verwendung im {@code UserController}:</strong></p>
 * <pre>{@code
 * @PutMapping("/change-password")
 * public ResponseEntity<String> changePassword(
 *         @AuthenticationPrincipal User currentUser,
 *         @RequestBody ChangePasswordDTO changePasswordDTO
 * ) {
 *     if (currentUser == null) {
 *         return ResponseEntity.status(401).build();
 *     }
 *     userService.changePassword(currentUser.getUsername(), changePasswordDTO);
 *     return ResponseEntity.ok("Password changed successfully.");
 * }
 * }</pre>
 *
 * @author Maciej Janowski
 */
public class ChangePasswordDTO {

    /** Das bisherige (aktuelle) Passwort des Benutzers. */
    private String oldPassword;

    /** Das neue Passwort, das gesetzt werden soll. */
    private String newPassword;

    /** Standardkonstruktor (erforderlich für Deserialisierung durch Jackson). */
    public ChangePasswordDTO() {}

    /**
     * Erstellt ein neues {@code ChangePasswordDTO} mit beiden Passwortwerten.
     *
     * @param oldPassword das bisherige Passwort (Klartext)
     * @param newPassword das neue Passwort (Klartext)
     */
    public ChangePasswordDTO(String oldPassword, String newPassword) {
        this.oldPassword = oldPassword;
        this.newPassword = newPassword;
    }

    /** @return das bisherige Passwort */
    public String getOldPassword() { return oldPassword; }
    public void setOldPassword(String oldPassword) { this.oldPassword = oldPassword; }

    /** @return das neue Passwort */
    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}