package org.example.reservationsystem.DTO;

import org.example.reservationsystem.model.User;

public record AuthUserDTO(
        String email,       // jedno pole zamiast username+email
        String role,
        String fullName,
        String phone
) {
    /** Tworzy DTO z encji User */
    public static AuthUserDTO fromUser(User user) {
        return new AuthUserDTO(
                user.getEmail(),
                user.getRole().name(),
                user.getFullName(),
                user.getPhone()
        );
    }
}