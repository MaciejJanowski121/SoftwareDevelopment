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
                @UniqueConstraint(name = "uk_users_username", columnNames = "username"),
                @UniqueConstraint(name = "uk_users_email",    columnNames = "email")
        })
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** = email (zawsze zsynchronizowany i znormalizowany) */
    @Column(nullable = false, length = 100)
    private String username;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(name = "full_name", nullable = false, length = 200)
    private String fullName;

    @Column(name = "email", nullable = false, length = 320)
    private String email;

    @Column(name = "phone", length = 50)
    private String phone;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Reservation reservation;

    public User() {}

    public User(String username, String password, Role role) {
        this.username = normalize(username);
        this.password = password;
        this.role = role;
        // jeśli username to e-mail – od razu ustaw też email
        this.email = this.username;
    }

    public User(String username, String password, Role role, String fullName, String email, String phone) {
        String normEmail = normalize(email);
        this.username = normEmail != null ? normEmail : normalize(username);
        this.password = password;
        this.role = role;
        this.fullName = fullName;
        this.email = this.username; // zawsze trzymamy username == email
        this.phone = phone;
    }

    // --- UserDetails ---
    @Override public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role != null ? role.name() : "ROLE_USER"));
    }
    @Override public String  getPassword()             { return password; }
    @Override public String  getUsername()             { return username; }
    @Override public boolean isAccountNonExpired()     { return true; }
    @Override public boolean isAccountNonLocked()      { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled()               { return true; }

    // --- Gettery/Settery ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    /** Utrzymujemy zasadę username == email */
    public void setUsername(String u) {
        String norm = normalize(u);
        this.username = norm;
        // jeśli nie ustawiono jeszcze emaila lub był inny – synchronizuj
        this.email = norm;
    }

    public void setPassword(String p) { this.password = p; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    /** Każde ustawienie e-maila normalizuje i synchronizuje username */
    public void setEmail(String email) {
        String norm = normalize(email);
        this.email = norm;
        this.username = norm;
    }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public Reservation getReservation() { return reservation; }
    public void setReservation(Reservation reservation) { this.reservation = reservation; }

    // --- Lifecycle: przed zapisem zawsze wyrównaj username==email i znormalizuj ---
    @PrePersist @PreUpdate
    private void syncUsernameWithEmail() {
        String normEmail = normalize(this.email);
        if (normEmail != null) {
            this.email = normEmail;
            this.username = normEmail;
        } else if (this.username != null) {
            String normUser = normalize(this.username);
            this.username = normUser;
            this.email = normUser;
        }
    }

    private static String normalize(String s) {
        return s == null ? null : s.trim().toLowerCase();
    }
}