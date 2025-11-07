package org.example.reservationsystem;

import org.example.reservationsystem.DTO.ChangePasswordDTO;
import org.example.reservationsystem.model.Role;
import org.example.reservationsystem.model.User;
import org.example.reservationsystem.repository.UserRepository;
import org.example.reservationsystem.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void loadUserByUsername_UserExists_ReturnsUser() {
        // email jest jedynym identyfikatorem logowania
        String email = "john@example.com";
        User user = new User(
          // jeśli encja wciąż ma pole username, możesz tu podać cokolwiek
                "encoded",
                Role.ROLE_USER,
                "John Doe",
                email,
                "+49 170 1234567"
        );

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        var result = userService.loadUserByUsername(email);
        // getUsername() w UserDetails powinno zwracać email (w wariancie email-only)
        assertEquals(email, result.getUsername());
    }

    @Test
    void loadUserByUsername_UserNotFound_ThrowsException() {
        String email = "unknown@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
                () -> userService.loadUserByUsername(email));
    }

    @Test
    void changePassword_CorrectOldPassword_ChangesPassword() {
        String email = "john@example.com";
        User user = new User(
                "oldEncoded",
                Role.ROLE_USER,
                "John Doe",
                email,
                "+49 170 1234567"
        );
        ChangePasswordDTO dto = new ChangePasswordDTO("old", "new");

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(bCryptPasswordEncoder.matches("old", "oldEncoded")).thenReturn(true);
        when(bCryptPasswordEncoder.encode("new")).thenReturn("newEncoded");

        userService.changePassword(email, dto);

        verify(userRepository).save(user);
        assertEquals("newEncoded", user.getPassword());
    }

    @Test
    void changePassword_WrongOldPassword_ThrowsException() {
        String email = "john@example.com";
        User user = new User(
                "oldEncoded",
                Role.ROLE_USER,
                "John Doe",
                email,
                "+49 170 1234567"
        );
        ChangePasswordDTO dto = new ChangePasswordDTO("WRONG", "new");

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(bCryptPasswordEncoder.matches("WRONG", "oldEncoded")).thenReturn(false);

        assertThrows(BadCredentialsException.class,
                () -> userService.changePassword(email, dto));

        verify(userRepository, never()).save(any());
    }
}