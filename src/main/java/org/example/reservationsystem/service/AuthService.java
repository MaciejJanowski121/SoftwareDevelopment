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

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository   = Objects.requireNonNull(userRepository);
        this.passwordEncoder  = Objects.requireNonNull(passwordEncoder);
        this.jwtService       = Objects.requireNonNull(jwtService);
    }

    /**
     * Registriert einen neuen Benutzer anhand von E-Mail, Passwort und Namen.
     * Liefert ein JWT mit der E-Mail als Subject (UserDetails#getUsername == E-Mail).
     */
    @Transactional
    public String register(UserRegisterDTO dto) {
        final String email    = normalizeEmail(dto.getEmail());
        final String fullName = requireNonBlank(dto.getFullName(), "Name darf nicht leer sein.");
        final String rawPwd   = requireNonBlank(dto.getPassword(), "Passwort darf nicht leer sein.");

        if (rawPwd.length() < 6) {
            throw new IllegalArgumentException("Passwort muss mindestens 6 Zeichen lang sein.");
        }

        // Optionales Fast-Fail: freundlichere Fehlermeldung vor DB-Constraint
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
            // Fängt Rennbedingungen ab, falls zwei Requests gleichzeitig kommen
            throw new DataIntegrityViolationException("E-Mail ist bereits registriert.", ex);
        }

        // Token wird über UserDetails erzeugt (Subject = user.getUsername() = E-Mail)
        return jwtService.generateToken(user);
    }

    /**
     * Führt Login über E-Mail + Passwort durch und liefert ein JWT.
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

    // ---------------- Helpers ----------------

    /** Normalisiert E-Mail (trim + lower) und prüft grob das Format. */
    private String normalizeEmail(String email) {
        String e = requireNonBlank(email, "E-Mail darf nicht leer sein.")
                .trim()
                .toLowerCase(Locale.ROOT);
        if (!e.contains("@") || !e.contains(".")) {
            throw new IllegalArgumentException("E-Mail-Format ist ungültig.");
        }
        return e;
    }

    /** Lässt leere Telefonnummer zu; ansonsten einfacher Format-Check. */
    private String normalizePhone(String phone) {
        if (phone == null) return null;
        String t = phone.trim();
        if (t.isEmpty()) return null;
        if (!t.matches("[0-9 +\\-()]{3,}")) {
            throw new IllegalArgumentException("Telefonformat ist ungültig.");
        }
        return t;
    }

    /** Prüft auf null/leer und gibt getrimmten Wert zurück. */
    private String requireNonBlank(String v, String msg) {
        if (v == null || v.trim().isEmpty()) {
            throw new IllegalArgumentException(msg);
        }
        return v.trim();
    }
}