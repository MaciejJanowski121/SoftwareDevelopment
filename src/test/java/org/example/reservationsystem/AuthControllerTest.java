package org.example.reservationsystem;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.reservationsystem.DTO.AuthUserDTO;
import org.example.reservationsystem.DTO.UserLoginDTO;
import org.example.reservationsystem.DTO.UserRegisterDTO;
import org.example.reservationsystem.JWTServices.JwtService;
import org.example.reservationsystem.controller.AuthController;
import org.example.reservationsystem.model.Role;
import org.example.reservationsystem.model.User;
import org.example.reservationsystem.repository.UserRepository;
import org.example.reservationsystem.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class AuthControllerTest {

    @Mock private AuthService authService;
    @Mock private JwtService jwtService;
    @Mock private UserRepository userRepository;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;

    @InjectMocks
    private AuthController authController;

    AutoCloseable mocks;

    @BeforeEach
    void setup() {
        mocks = MockitoAnnotations.openMocks(this);
    }

    // -------- register --------

    @Test
    void register_returnsAuthUserAndSetsCookie_onSuccess() {
        UserRegisterDTO dto = new UserRegisterDTO();
        // frontend wysyła email jako username (dla zgodności ze starym backendem), ale kluczowe jest email
        dto.setEmail("maciej@example.com");
        dto.setPassword("test123");
        dto.setFullName("Maciej");
        dto.setEmail("maciej@example.com");
        dto.setPhone("+49 111 222");

        when(authService.register(dto)).thenReturn("jwt-token");

        // jeśli kontroler po rejestracji odczytuje usera z bazy po emailu — zapewniamy mock:
        User saved = new User(
                "maciej@example.com", // username = email (spójnie z FE)
                "hashed",
                Role.ROLE_USER,
                "Maciej",
                "maciej@example.com",
                "+49 111 222"
        );
        when(userRepository.findByEmail("maciej@example.com")).thenReturn(Optional.of(saved));

        ResponseEntity<AuthUserDTO> result = authController.register(dto, response);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        AuthUserDTO body = result.getBody();
        assertNotNull(body);
        // w FE localStorage.username = email, więc testujemy tak samo
        assertEquals("maciej@example.com", body.username());
        assertEquals("ROLE_USER", body.role());
        assertEquals("Maciej", body.fullName());
        assertEquals("maciej@example.com", body.email());
        assertEquals("+49 111 222", body.phone());

        // cookie przez Set-Cookie:
        verify(response).addHeader(eq("Set-Cookie"), argThat(h -> h.contains("token=jwt-token")));
    }

    @Test
    void register_returns409_onDuplicate() {
        UserRegisterDTO dto = new UserRegisterDTO();
        dto.setEmail("maciej@example.com");
        dto.setEmail("maciej@example.com");
        dto.setPassword("test123");

        when(authService.register(dto)).thenThrow(new DataIntegrityViolationException("duplicate"));

        ResponseEntity<AuthUserDTO> result = authController.register(dto, response);

        assertEquals(HttpStatus.CONFLICT, result.getStatusCode());
        assertNull(result.getBody());
        verify(response, never()).addHeader(eq("Set-Cookie"), anyString());
    }

    // -------- login --------

    @Test
    void login_returnsAuthUserAndSetsCookie_onSuccess() {
        UserLoginDTO dto = new UserLoginDTO();
        dto.setEmail("m@example.com");
        dto.setPassword("test123");

        when(authService.login(dto)).thenReturn("jwt-token");

        User user = new User(
                "m@example.com",   // username = email
                "hashed",
                Role.ROLE_USER,
                "Maciej J",
                "m@example.com",
                "+49 123"
        );
        when(userRepository.findByEmail("m@example.com")).thenReturn(Optional.of(user));

        ResponseEntity<?> result = authController.login(dto, response);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertTrue(result.getBody() instanceof AuthUserDTO);
        AuthUserDTO body = (AuthUserDTO) result.getBody();
        assertEquals("m@example.com", body.username()); // FE traktuje to jako username
        assertEquals("ROLE_USER", body.role());
        assertEquals("Maciej J", body.fullName());
        assertEquals("m@example.com", body.email());
        assertEquals("+49 123", body.phone());

        verify(response).addHeader(eq("Set-Cookie"), argThat(h -> h.contains("token=jwt-token")));
    }

    @Test
    void login_returns401_onBadCredentials() {
        UserLoginDTO dto = new UserLoginDTO();
        dto.setEmail("m@example.com");
        dto.setPassword("bad");

        when(authService.login(dto)).thenThrow(new BadCredentialsException("bad"));

        ResponseEntity<?> result = authController.login(dto, response);

        // jeśli kontroler sam zwraca "Login Failed":
        assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode());
        assertEquals("Login Failed", result.getBody());

        verify(response, never()).addHeader(eq("Set-Cookie"), anyString());
    }

    // -------- checkAuth --------

    @Test
    void checkAuth_returnsAuthUser_whenTokenValid() {
        Cookie tokenCookie = new Cookie("token", "valid-token");
        when(request.getCookies()).thenReturn(new Cookie[]{ tokenCookie });

        // 1) kontroler wyciąga username z tokena
        when(jwtService.getUsername("valid-token")).thenReturn("maciej");

        // 2) ładuje użytkownika
        User user = new User(
                "maciej",
                "pw",
                Role.ROLE_USER,
                "Maciej J",
                "m@example.com",
                "+49 123"
        );
        when(userRepository.findByUsername("maciej")).thenReturn(Optional.of(user));

        // 3) i weryfikuje ważność tokenu względem usera
        when(jwtService.isTokenValid("valid-token", user)).thenReturn(true);

        ResponseEntity<?> result = authController.checkAuth(request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertTrue(result.getBody() instanceof AuthUserDTO);
        AuthUserDTO body = (AuthUserDTO) result.getBody();
        assertEquals("maciej", body.username());
        assertEquals("ROLE_USER", body.role());
    }
    @Test
    void checkAuth_returns401_whenCookieMissing() {
        when(request.getCookies()).thenReturn(null);

        ResponseEntity<?> result = authController.checkAuth(request);

        assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode());
        assertEquals("Invalid Token", result.getBody());
    }

    @Test
    void checkAuth_returns401_whenTokenInvalid() {
        Cookie tokenCookie = new Cookie("token", "bad");
        when(request.getCookies()).thenReturn(new Cookie[]{ tokenCookie });
        when(jwtService.getUsername("bad")).thenThrow(new RuntimeException("bad token"));

        ResponseEntity<?> result = authController.checkAuth(request);

        assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode());
        assertEquals("Invalid Token", result.getBody());
    }

    // -------- logout --------

    @Test
    void logout_overwritesCookieWithMaxAge0() {
        ResponseEntity<Void> result = authController.logout(response);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(response).addHeader(eq("Set-Cookie"),
                argThat(h -> h.contains("token=") && h.matches(".*[Mm]ax-[Aa]ge=0.*")));
    }
}