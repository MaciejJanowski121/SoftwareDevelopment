package org.example.reservationsystem;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * **Integrations test** für die Authentifizierungs-Endpunkte des Backends.
 *
 * <p>Dieser Test fährt den Spring-Kontext hoch und prüft den vollständigen Flow
 * mit echter Web-Schicht (MockMvc):</p>
 *
 * <ol>
 *   <li><b>Registrierung</b> über {@code POST /auth/register} – erwartet: 200, gesetztes HttpOnly-Cookie, korrektes {@code AuthUserDTO}.</li>
 *   <li><b>Login</b> über {@code POST /auth/login} – erwartet: 200, neues HttpOnly-Cookie, korrektes {@code AuthUserDTO}.</li>
 *   <li><b>Auth-Check</b> über {@code GET /auth/auth_check} mit Cookie – erwartet: 200, DTO des eingeloggten Benutzers.</li>
 *   <li><b>Negativ</b>: Login mit ungültigen Credentials → 401 + ProblemDetail.</li>
 *   <li><b>Negativ</b>: Logout setzt ablaufendes Cookie (Max-Age=0) → 200.</li>
 * </ol>
 *
 * <p>Der Test nutzt das aktive Profil {@code test} und überprüft zusätzlich
 * sicherheitsrelevante Cookie-Attribute (HttpOnly, SameSite=Lax).</p>
 *
 * @author Maciej Janowski
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    /**
     * Vollständiger Happy-Path: Registrieren → Einloggen → Auth prüfen.
     *
     * @throws Exception bei HTTP-/Serialisierungsfehlern
     */
    @Test
    void shouldRegisterLoginAndCheckAuthSuccessfully() throws Exception {
        String email = "maciej_" + System.currentTimeMillis() + "@example.com";
        String password = "test123";


        Map<String, Object> registerDto = Map.of(
                "fullName", "Maciej Janowski",
                "email", email,
                "phone", "+49 170 0000000",
                "password", password
        );

        MvcResult reg = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerDto)))
                .andExpect(status().isOk())
                .andExpect(header().exists("Set-Cookie"))
                .andExpect(header().string("Set-Cookie", containsString("HttpOnly")))
                .andExpect(header().string("Set-Cookie", containsString("SameSite=Lax")))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("ROLE_USER"))
                .andExpect(jsonPath("$.fullName").value("Maciej Janowski"))
                .andExpect(jsonPath("$.phone").value("+49 170 0000000"))
                .andReturn();


        Cookie regCookie = reg.getResponse().getCookie("token");


        Map<String, Object> loginDto = Map.of(
                "email", email,
                "password", password
        );

        MvcResult login = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDto)))
                .andExpect(status().isOk())
                .andExpect(header().exists("Set-Cookie"))
                .andExpect(header().string("Set-Cookie", containsString("HttpOnly")))
                .andExpect(header().string("Set-Cookie", containsString("SameSite=Lax")))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("ROLE_USER"))
                .andExpect(jsonPath("$.fullName").exists())
                .andExpect(jsonPath("$.phone").exists())
                .andReturn();


        Cookie loginCookie = login.getResponse().getCookie("token");
        assertNotNull(loginCookie);


        mockMvc.perform(get("/auth/auth_check").cookie(loginCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("ROLE_USER"))
                .andExpect(jsonPath("$.fullName").exists())
                .andExpect(jsonPath("$.phone").exists());
    }

    /**
     * Negativer Integrationsfall: Login mit ungültigen Zugangsdaten
     * führt zu {@code 401 Unauthorized} und gibt ein standardisiertes {@code ProblemDetail} zurück.
     *
     * @throws Exception bei HTTP-/Serialisierungsfehlern
     */
    @Test
    void login_withInvalidCredentials_returns401ProblemDetail() throws Exception {
        Map<String, Object> loginDto = Map.of(
                "email", "does-not-exist+" + System.currentTimeMillis() + "@example.com",
                "password", "wrong"
        );

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDto)))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("Set-Cookie"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.detail").value("E-Mail oder Passwort ist falsch."));
    }

    /**
     * Integrationsfall: Logout setzt ein sofort ablaufendes Cookie ({@code Max-Age=0})
     * und liefert {@code 200 OK}.
     *
     * @throws Exception bei HTTP-Fehlern
     */
    @Test
    void logout_setsExpiredCookie() throws Exception {
        mockMvc.perform(post("/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Set-Cookie"))
                .andExpect(header().string("Set-Cookie",
                        allOf(
                                containsString("token="),
                                containsString("Max-Age=0")
                        )));
    }
}