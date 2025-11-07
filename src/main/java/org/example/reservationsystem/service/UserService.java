package org.example.reservationsystem.service;

import org.example.reservationsystem.DTO.ChangePasswordDTO;
import org.example.reservationsystem.DTO.UserProfileDTO;
import org.example.reservationsystem.model.User;
import org.example.reservationsystem.repository.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service zur Benutzerverwaltung sowie Integration mit Spring Security.
 *
 * <p>Dieser Service stellt Funktionen für das Laden von Benutzerdaten (für die
 * Authentifizierung), das Lesen/Aktualisieren von Profilinformationen sowie
 * für die Passwortänderung bereit. Der Login-Identifier ist die E-Mail-Adresse,
 * die intern normalisiert (trim + lower-case) verarbeitet wird.</p>
 *
 * <p>Transaktionen:
 * <ul>
 *   <li>{@link #loadUserByUsername(String)} und {@link #getProfile(String)} sind schreibgeschützt.</li>
 *   <li>{@link #updateProfile(String, UserProfileDTO)} und {@link #changePassword(String, ChangePasswordDTO)} sind schreibend.</li>
 * </ul>
 * </p>
 *
 * <p>Ausnahmen:
 * <ul>
 *   <li>{@link UsernameNotFoundException}, wenn ein Benutzer zur E-Mail nicht existiert.</li>
 *   <li>{@link BadCredentialsException}, wenn das alte Passwort bei der Passwortänderung nicht stimmt.</li>
 *   <li>{@link IllegalArgumentException}, wenn das neue Passwort zu kurz ist.</li>
 * </ul>
 * </p>
 *
 * @author Maciej Janowski
 */
@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    /**
     * Erstellt einen neuen {@code UserService}.
     *
     * @param userRepository   Repository für Benutzerzugriffe
     * @param passwordEncoder  BCrypt-Encoder/Verifier für Passwörter
     */
    public UserService(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder) {
        this.userRepository   = userRepository;
        this.passwordEncoder  = passwordEncoder;
    }

    /**
     * Lädt Benutzerdaten für Spring Security.
     *
     * <p>Der übergebene Benutzername entspricht der E-Mail-Adresse. Diese wird
     * normalisiert (trim + lower-case) und anschließend in der Datenbank gesucht.</p>
     *
     * @param email E-Mail des Benutzers (Login-Identifier)
     * @return {@link UserDetails} des gefundenen Benutzers
     * @throws UsernameNotFoundException wenn kein Benutzer zur E-Mail existiert
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(normalize(email))
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    /**
     * Liefert das Benutzerprofil als DTO zurück.
     *
     * <p>Beinhaltet vollständigen Namen, E-Mail und optionale Telefonnummer.
     * Die E-Mail wird als im System gespeicherter Login-Identifier zurückgegeben.</p>
     *
     * @param email E-Mail des Benutzers (Login-Identifier)
     * @return {@link UserProfileDTO} mit Profilinformationen
     * @throws UsernameNotFoundException wenn kein Benutzer zur E-Mail existiert
     */
    @Transactional(readOnly = true)
    public UserProfileDTO getProfile(String email) {
        User user = userRepository.findByEmail(normalize(email))
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        UserProfileDTO dto = new UserProfileDTO();
        dto.setFullName(user.getFullName());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        return dto;
    }

    /**
     * Aktualisiert Profilinformationen (voller Name, E-Mail, Telefon).
     *
     * <p>Übergebene Werte werden, sofern gesetzt und nicht leer, übernommen.
     * Die E-Mail wird auf Kleinbuchstaben normalisiert. Persistiert den Benutzer nach Anpassungen.</p>
     *
     * @param email aktuelle E-Mail des Benutzers (Login-Identifier)
     * @param dto   neue Profildaten (Felder können {@code null} oder leer sein)
     * @throws UsernameNotFoundException wenn kein Benutzer zur E-Mail existiert
     */
    @Transactional
    public void updateProfile(String email, UserProfileDTO dto) {
        User user = userRepository.findByEmail(normalize(email))
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (dto.getFullName() != null && !dto.getFullName().isBlank()) {
            user.setFullName(dto.getFullName().trim());
        }
        if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
            user.setEmail(dto.getEmail().trim().toLowerCase());
        }
        if (dto.getPhone() != null && !dto.getPhone().isBlank()) {
            user.setPhone(dto.getPhone().trim());
        }

        userRepository.save(user);
    }

    /**
     * Ändert das Passwort eines Benutzers.
     *
     * <p>Prüft das alte Passwort mittels {@link BCryptPasswordEncoder#matches(CharSequence, String)}.
     * Das neue Passwort muss mindestens 6 Zeichen lang sein und wird mit BCrypt gehasht gespeichert.</p>
     *
     * @param email E-Mail des Benutzers (Login-Identifier)
     * @param dto   DTO mit altem und neuem Passwort
     * @throws UsernameNotFoundException wenn kein Benutzer zur E-Mail existiert
     * @throws BadCredentialsException   wenn das alte Passwort nicht korrekt ist
     * @throws IllegalArgumentException  wenn das neue Passwort zu kurz ist
     */
    @Transactional
    public void changePassword(String email, ChangePasswordDTO dto) {
        User user = userRepository.findByEmail(normalize(email))
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            throw new BadCredentialsException("Old password is incorrect.");
        }

        if (dto.getNewPassword() == null || dto.getNewPassword().length() < 6) {
            throw new IllegalArgumentException("New password must be at least 6 characters long.");
        }

        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userRepository.save(user);
    }

    

    /**
     * Normalisiert eine E-Mail (Trim + Kleinschreibung). {@code null} bleibt {@code null}.
     *
     * @param email Eingabewert oder {@code null}
     * @return normalisierte E-Mail oder {@code null}
     */
    private static String normalize(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}