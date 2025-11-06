package org.example.reservationsystem;

import org.example.reservationsystem.DTO.UserLoginDTO;
import org.example.reservationsystem.DTO.UserRegisterDTO;
import org.example.reservationsystem.JWTServices.JwtAuthenticationFilter;
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
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;

    // Nie używamy bezpośrednio w serwisie, ale zostaje dla zgodności z konstruktorem
    @Mock private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Mock private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private UserRegisterDTO registerDTO;
    private UserLoginDTO loginDTO;
    private User user; // istniejący user z hasłem zakodowanym

    @BeforeEach
    void setUp() {
        // DTO do rejestracji (email = username)
        registerDTO = new UserRegisterDTO();
        registerDTO.setEmail("testuser@example.com");
        registerDTO.setPassword("password123");
        registerDTO.setFullName("Test User");
        registerDTO.setPhone("+49 170 0000000");

        // DTO do logowania
        loginDTO = new UserLoginDTO();
        loginDTO.setEmail("testuser@example.com");
        loginDTO.setPassword("password123");

        // UŻYJ WŁAŚCIWEGO KONSTRUKTORA USERA: (username, password, role, fullName, email, phone)
        user = new User(
                "testuser@example.com",    // username (u Ciebie = email)
                "encodedPassword",         // zakodowane hasło trzymane w DB
                Role.ROLE_USER,
                "Test User",
                "testuser@example.com",
                "+49 170 0000000"
        );
    }

    @Test
    void register_shouldReturnToken() {
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        // save() zwraca zapisany obiekt
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.generateToken(any(User.class))).thenReturn("mockToken");

        String token = authService.register(registerDTO);

        assertEquals("mockToken", token);
        verify(userRepository).save(any(User.class));
        verify(jwtService).generateToken(any(User.class));
        // brak innych wywołań nie jest tu konieczne, ale jeśli chcesz:
        verifyNoMoreInteractions(jwtService);
    }

    @Test
    void login_shouldReturnToken_whenCredentialsAreCorrect() {
        when(userRepository.findByEmail("testuser@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("mockToken");

        String token = authService.login(loginDTO);

        assertEquals("mockToken", token);
        verify(jwtService).generateToken(user);
    }

    @Test
    void login_shouldThrow_whenUserNotFound() {
        when(userRepository.findByEmail("testuser@example.com")).thenReturn(Optional.empty());

        assertThrows(BadCredentialsException.class, () -> authService.login(loginDTO));

        verifyNoInteractions(jwtService);
    }

    @Test
    void login_shouldThrow_whenPasswordInvalid() {
        when(userRepository.findByEmail("testuser@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> authService.login(loginDTO));

        verifyNoInteractions(jwtService);
    }
}