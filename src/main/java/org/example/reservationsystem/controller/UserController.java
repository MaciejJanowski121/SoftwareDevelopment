package org.example.reservationsystem.controller;

import org.example.reservationsystem.DTO.ChangePasswordDTO;
import org.example.reservationsystem.DTO.UserProfileDTO;
import org.example.reservationsystem.model.User;
import org.example.reservationsystem.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * REST-Controller für Benutzerkontenverwaltung.
 *
 * <p>Stellt Endpunkte für das Abrufen und Aktualisieren des Benutzerprofils
 * sowie für die Änderung des Passworts bereit. Alle Operationen setzen voraus,
 * dass der Benutzer authentifiziert ist (JWT-Authentifizierung über SecurityContext).</p>
 *
 * <p><strong>Basis-URL:</strong> {@code /user}</p>
 *
 * <p>CORS ist für {@code http://localhost:3000} aktiviert, um
 * den Zugriff aus dem React-Frontend mit Cookies zu ermöglichen.</p>
 *
 * <ul>
 *   <li>{@code GET /user/me} – liefert das Profil des aktuell eingeloggten Benutzers.</li>
 *   <li>{@code PUT /user/me} – aktualisiert Profildaten (Name, E-Mail, Telefonnummer).</li>
 *   <li>{@code PUT /user/change-password} – ändert das Passwort nach Validierung des alten Passworts.</li>
 * </ul>
 *
 * @author Maciej Janowski
 */
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
@RestController
@RequestMapping("/user")
public class UserController {

    private final UserService userService;

    /**
     * Erstellt einen neuen {@code UserController}.
     *
     * @param userService Service für Benutzerverwaltung und Passwortänderung
     */
    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Gibt das Profil des aktuell authentifizierten Benutzers zurück.
     *
     * <p>Die Benutzerdaten werden aus dem Security-Kontext entnommen
     * ({@link AuthenticationPrincipal}) und über den {@link UserService}
     * in ein {@link UserProfileDTO} umgewandelt.</p>
     *
     * @param currentUser aktuell eingeloggter Benutzer (aus SecurityContext)
     * @return {@link UserProfileDTO} mit Profildaten oder {@code 401 Unauthorized}, wenn nicht authentifiziert
     */
    @GetMapping("/me")
    public ResponseEntity<UserProfileDTO> me(@AuthenticationPrincipal User currentUser) {
        if (currentUser == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(userService.getProfile(currentUser.getUsername()));
    }

    /**
     * Aktualisiert die Profildaten (vollständiger Name, E-Mail-Adresse, Telefonnummer)
     * des aktuell eingeloggten Benutzers.
     *
     * <p>Leere oder ungültige Felder werden ignoriert. Änderungen werden
     * in der Datenbank über {@link UserService#updateProfile(String, UserProfileDTO)} gespeichert.</p>
     *
     * @param currentUser aktuell eingeloggter Benutzer (aus SecurityContext)
     * @param dto         neues Profilobjekt mit optionalen Änderungen
     * @return {@code 204 No Content} bei erfolgreicher Aktualisierung oder {@code 401 Unauthorized}, wenn nicht authentifiziert
     */
    @PutMapping("/me")
    public ResponseEntity<Void> updateMe(@AuthenticationPrincipal User currentUser,
                                         @RequestBody UserProfileDTO dto) {
        if (currentUser == null) {
            return ResponseEntity.status(401).build();
        }
        userService.updateProfile(currentUser.getUsername(), dto);
        return ResponseEntity.noContent().build();
    }

    /**
     * Ändert das Passwort des eingeloggten Benutzers.
     *
     * <p>Das alte Passwort wird mit dem gespeicherten Hash verglichen.
     * Wenn es korrekt ist, wird das neue Passwort mit BCrypt gehasht
     * und gespeichert.</p>
     *
     * <p>Validierungsregeln:
     * <ul>
     *   <li>Altes Passwort muss korrekt sein</li>
     *   <li>Neues Passwort muss mindestens 6 Zeichen lang sein</li>
     * </ul>
     * </p>
     *
     * @param currentUser        aktuell eingeloggter Benutzer (aus SecurityContext)
     * @param changePasswordDTO  DTO mit altem und neuem Passwort
     * @return {@code 200 OK} mit Bestätigungstext oder {@code 401 Unauthorized}, wenn nicht authentifiziert
     */
    @PutMapping("/change-password")
    public ResponseEntity<String> changePassword(@AuthenticationPrincipal User currentUser,
                                                 @RequestBody ChangePasswordDTO changePasswordDTO) {
        if (currentUser == null) {
            return ResponseEntity.status(401).build();
        }
        userService.changePassword(currentUser.getUsername(), changePasswordDTO);
        return ResponseEntity.ok("Password changed successfully.");
    }
}