// src/main/java/org/example/reservationsystem/service/AuthService.java
package org.example.reservationsystem.service;

import org.example.reservationsystem.DTO.UserLoginDTO;
import org.example.reservationsystem.DTO.UserRegisterDTO;
import org.example.reservationsystem.JWTServices.JwtAuthenticationFilter;
import org.example.reservationsystem.JWTServices.JwtService;
import org.example.reservationsystem.model.Role;
import org.example.reservationsystem.model.User;
import org.example.reservationsystem.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtAuthenticationFilter jwtAuthenticationFilter,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.jwtService = jwtService;
    }

    // Registrierung ohne Benutzername: wir setzen username = email (stabiler Identifier)
    public String register(UserRegisterDTO dto) {
        String encodedPassword = passwordEncoder.encode(dto.getPassword());

        User newUser = new User(
                /* username */ dto.getEmail(),              // <- username = email
                encodedPassword,
                Role.ROLE_USER,
                dto.getFullName(),
                dto.getEmail(),
                dto.getPhone()
        );

        try {
            userRepository.save(newUser);
        } catch (DataIntegrityViolationException ex) {
            throw ex;
        }

        return jwtService.generateToken(newUser);
    }

    public String login(UserLoginDTO dto) {
        User user = userRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new BadCredentialsException("E-Mail oder Passwort ist falsch."));

        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("E-Mail oder Passwort ist falsch.");
        }

        return jwtService.generateToken(user);
    }
}