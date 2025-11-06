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

    // <<< WAŻNE: mockujemy dokładnie BCryptPasswordEncoder, bo tego używa serwis >>>
    @Mock
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void loadUserByUsername_UserExists_ReturnsUser() {
        User user = new User(
                "john", "encoded", Role.ROLE_USER,
                "John Doe", "john@example.com", "+49 170 1234567"
        );

        // Stubuj TYLKO to, czego używa serwis (tu: findByUsername)
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));

        var result = userService.loadUserByUsername("john");
        assertEquals("john", result.getUsername());
    }

    @Test
    void loadUserByUsername_UserNotFound_ThrowsException() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());
        assertThrows(UsernameNotFoundException.class,
                () -> userService.loadUserByUsername("unknown"));
    }

    @Test
    void changePassword_CorrectOldPassword_ChangesPassword() {
        User user = new User(
                "john", "oldEncoded", Role.ROLE_USER,
                "John Doe", "john@example.com", "+49 170 1234567"
        );
        ChangePasswordDTO dto = new ChangePasswordDTO("old", "new");

        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        when(bCryptPasswordEncoder.matches("old", "oldEncoded")).thenReturn(true);
        when(bCryptPasswordEncoder.encode("new")).thenReturn("newEncoded");

        userService.changePassword("john", dto);

        verify(userRepository).save(user);
        assertEquals("newEncoded", user.getPassword());
    }

    @Test
    void changePassword_WrongOldPassword_ThrowsException() {
        User user = new User(
                "john", "oldEncoded", Role.ROLE_USER,
                "John Doe", "john@example.com", "+49 170 1234567"
        );
        ChangePasswordDTO dto = new ChangePasswordDTO("WRONG", "new");

        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        when(bCryptPasswordEncoder.matches("WRONG", "oldEncoded")).thenReturn(false);

        assertThrows(BadCredentialsException.class,
                () -> userService.changePassword("john", dto));

        verify(userRepository, never()).save(any());
    }
}