package org.example.reservationsystem.DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UserRegisterDTO {

    @NotBlank(message = "E-Mail darf nicht leer sein.")
    @Email(message = "Bitte geben Sie eine gültige E-Mail-Adresse ein.")
    private String email;

    @NotBlank(message = "Passwort darf nicht leer sein.")
    @Size(min = 6, message = "Das Passwort muss mindestens 6 Zeichen lang sein.")
    private String password;

    @NotBlank(message = "Vollständiger Name ist erforderlich.")
    private String fullName;

    private String phone; // kann optional bleiben

    public UserRegisterDTO() {}

    // --- Getter & Setter ---
    public String getEmail() { return email; }
    public void setEmail(String v) { this.email = v; }

    public String getPassword() { return password; }
    public void setPassword(String v) { this.password = v; }

    public String getFullName() { return fullName; }
    public void setFullName(String v) { this.fullName = v; }

    public String getPhone() { return phone; }
    public void setPhone(String v) { this.phone = v; }
}