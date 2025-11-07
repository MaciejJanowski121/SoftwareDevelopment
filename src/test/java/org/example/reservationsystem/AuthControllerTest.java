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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock private AuthService authService;
    @Mock private JwtService jwtService;
    @Mock private UserRepository userRepository;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;

    @InjectMocks
    private AuthController authController;

    // -------- register --------

    @Test
    void register_returnsAuthUserAndSetsCookie_onSuccess() {
        UserRegisterDTO dto = new UserRegisterDTO();
        dto.setEmail("maciej@example.com");
        dto.setPassword("test123");
        dto.setFullName("Maciej");
        dto.setPhone("+49 111 222");

        when(authService.register(dto)).thenReturn("jwt-token");

        // ⬇️ KRYTYCZNE: kontroler po rejestracji czyta usera z bazy
        User saved = new User(
                "encoded",             // password (nieistotne w teście)
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
        assertEquals("maciej@example.com", body.email());
        assertEquals("+49 111 222", body.phone());

        verify(response).addHeader(eq("Set-Cookie"),
                argThat(h -> h.contains("token=jwt-token")));
    }
    @Test
    void register_returns409_onDuplicate() {
        UserRegisterDTO dto = new UserRegisterDTO();
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
        assertEquals("m@example.com", body.email());
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

        when(authService.login(dto)).thenThrow(new BadCredentialsException("E-Mail oder Passwort ist falsch."));

        ResponseEntity<?> result = authController.login(dto, response);

        assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode());
        // jeśli globalny ExceptionHandler zwraca dokładnie ten tekst:
        assertEquals("E-Mail oder Passwort ist falsch.", result.getBody());
        verify(response, never()).addHeader(eq("Set-Cookie"), anyString());
    }

    // -------- checkAuth --------

    @Test
    void checkAuth_returnsAuthUser_whenTokenValid() {
        Cookie tokenCookie = new Cookie("token", "valid-token");
        when(request.getCookies()).thenReturn(new Cookie[]{ tokenCookie });

        // kontroler używa getUsername(token) → zwracamy EMAIL
        when(jwtService.getUsername("valid-token")).thenReturn("m@example.com");

        // najpierw spróbuje findByUsername(...), niech będzie pusto:
        when(userRepository.findByEmail("m@example.com")).thenReturn(Optional.empty());
        // potem po e-mailu zwracamy użytkownika:
        User user = new User("pw", Role.ROLE_USER, "Maciej J", "m@example.com", "+49 123");
        when(userRepository.findByEmail("m@example.com")).thenReturn(Optional.of(user));

        ResponseEntity<?> result = authController.checkAuth(request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertTrue(result.getBody() instanceof AuthUserDTO);
        AuthUserDTO body = (AuthUserDTO) result.getBody();
        assertEquals("m@example.com", body.email());
        assertEquals("ROLE_USER", body.role());
        assertEquals("Maciej J", body.fullName());
        assertEquals("m@example.com", body.email());
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
                argThat(h -> h.contains("token=") && h.toLowerCase().contains("max-age=0")));
    }
}