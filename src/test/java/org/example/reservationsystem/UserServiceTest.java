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

/**
 * Komponententest für den {@link UserService}.
 *
 * <p>Dieser Test überprüft die Geschäftslogik der Benutzerverwaltung,
 * insbesondere das Laden von Benutzern per E-Mail und den sicheren
 * Passwortwechsel unter Verwendung von {@link BCryptPasswordEncoder}.</p>
 *
 * <p>Die Tests werden mit {@link Mockito} ausgeführt und isolieren den
 * Service vollständig von der Datenbank. Das Repository wird gemockt,
 * sodass nur die Fachlogik im {@link UserService} überprüft wird.</p>
 *
 * <p>Abgedeckte Szenarien:</p>
 * <ul>
 *   <li>Laden eines vorhandenen Benutzers per E-Mail (UserDetails)</li>
 *   <li>Fehlerfall: Benutzer nicht gefunden → {@link UsernameNotFoundException}</li>
 *   <li>Passwortänderung bei korrektem alten Passwort</li>
 *   <li>Fehlerfall: falsches altes Passwort → {@link BadCredentialsException}</li>
 * </ul>
 *
 * author Maciej Janowski
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    @InjectMocks
    private UserService userService;

    /**
     * Testet das erfolgreiche Laden eines Benutzers über {@link UserService#loadUserByUsername(String)}.
     *
     * <p>Das Repository liefert einen Benutzer mit der angegebenen E-Mail-Adresse.
     * Erwartet wird, dass der zurückgegebene {@code UserDetails}-Objekt denselben
     * Benutzernamen (E-Mail) enthält.</p>
     */
    @Test
    void loadUserByUsername_UserExists_ReturnsUser() {
        String email = "john@example.com";
        User user = new User(
                "encoded",
                Role.ROLE_USER,
                "John Doe",
                email,
                "+49 170 1234567"
        );

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        var result = userService.loadUserByUsername(email);

        assertEquals(email, result.getUsername(), "UserDetails sollte dieselbe E-Mail zurückgeben");
    }

    /**
     * Testet den Fehlerfall beim Laden eines nicht existierenden Benutzers.
     *
     * <p>Wenn das Repository keinen Treffer liefert, soll
     * {@link UsernameNotFoundException} geworfen werden.</p>
     */
    @Test
    void loadUserByUsername_UserNotFound_ThrowsException() {
        String email = "unknown@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
                () -> userService.loadUserByUsername(email));
    }

    /**
     * Testet den erfolgreichen Ablauf von {@link UserService#changePassword(String, ChangePasswordDTO)}.
     *
     * <p>Bei korrektem altem Passwort wird das neue Passwort verschlüsselt
     * und gespeichert. Die Methode darf keine Ausnahme werfen.</p>
     */
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

        ChangePasswordDTO dto = new ChangePasswordDTO("old", "newpass");

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(bCryptPasswordEncoder.matches("old", "oldEncoded")).thenReturn(true);
        when(bCryptPasswordEncoder.encode("newpass")).thenReturn("newEncoded");

        userService.changePassword(email, dto);

        verify(userRepository).save(user);
        assertEquals("newEncoded", user.getPassword());
    }

    /**
     * Testet den Fehlerfall eines falschen alten Passworts beim Passwortwechsel.
     *
     * <p>Wenn {@link BCryptPasswordEncoder#matches(CharSequence, String)} {@code false}
     * zurückgibt, wird eine {@link BadCredentialsException} erwartet und der Benutzer
     * darf nicht gespeichert werden.</p>
     */
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