package org.example.reservationsystem.DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class UserLoginDTO {

    @NotBlank(message = "E-Mail darf nicht leer sein.")
    @Email(message = "Bitte geben Sie eine gültige E-Mail-Adresse ein.")
    private String email;

    @NotBlank(message = "Passwort darf nicht leer sein.")
    private String password;

    public UserLoginDTO() {}

    // --- Getter & Setter ---
    public String getEmail() { return email; }
    public void setEmail(String v) { this.email = v; }

    public String getPassword() { return password; }
    public void setPassword(String v) { this.password = v; }
}