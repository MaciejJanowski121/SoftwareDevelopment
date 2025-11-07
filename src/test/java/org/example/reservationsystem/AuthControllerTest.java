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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * **Komponententest für den {@link AuthController}.**
 *
 * <p>Diese Testklasse überprüft die Funktionalität der Authentifizierungs-Endpunkte
 * des Backends. Alle abhängigen Komponenten ({@link AuthService}, {@link JwtService},
 * {@link UserRepository}) werden mithilfe von Mockito gemockt, um das Verhalten
 * des Controllers isoliert zu testen.</p>
 *
 * <p>Getestete Funktionen:
 * <ul>
 *   <li>Registrierung eines neuen Benutzers und Setzen des Authentifizierungs-Cookies</li>
 *   <li>Login mit gültigen und ungültigen Zugangsdaten</li>
 *   <li>Überprüfung der Authentifizierung über Cookie oder Authorization-Header</li>
 *   <li>Logout und Löschen des Authentifizierungs-Cookies</li>
 * </ul>
 * </p>
 *
 * <p>Ziel dieser Tests ist es, die HTTP-Logik (Statuscodes, Rückgabewerte, Cookies)
 * zu verifizieren, nicht die interne Geschäftslogik des {@link AuthService}.</p>
 *
 * @author Maciej Janowski
 */
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock private AuthService authService;
    @Mock private JwtService jwtService;
    @Mock private UserRepository userRepository;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;

    private AuthController authController;

    /**
     * Initialisiert den zu testenden {@link AuthController} mit gemockten Abhängigkeiten.
     */
    @BeforeEach
    void setUp() {
        authController = new AuthController(authService, jwtService, userRepository);
    }



    /**
     * Prüft, ob bei einer erfolgreichen Registrierung der Benutzer korrekt zurückgegeben
     * und das JWT-Cookie gesetzt wird.
     */
    @Test
    void register_returnsAuthUserAndSetsCookie_onSuccess() {
        UserRegisterDTO dto = new UserRegisterDTO();
        dto.setEmail("maciej@example.com");
        dto.setPassword("test123");
        dto.setFullName("Maciej");
        dto.setPhone("+49 111 222");

        when(authService.register(any(UserRegisterDTO.class))).thenReturn("jwt-token");

        User saved = new User(
                "encoded",
                Role.ROLE_USER,
                "Maciej",
                "maciej@example.com",
                "+49 111 222"
        );
        when(userRepository.findByEmail("maciej@example.com"))
                .thenReturn(Optional.of(saved));

        ResponseEntity<AuthUserDTO> result = authController.register(dto, response);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        AuthUserDTO body = result.getBody();
        assertNotNull(body);
        assertEquals("maciej@example.com", body.email());
        assertEquals("ROLE_USER", body.role());
        assertEquals("Maciej", body.fullName());
        assertEquals("+49 111 222", body.phone());

        verify(response).addHeader(eq("Set-Cookie"),
                argThat(v -> v.contains("token=jwt-token")));
    }

    /**
     * Prüft, ob bei einer doppelten Registrierung eine {@link DataIntegrityViolationException}
     * geworfen wird und kein Cookie gesetzt wird.
     */
    @Test
    void register_throwsException_onDuplicate() {
        UserRegisterDTO dto = new UserRegisterDTO();
        dto.setEmail("maciej@example.com");
        dto.setPassword("test123");

        when(authService.register(any(UserRegisterDTO.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThrows(DataIntegrityViolationException.class,
                () -> authController.register(dto, response));

        verify(response, never()).addHeader(eq("Set-Cookie"), anyString());
    }


    /**
     * Prüft, ob beim erfolgreichen Login ein {@link AuthUserDTO} zurückgegeben
     * und das Authentifizierungs-Cookie gesetzt wird.
     */
    @Test
    void login_returnsAuthUserAndSetsCookie_onSuccess() {
        UserLoginDTO dto = new UserLoginDTO();
        dto.setEmail("m@example.com");
        dto.setPassword("test123");

        when(authService.login(any(UserLoginDTO.class))).thenReturn("jwt-token");

        User user = new User("hashed", Role.ROLE_USER, "Maciej J", "m@example.com", "+49 123");
        when(userRepository.findByEmail("m@example.com")).thenReturn(Optional.of(user));

        ResponseEntity<AuthUserDTO> result = authController.login(dto, response);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        AuthUserDTO body = result.getBody();
        assertNotNull(body);
        assertEquals("m@example.com", body.email());
        assertEquals("ROLE_USER", body.role());
        assertEquals("Maciej J", body.fullName());
        assertEquals("+49 123", body.phone());

        verify(response).addHeader(eq("Set-Cookie"),
                argThat(h -> h.contains("token=jwt-token")));
    }

    /**
     * Prüft, ob bei ungültigen Zugangsdaten eine {@link BadCredentialsException}
     * geworfen wird und kein Cookie gesetzt wird.
     */
    @Test
    void login_throwsBadCredentials_onInvalidPassword() {
        UserLoginDTO dto = new UserLoginDTO();
        dto.setEmail("m@example.com");
        dto.setPassword("bad");

        when(authService.login(any(UserLoginDTO.class)))
                .thenThrow(new BadCredentialsException("E-Mail oder Passwort ist falsch."));

        assertThrows(BadCredentialsException.class,
                () -> authController.login(dto, response));

        verify(response, never()).addHeader(eq("Set-Cookie"), anyString());
    }

    /**
     * Prüft, ob bei gültigem Token im Cookie die Benutzerdaten korrekt zurückgegeben werden.
     */
    @Test
    void checkAuth_returnsAuthUser_whenTokenValid() {
        Cookie tokenCookie = new Cookie("token", "valid-token");
        when(request.getCookies()).thenReturn(new Cookie[]{tokenCookie});
        when(jwtService.getUsername("valid-token")).thenReturn("m@example.com");

        User user = new User("pw", Role.ROLE_USER, "Maciej J", "m@example.com", "+49 123");
        when(userRepository.findByEmail("m@example.com")).thenReturn(Optional.of(user));

        ResponseEntity<?> result = authController.checkAuth(request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertTrue(result.getBody() instanceof AuthUserDTO);
        AuthUserDTO body = (AuthUserDTO) result.getBody();
        assertEquals("m@example.com", body.email());
        assertEquals("ROLE_USER", body.role());
        assertEquals("Maciej J", body.fullName());
    }

    /**
     * Prüft, ob bei fehlendem Token (kein Cookie) der Statuscode 401 (Unauthorized) zurückgegeben wird.
     */
    @Test
    void checkAuth_returns401_whenCookieMissing() {
        when(request.getCookies()).thenReturn(null);

        ResponseEntity<?> result = authController.checkAuth(request);

        assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode());
        assertEquals("Invalid Token", result.getBody());
    }

    /**
     * Prüft, ob eine Ausnahme geworfen wird, wenn das Token ungültig ist.
     */
    @Test
    void checkAuth_throws_onInvalidToken() {
        Cookie tokenCookie = new Cookie("token", "bad");
        when(request.getCookies()).thenReturn(new Cookie[]{tokenCookie});
        when(jwtService.getUsername("bad")).thenThrow(new RuntimeException("bad token"));

        assertThrows(RuntimeException.class, () -> authController.checkAuth(request));
    }



    /**
     * Prüft, ob beim Logout das Authentifizierungs-Cookie korrekt gelöscht wird (Max-Age = 0).
     */
    @Test
    void logout_overwritesCookieWithMaxAge0() {
        ResponseEntity<Void> result = authController.logout(response);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(response).addHeader(eq("Set-Cookie"),
                argThat(h -> h.contains("token=") && h.toLowerCase().contains("max-age=0")));
    }
}