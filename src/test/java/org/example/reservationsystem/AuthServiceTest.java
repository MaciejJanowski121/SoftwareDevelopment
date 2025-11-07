package org.example.reservationsystem;

import org.example.reservationsystem.DTO.UserLoginDTO;
import org.example.reservationsystem.DTO.UserRegisterDTO;
import org.example.reservationsystem.JWTServices.JwtService;
import org.example.reservationsystem.model.Role;
import org.example.reservationsystem.model.User;
import org.example.reservationsystem.repository.UserRepository;
import org.example.reservationsystem.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Komponententests für den {@link AuthService}.
 *
 * <p>Diese Tests prüfen die Geschäftslogik der Registrierung und des Logins
 * isoliert von der Webschicht. Alle Abhängigkeiten werden gemockt:
 * {@link UserRepository}, {@link PasswordEncoder}, {@link JwtService}.</p>
 *
 * <p>Abgedeckte Pfade:
 * <ul>
 *   <li>Registrierung (Happy Path)</li>
 *   <li>Registrierung – E-Mail bereits vergeben (Konflikt vor dem DB-Constraint)</li>
 *   <li>Registrierung – ungültiges E-Mail-Format</li>
 *   <li>Registrierung – zu kurzes Passwort</li>
 *   <li>Login (Happy Path)</li>
 *   <li>Login – Benutzer nicht gefunden</li>
 *   <li>Login – falsches Passwort</li>
 * </ul>
 * </p>
 *
 * <p>Hinweis: Der Login-Identifier ist die E-Mail; der Service normalisiert
 * Eingaben (trim + lower). Die Token-Erzeugung wird über {@link JwtService}
 * gemockt, da ihre Korrektheit separat getestet/abgedeckt wird.</p>
 *
 * author Maciej Janowski
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private UserRegisterDTO registerDTO;
    private UserLoginDTO loginDTO;
    private User existingUser;

    @BeforeEach
    void setUp() {

        registerDTO = new UserRegisterDTO();
        registerDTO.setEmail("testuser@example.com");
        registerDTO.setPassword("password123");
        registerDTO.setFullName("Test User");
        registerDTO.setPhone("+49 170 0000000");


        loginDTO = new UserLoginDTO();
        loginDTO.setEmail("testuser@example.com");
        loginDTO.setPassword("password123");


        existingUser = new User(
                "encodedPassword",
                Role.ROLE_USER,
                "Test User",
                "testuser@example.com",
                "+49 170 0000000"
        );
    }



    /**
     * Happy Path: neue Registrierung liefert ein JWT-Token.
     */
    @Test
    void register_shouldReturnToken() {
        when(userRepository.existsByEmail("testuser@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.generateToken(any(User.class))).thenReturn("mockToken");

        String token = authService.register(registerDTO);

        assertEquals("mockToken", token);
        verify(userRepository).existsByEmail("testuser@example.com");
        verify(userRepository).save(any(User.class));
        verify(jwtService).generateToken(any(User.class));
        verifyNoMoreInteractions(jwtService);
    }

    /**
     * E-Mail ist bereits registriert → es wird eine {@link DataIntegrityViolationException} geworfen.
     * (Vorab-Check per {@code existsByEmail}, noch vor DB-Constraint.)
     */
    @Test
    void register_shouldThrow_whenEmailAlreadyExists() {
        when(userRepository.existsByEmail("testuser@example.com")).thenReturn(true);

        assertThrows(DataIntegrityViolationException.class, () -> authService.register(registerDTO));

        verify(userRepository, never()).save(any());
        verifyNoInteractions(jwtService);
    }

    /**
     * Ungültiges E-Mail-Format → {@link IllegalArgumentException}.
     */
    @Test
    void register_shouldThrow_onInvalidEmailFormat() {
        registerDTO.setEmail("invalid-email-without-at");
        assertThrows(IllegalArgumentException.class, () -> authService.register(registerDTO));
        verifyNoInteractions(userRepository, jwtService);
    }

    /**
     * Zu kurzes Passwort (< 6) → {@link IllegalArgumentException}.
     */
    @Test
    void register_shouldThrow_onTooShortPassword() {
        registerDTO.setPassword("123"); // < 6
        assertThrows(IllegalArgumentException.class, () -> authService.register(registerDTO));
        verifyNoInteractions(userRepository, jwtService);
    }



    /**
     * Happy Path: korrekte Credentials liefern ein JWT-Token.
     */
    @Test
    void login_shouldReturnToken_whenCredentialsAreCorrect() {
        when(userRepository.findByEmail("testuser@example.com")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
        when(jwtService.generateToken(existingUser)).thenReturn("mockToken");

        String token = authService.login(loginDTO);

        assertEquals("mockToken", token);
        verify(jwtService).generateToken(existingUser);
    }

    /**
     * Benutzer existiert nicht → {@link BadCredentialsException}.
     */
    @Test
    void login_shouldThrow_whenUserNotFound() {
        when(userRepository.findByEmail("testuser@example.com")).thenReturn(Optional.empty());

        assertThrows(BadCredentialsException.class, () -> authService.login(loginDTO));

        verifyNoInteractions(jwtService);
    }

    /**
     * Passwort ist falsch → {@link BadCredentialsException}.
     */
    @Test
    void login_shouldThrow_whenPasswordInvalid() {
        when(userRepository.findByEmail("testuser@example.com")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> authService.login(loginDTO));

        verifyNoInteractions(jwtService);
    }
}