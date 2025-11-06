package org.example.reservationsystem;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockCookie;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
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

        // --- REJESTRACJA (bez username) ---
        Map<String, Object> registerDto = Map.of(
                "fullName", "Maciej Janowski",
                "email", email,
                "phone", "+49 170 0000000",
                "password", password
        );

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerDto)))
                .andExpect(status().isOk())
                .andExpect(header().exists("Set-Cookie"))
                // akceptujemy: backend może zwrócić username==email lub wcale
                .andExpect(jsonPath("$.username", anyOf(is(email), nullValue())))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("ROLE_USER"));

        // --- LOGOWANIE (email + password) ---
        Map<String, Object> loginDto = Map.of(
                "email", email,
                "password", password
        );

        var loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDto)))
                .andExpect(status().isOk())
                .andExpect(header().exists("Set-Cookie"))
                .andExpect(jsonPath("$.username", anyOf(is(email), nullValue())))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("ROLE_USER"))
                .andReturn();

        // token z Set-Cookie
        String setCookie = loginResult.getResponse().getHeader("Set-Cookie");
        String token = setCookie.split("token=")[1].split(";")[0];

        // --- AUTH CHECK ---
        mockMvc.perform(get("/auth/auth_check")
                        .cookie(new MockCookie("token", token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", anyOf(is(email), nullValue())))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("ROLE_USER"));
    }
}