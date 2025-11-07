package org.example.reservationsystem.service;

import org.example.reservationsystem.DTO.UserLoginDTO;
import org.example.reservationsystem.DTO.UserRegisterDTO;
import org.example.reservationsystem.JWTServices.JwtService;
import org.example.reservationsystem.model.Role;
import org.example.reservationsystem.model.User;
import org.example.reservationsystem.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Objects;

/**
 * Service für Registrierung und Anmeldung von Benutzern.
 *
 * <p>Der Service verwaltet die Erstellung neuer Benutzerkonten (mit gehashtem Passwort
 * und Standardrolle {@link Role#ROLE_USER}) sowie die Authentifizierung bestehender
 * Benutzer per E-Mail und Passwort. Nach erfolgreicher Registrierung bzw. Anmeldung
 * wird ein JWT erzeugt, dessen Subject dem Login-Identifier entspricht
 * ({@link UserDetails#getUsername()} == E-Mail).</p>
 *
 * <p>Transaktionen:
 * <ul>
 *   <li>{@link #register(UserRegisterDTO)}: schreibende Transaktion (persistiert Benutzer).</li>
 *   <li>{@link #login(UserLoginDTO)}: nur lesend; erzeugt ein JWT ohne Änderungen in der DB.</li>
 * </ul>
 * </p>
 *
 * <p>Validierung/Normalisierung:
 * E-Mails werden getrimmt und in Kleinschreibung konvertiert; einfache Formatprüfungen
 * werden durchgeführt. Passwörter müssen mindestens 6 Zeichen lang sein. Telefonnummern
 * sind optional und werden bei Angabe auf ein einfaches zulässiges Muster geprüft.</p>
 *
 * <p>Fehlerfälle:
 * <ul>
 *   <li>{@link IllegalArgumentException} bei ungültigen oder leeren Eingaben (E-Mail, Name, Passwort)
 *       bzw. bei fehlerhaftem Format.</li>
 *   <li>{@link DataIntegrityViolationException} wenn die E-Mail bereits registriert ist
 *       (inkl. Rennen um die Eindeutigkeit).</li>
 *   <li>{@link BadCredentialsException} bei falscher E-Mail/Passwort-Kombination im Login.</li>
 * </ul>
 * </p>
 *
 * <p>Thread-Sicherheit: Der Service ist zustandslos und somit für typische Spring-Scopes
 * threadsicher, solange die injizierten Abhängigkeiten threadsicher sind.</p>
 *
 * @author Maciej Janowski
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    /**
     * Erstellt einen neuen {@code AuthService}.
     *
     * @param userRepository Repository für Benutzerabfragen und -speicherungen (nicht {@code null})
     * @param passwordEncoder Passwort-Hasher/Verifier (nicht {@code null})
     * @param jwtService Service zur JWT-Erzeugung (nicht {@code null})
     * @throws NullPointerException wenn eine der Abhängigkeiten {@code null} ist
     */
    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository   = Objects.requireNonNull(userRepository);
        this.passwordEncoder  = Objects.requireNonNull(passwordEncoder);
        this.jwtService       = Objects.requireNonNull(jwtService);
    }

    /**
     * Registriert einen neuen Benutzer anhand von E-Mail, Passwort und vollem Namen
     * und liefert ein JWT zurück.
     *
     * <p>Ablauf in Kurzform:
     * Normalisiert und validiert Eingaben; prüft, ob die E-Mail bereits existiert;
     * speichert den Benutzer mit {@link Role#ROLE_USER} und gehashtem Passwort;
     * erzeugt ein JWT, dessen Subject die E-Mail ist.</p>
     *
     * @param dto Registrierungsdaten (E-Mail, Passwort, voller Name, optionale Telefonnummer)
     * @return JWT-String mit der E-Mail als Subject
     *
     * @throws IllegalArgumentException wenn E-Mail/Passwort/Name leer oder ungültig sind
     *                                  bzw. das Passwort kürzer als 6 Zeichen ist
     * @throws DataIntegrityViolationException wenn die E-Mail bereits registriert ist
     *                                         (inkl. Abfangen von Rennbedingungen)
     */
    @Transactional
    public String register(UserRegisterDTO dto) {
        final String email    = normalizeEmail(dto.getEmail());
        final String fullName = requireNonBlank(dto.getFullName(), "Name darf nicht leer sein.");
        final String rawPwd   = requireNonBlank(dto.getPassword(), "Passwort darf nicht leer sein.");

        if (rawPwd.length() < 6) {
            throw new IllegalArgumentException("Passwort muss mindestens 6 Zeichen lang sein.");
        }


        if (userRepository.existsByEmail(email)) {
            throw new DataIntegrityViolationException("E-Mail ist bereits registriert.");
        }

        User user = new User(
                passwordEncoder.encode(rawPwd),
                Role.ROLE_USER,
                fullName.trim(),
                email,
                normalizePhone(dto.getPhone())
        );

        try {
            userRepository.save(user);
        } catch (DataIntegrityViolationException ex) {

            throw new DataIntegrityViolationException("E-Mail ist bereits registriert.", ex);
        }


        return jwtService.generateToken(user);
    }

    /**
     * Authentifiziert einen Benutzer mittels E-Mail und Passwort und liefert ein JWT zurück.
     *
     * <p>Ablauf in Kurzform:
     * Normalisiert und validiert Eingaben; lädt Benutzer per E-Mail; verifiziert das
     * Passwort mittels {@link PasswordEncoder#matches(CharSequence, String)}; erzeugt
     * ein JWT bei erfolgreicher Prüfung.</p>
     *
     * @param dto Logindaten (E-Mail, Passwort)
     * @return JWT-String mit der E-Mail als Subject
     *
     * @throws BadCredentialsException wenn E-Mail nicht existiert oder das Passwort nicht passt
     * @throws IllegalArgumentException wenn Eingaben leer/ungültig sind
     */
    @Transactional(readOnly = true)
    public String login(UserLoginDTO dto) {
        final String email  = normalizeEmail(dto.getEmail());
        final String rawPwd = requireNonBlank(dto.getPassword(), "E-Mail oder Passwort ist falsch.");

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("E-Mail oder Passwort ist falsch."));

        if (!passwordEncoder.matches(rawPwd, user.getPassword())) {
            throw new BadCredentialsException("E-Mail oder Passwort ist falsch.");
        }

        return jwtService.generateToken(user);
    }


    /**
     * Normalisiert eine E-Mail (Trim + Kleinschreibung) und führt eine einfache
     * Formatprüfung durch.
     *
     * @param email Eingabewert
     * @return normalisierte E-Mail
     * @throws IllegalArgumentException wenn der Wert leer/ungültig ist
     */
    private String normalizeEmail(String email) {
        String e = requireNonBlank(email, "E-Mail darf nicht leer sein.")
                .trim()
                .toLowerCase(Locale.ROOT);
        if (!e.contains("@") || !e.contains(".")) {
            throw new IllegalArgumentException("E-Mail-Format ist ungültig.");
        }
        return e;
    }

    /**
     * Normalisiert eine Telefonnummer. Leere Werte sind erlaubt; bei Angabe wird
     * auf ein einfaches Muster geprüft.
     *
     * @param phone Eingabewert (optional)
     * @return normalisierte Telefonnummer oder {@code null}, wenn leer
     * @throws IllegalArgumentException bei ungültigem Format
     */
    private String normalizePhone(String phone) {
        if (phone == null) return null;
        String t = phone.trim();
        if (t.isEmpty()) return null;
        if (!t.matches("[0-9 +\\-()]{3,}")) {
            throw new IllegalArgumentException("Telefonformat ist ungültig.");
        }
        return t;
    }

    /**
     * Prüft, ob der String nicht {@code null} bzw. nicht leer ist, und gibt den
     * getrimmten Wert zurück.
     *
     * @param v   Eingabewert
     * @param msg Fehlermeldung bei Verstoß
     * @return getrimmter Wert
     * @throws IllegalArgumentException wenn der Wert {@code null} oder leer ist
     */
    private String requireNonBlank(String v, String msg) {
        if (v == null || v.trim().isEmpty()) {
            throw new IllegalArgumentException(msg);
        }
        return v.trim();
    }
}