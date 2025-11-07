package org.example.reservationsystem.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.example.reservationsystem.DTO.AuthUserDTO;
import org.example.reservationsystem.DTO.UserLoginDTO;
import org.example.reservationsystem.DTO.UserRegisterDTO;
import org.example.reservationsystem.JWTServices.JwtService;
import org.example.reservationsystem.model.User;
import org.example.reservationsystem.repository.UserRepository;
import org.example.reservationsystem.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

/**
 * REST-Controller für Authentifizierungs- und Sitzungsverwaltung.
 *
 * <p>Stellt Endpunkte für Registrierung, Anmeldung, Session-Check und Logout bereit.
 * Nach erfolgreichem Login/Registrierung wird ein JWT erzeugt und als
 * {@code HttpOnly}-Cookie (SameSite=Lax) gesetzt. Der Login-Identifier ist die E-Mail-Adresse.</p>
 *
 * <p><strong>Basis-URL:</strong> {@code /auth}</p>
 *
 * <p>CORS ist für {@code http://localhost:3000} mit {@code allowCredentials=true} aktiviert,
 * um Cookie-basierte Authentifizierung mit einem React-Frontend zu ermöglichen.</p>
 *
 * <ul>
 *   <li>{@code POST /auth/register} – Registrierung, setzt Auth-Cookie, gibt {@link AuthUserDTO} zurück.</li>
 *   <li>{@code POST /auth/login} – Anmeldung, setzt Auth-Cookie, gibt {@link AuthUserDTO} zurück.</li>
 *   <li>{@code GET  /auth/auth_check} – prüft Token (Cookie/Bearer) und gibt {@link AuthUserDTO} zurück, sonst 401.</li>
 *   <li>{@code POST /auth/logout} – löscht Auth-Cookie.</li>
 * </ul>
 *
 * @author Maciej Janowski
 */
@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    /**
     * Erstellt einen neuen {@code AuthController}.
     *
     * @param authService    Service für Registrierung/Anmeldung (JWT-Erzeugung)
     * @param jwtService     Service für JWT-Operationen (Subject = E-Mail)
     * @param userRepository Repository zum Laden des angemeldeten Benutzers
     */
    public AuthController(AuthService authService,
                          JwtService jwtService,
                          UserRepository userRepository) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    /**
     * Schreibt das Authentifizierungs-JWT in ein {@code HttpOnly}-Cookie.
     *
     * <p>Cookie-Eigenschaften: {@code HttpOnly=true}, {@code SameSite=Lax}, {@code path=/},
     * {@code secure=false} (für lokale Entwicklung), {@code maxAge} gemäß JWT-Ablaufzeit.</p>
     *
     * @param response HTTP-Antwort
     * @param token    signiertes JWT
     */
    private void writeAuthCookie(HttpServletResponse response, String token) {
        long maxAgeSeconds = Duration.ofMillis(jwtService.getExpirationTime()).getSeconds();
        ResponseCookie cookie = ResponseCookie.from("token", token)
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAgeSeconds)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    /**
     * Extrahiert ein JWT aus dem Request.
     *
     * <p>Prüft zuerst das Cookie {@code token}; falls nicht vorhanden, wird der
     * {@code Authorization}-Header im Format {@code Bearer &lt;token&gt;} ausgewertet.</p>
     *
     * @param request HTTP-Request
     * @return JWT-String oder {@code null}, wenn nicht vorhanden
     */
    private String extractToken(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (var c : request.getCookies()) {
                if ("token".equals(c.getName())) {
                    return c.getValue();
                }
            }
        }
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            return auth.substring(7);
        }
        return null;
    }

    /**
     * Registriert einen neuen Benutzer und setzt das Authentifizierungs-Cookie.
     *
     * @param dto      Registrierungsdaten (E-Mail, Passwort, Name, optional Telefon)
     * @param response HTTP-Antwort (zum Setzen des Cookies)
     * @return {@link AuthUserDTO} des neu registrierten Benutzers
     * @throws IllegalStateException wenn der Benutzer nach der Registrierung nicht geladen werden kann
     */
    @PostMapping("/register")
    public ResponseEntity<AuthUserDTO> register(
            @Valid @RequestBody UserRegisterDTO dto,
            HttpServletResponse response
    ) {
        String token = authService.register(dto);
        writeAuthCookie(response, token);

        User saved = userRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new IllegalStateException("User not found after registration"));

        return ResponseEntity.ok(AuthUserDTO.fromUser(saved));
    }

    /**
     * Meldet einen Benutzer an und setzt das Authentifizierungs-Cookie.
     *
     * @param dto      Logindaten (E-Mail, Passwort)
     * @param response HTTP-Antwort (zum Setzen des Cookies)
     * @return {@link AuthUserDTO} des angemeldeten Benutzers
     * @throws IllegalStateException wenn der Benutzer nach dem Login nicht geladen werden kann
     */
    @PostMapping("/login")
    public ResponseEntity<AuthUserDTO> login(
            @Valid @RequestBody UserLoginDTO dto,
            HttpServletResponse response
    ) {
        String token = authService.login(dto);
        writeAuthCookie(response, token);

        User user = userRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new IllegalStateException("User not found"));

        return ResponseEntity.ok(AuthUserDTO.fromUser(user));
    }

    /**
     * Prüft, ob ein gültiges JWT vorhanden ist, und gibt die Benutzerdaten zurück.
     *
     * <p>Das Token wird aus dem {@code HttpOnly}-Cookie oder dem
     * {@code Authorization}-Header extrahiert. Bei fehlendem/leerem Token
     * wird {@code 401 Unauthorized} geliefert.</p>
     *
     * @param request HTTP-Request (zur Token-Extraktion)
     * @return {@link AuthUserDTO} bei gültigem Token; sonst {@code 401} mit Fehlermeldung
     */
    @GetMapping("/auth_check")
    public ResponseEntity<?> checkAuth(HttpServletRequest request) {
        String token = extractToken(request);
        if (token == null || token.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid Token");
        }

        String email = jwtService.getUsername(token);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("User not found"));

        return ResponseEntity.ok(AuthUserDTO.fromUser(user));
    }

    /**
     * Meldet den Benutzer ab, indem das Authentifizierungs-Cookie gelöscht wird.
     *
     * @param response HTTP-Antwort (zum Setzen eines ablaufenden Cookies)
     * @return {@code 200 OK} nach erfolgreichem Logout
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("token", "")
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
        return ResponseEntity.ok().build();
    }
}