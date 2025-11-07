package org.example.reservationsystem;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockCookie;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void shouldRegisterLoginAndCheckAuthSuccessfully() throws Exception {
        String email = "maciej_" + System.currentTimeMillis() + "@example.com";
        String password = "test123";

        // --- REJESTRACJA ---
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
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("ROLE_USER"))
                .andExpect(jsonPath("$.fullName").value("Maciej Janowski"))
                .andExpect(jsonPath("$.phone").value("+49 170 0000000"))
                .andReturn();

        // wyciągamy cookie z rejestracji (jeśli chcesz użyć dalej)
        Cookie regCookie = reg.getResponse().getCookie("token");

        // --- LOGOWANIE ---
        Map<String, Object> loginDto = Map.of(
                "email", email,
                "password", password
        );

        MvcResult login = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDto)))
                .andExpect(status().isOk())
                .andExpect(header().exists("Set-Cookie"))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("ROLE_USER"))
                .andExpect(jsonPath("$.fullName").exists())
                .andExpect(jsonPath("$.phone").exists())
                .andReturn();

        // bierzemy świeży token z loginu
        Cookie loginCookie = login.getResponse().getCookie("token");
        assertNotNull(loginCookie);

        // --- AUTH CHECK ---
        mockMvc.perform(get("/auth/auth_check")
                        .cookie(loginCookie)) // przekazujemy token w cookie
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("ROLE_USER"))
                .andExpect(jsonPath("$.fullName").exists())
                .andExpect(jsonPath("$.phone").exists());
    }
}