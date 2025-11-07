package org.example.reservationsystem;

import io.jsonwebtoken.security.Keys;
import org.example.reservationsystem.JWTServices.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Komponententest für den {@link JwtService}.
 *
 * <p>Dieser Test überprüft die zentralen Funktionen des JWT-Services:
 * <ul>
 *   <li>Erzeugung eines Tokens mit korrekt gesetztem {@code Subject} (E-Mail-Adresse)</li>
 *   <li>Extraktion des Benutzernamens (Subject) aus dem Token</li>
 *   <li>Validierung gültiger Tokens</li>
 *   <li>Erkennung abgelaufener Tokens</li>
 *   <li>Umgang mit fehlerhaften oder manipulierten Tokens</li>
 * </ul>
 * </p>
 *
 * <p>Der geheime Schlüssel wird dynamisch per {@link ReflectionTestUtils} gesetzt,
 * um eine stabile Testumgebung ohne externe Konfiguration zu gewährleisten.</p>
 *
 * <p>Algorithmus: {@code HS256}, Ablaufzeit (Default): 3600000 ms = 1 h.</p>
 *
 * @author Maciej Janowski
 */
public class JwtServiceTest {

    private JwtService jwtService;

    /** Beispiel-Schlüssel im Base64-Format (zufällig generiert). */
    private final String testSecretBase64 = Base64.getEncoder()
            .encodeToString(Keys.secretKeyFor(io.jsonwebtoken.SignatureAlgorithm.HS256).getEncoded());

    /**
     * Initialisiert den {@link JwtService} vor jedem Test mit
     * einem zufälligen geheimen Schlüssel und 1 h Ablaufzeit.
     */
    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKeyBase64", testSecretBase64);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 3600000L);
    }

    /**
     * Prüft, dass ein Token erfolgreich erzeugt und der Benutzername (E-Mail)
     * korrekt aus dem Token extrahiert werden kann.
     */
    @Test
    void shouldGenerateTokenAndExtractEmailAsUsername() {
        UserDetails user = new User("maciej@example.com", "test-password", Collections.emptyList());

        String token = jwtService.generateToken(user);
        assertNotNull(token, "Das generierte Token darf nicht null sein.");

        String extracted = jwtService.extractUsername(token);
        assertEquals("maciej@example.com", extracted, "Das Subject (E-Mail) sollte korrekt extrahiert werden.");
    }

    /**
     * Prüft, dass ein frisch erzeugtes Token als gültig erkannt wird.
     */
    @Test
    void shouldValidateToken() {
        UserDetails user = new User("maciej@example.com", "test-password", Collections.emptyList());
        String token = jwtService.generateToken(user);

        assertTrue(jwtService.isTokenValid(token, user), "Das Token sollte gültig sein.");
    }

    /**
     * Prüft, dass ein abgelaufenes Token korrekt als ungültig erkannt wird.
     *
     * @throws InterruptedException falls {@link Thread#sleep(long)} unterbrochen wird
     */
    @Test
    void shouldDetectExpiredToken() throws InterruptedException {
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 1000L); // 1 s
        UserDetails user = new User("maciej@example.com", "test-password", Collections.emptyList());
        String token = jwtService.generateToken(user);

        Thread.sleep(2000);
        assertFalse(jwtService.isTokenValid(token, user), "Abgelaufenes Token sollte ungültig sein.");
    }

    /**
     * Prüft, dass ein syntaktisch ungültiges Token (falsches Format)
     * als ungültig erkannt wird.
     */
    @Test
    void shouldReturnFalseForInvalidToken() {
        UserDetails user = new User("maciej@example.com", "test-password", Collections.emptyList());
        assertFalse(jwtService.isTokenValid("invalid.token.value", user),
                "Manipuliertes oder ungültiges Token sollte verworfen werden.");
    }
}