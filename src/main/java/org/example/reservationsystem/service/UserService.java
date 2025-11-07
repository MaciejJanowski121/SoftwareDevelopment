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

@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder) {
        this.userRepository   = userRepository;
        this.passwordEncoder  = passwordEncoder;
    }

    /**
     * Wird von Spring Security verwendet – loginName = E-Mail.
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(normalize(email))
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    /**
     * Gibt das Benutzerprofil als DTO zurück.
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
     * Aktualisiert Profilinformationen (Name, E-Mail, Telefon).
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
     * Ändert das Passwort des Benutzers, nachdem das alte überprüft wurde.
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

    // ---------------- helpers ----------------

    private static String normalize(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}