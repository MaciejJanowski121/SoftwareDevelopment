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

public class JwtServiceTest {

    private JwtService jwtService;

    private final String testSecretBase64 = Base64.getEncoder()
            .encodeToString(Keys.secretKeyFor(io.jsonwebtoken.SignatureAlgorithm.HS256).getEncoded());

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKeyBase64", testSecretBase64);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 3600000L);
    }

    @Test
    void shouldGenerateTokenAndExtractEmailAsUsername() {
        UserDetails user = new User("maciej@example.com", "test-password", Collections.emptyList());

        String token = jwtService.generateToken(user);
        assertNotNull(token);

        String extracted = jwtService.extractUsername(token);
        assertEquals("maciej@example.com", extracted);
    }

    @Test
    void shouldValidateToken() {
        UserDetails user = new User("maciej@example.com", "test-password", Collections.emptyList());
        String token = jwtService.generateToken(user);

        assertTrue(jwtService.isTokenValid(token, user));
    }

    @Test
    void shouldDetectExpiredToken() throws InterruptedException {
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 1000L);
        UserDetails user = new User("maciej@example.com", "test-password", Collections.emptyList());
        String token = jwtService.generateToken(user);

        Thread.sleep(2000);
        assertFalse(jwtService.isTokenValid(token, user));
    }

    @Test
    void shouldReturnFalseForInvalidToken() {
        UserDetails user = new User("maciej@example.com", "test-password", Collections.emptyList());
        assertFalse(jwtService.isTokenValid("invalid.token.value", user));
    }
}