// src/main/java/.../model/User.java
package org.example.reservationsystem.model;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
@Entity
@Table(name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_users_email", columnNames = "email")
        })
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Hasło zahashowane (BCrypt)
    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    // Pełne imię i nazwisko
    @Column(name = "full_name", nullable = false, length = 200)
    private String fullName;

    // Jedyny identyfikator logowania – e-mail (unikalny)
    @Column(name = "email", nullable = false, length = 320, unique = true)
    private String email;

    // Telefon opcjonalny
    @Column(name = "phone", length = 50)
    private String phone;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Reservation reservation;

    public User() {}

    // Minimalny konstruktor do rejestracji
    public User(String password, Role role, String fullName, String email, String phone) {
        this.password = password;
        this.role = role;
        this.fullName = fullName;
        this.email = normalize(email);
        this.phone = phone;
    }

    // --- UserDetails ---
    @Override public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role != null ? role.name() : "ROLE_USER"));
    }
    @Override public String  getPassword()             { return password; }
    @Override public String  getUsername()             { return email; } // identyfikator = e-mail
    @Override public boolean isAccountNonExpired()     { return true; }
    @Override public boolean isAccountNonLocked()      { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled()               { return true; }

    // --- gettery/settery ---
    public Long getId() { return id; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = normalize(email); }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public void setPassword(String password) { this.password = password; }

    public Reservation getReservation() { return reservation; }
    public void setReservation(Reservation reservation) { this.reservation = reservation; }

    // Normalizacja e-maila
    private static String normalize(String s) {
        return s == null ? null : s.trim().toLowerCase();
    }

    // Spójność i normalizacja przed zapisem
    @PrePersist @PreUpdate
    private void prePersist() {
        this.email = normalize(this.email);
    }
}