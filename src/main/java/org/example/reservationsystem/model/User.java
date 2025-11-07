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

/**
 * Repräsentiert ein Benutzerkonto der Anwendung.
 *
 * <p>Die Anmeldung erfolgt ausschließlich über die eindeutige E-Mail-Adresse
 * (siehe {@link #getUsername()}), Passwörter werden gehasht gespeichert
 * (z. B. mittels BCrypt). Die Rolle des Benutzers wird über {@link Role}
 * festgelegt und in {@link #getAuthorities()} in eine Spring-Security-Authority
 * umgewandelt.</p>
 *
 * <p>Zwischen {@code User} und {@link Reservation} besteht eine 1:1-Beziehung:
 * Pro Benutzer ist maximal eine aktive Reservierung zulässig.</p>
 *
 * <p>Persistenz: Die Entität wird in der Tabelle {@code users} gespeichert.
 * Die Spalte {@code email} ist eindeutig (Unique Constraint) und dient als
 * primärer Login-Identifier.</p>
 *
 * <p>Serialisierung: {@link JsonIdentityInfo} verhindert zirkuläre Referenzen
 * bei der JSON-Ausgabe in Verbindung mit {@link Reservation}.</p>
 *
 * <p><strong>Sicherheits-Hinweis:</strong> Das Feld {@link #password} enthält
 * Hashes und darf niemals im Klartext serialisiert oder geloggt werden.</p>
 *
 * @author Maciej Janowski
 */
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
@Entity
@Table(name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_users_email", columnNames = "email")
        })
public class User implements UserDetails {

    /** Eindeutige Datenbank-ID des Benutzers. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Gehashter Passwort-Wert (z. B. BCrypt). Niemals im Klartext speichern. */
    @Column(nullable = false)
    private String password;

    /** Rolle des Benutzers zur Steuerung von Berechtigungen. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    /** Vollständiger Name der Person (Anzeigename). */
    @Column(name = "full_name", nullable = false, length = 200)
    private String fullName;

    /**
     * Primärer Login-Identifier und eindeutige Kontaktadresse.
     * <p>Wird vor Persistenz normalisiert (Trim + Kleinschreibung).</p>
     */
    @Column(name = "email", nullable = false, length = 320, unique = true)
    private String email;

    /** Optionale Telefonnummer zur Kontaktaufnahme. */
    @Column(name = "phone", length = 50)
    private String phone;

    /**
     * 1:1-Beziehung zur Benutzer-Reservierung.
     * <p>{@code orphanRemoval = true} löscht verwaiste Reservierungen automatisch,
     * wenn die Referenz am Benutzer entfernt wird.</p>
     */
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Reservation reservation;

    /** Standardkonstruktor (erforderlich für JPA). */
    public User() {}

    /**
     * Minimaler Registrierungs-Konstruktor.
     *
     * @param password gehashter Passwort-Wert (nicht leer)
     * @param role     Benutzerrolle
     * @param fullName Vollständiger Name
     * @param email    E-Mail-Adresse (Login-Identifier); wird normalisiert
     * @param phone    optionale Telefonnummer
     */
    public User(String password, Role role, String fullName, String email, String phone) {
        this.password = password;
        this.role = role;
        this.fullName = fullName;
        this.email = normalize(email);
        this.phone = phone;
    }

    // --- UserDetails ---

    /**
     * Liefert die Authorities für Spring Security auf Basis der {@link Role}.
     * Gibt standardmäßig {@code ROLE_USER} zurück, wenn {@link #role} {@code null} ist.
     */
    @Override public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role != null ? role.name() : "ROLE_USER"));
    }

    /** @return gehashter Passwort-Wert (nie Klartext) */
    @Override public String  getPassword()             { return password; }

    /**
     * Liefert den Login-Identifier des Benutzers.
     * @return E-Mail-Adresse (normalisiert) des Benutzers
     */
    @Override public String  getUsername()             { return email; } // identyfikator = e-mail

    /** Konto ist nicht abgelaufen (statisch true; Fachlogik kann dies später steuern). */
    @Override public boolean isAccountNonExpired()     { return true; }

    /** Konto ist nicht gesperrt (statisch true; Fachlogik kann dies später steuern). */
    @Override public boolean isAccountNonLocked()      { return true; }

    /** Zugangsdaten sind gültig (statisch true; Fachlogik kann dies später steuern). */
    @Override public boolean isCredentialsNonExpired() { return true; }

    /** Konto ist aktiviert (statisch true; Fachlogik kann dies später steuern). */
    @Override public boolean isEnabled()               { return true; }

    // --- Getter & Setter ---

    /** @return eindeutige ID des Benutzers */
    public Long getId() { return id; }

    /** @return aktuelle Rolle */
    public Role getRole() { return role; }

    public void setRole(Role role) { this.role = role; }

    /** @return vollständiger Name */
    public String getFullName() { return fullName; }

    public void setFullName(String fullName) { this.fullName = fullName; }

    /** @return normalisierte E-Mail-Adresse (Login) */
    public String getEmail() { return email; }

    /**
     * Setzt die E-Mail und normalisiert sie (Trim + Kleinschreibung).
     * @param email E-Mail-Adresse
     */
    public void setEmail(String email) { this.email = normalize(email); }

    /** @return optionale Telefonnummer */
    public String getPhone() { return phone; }

    public void setPhone(String phone) { this.phone = phone; }

    /** Setzt den gehashten Passwort-Wert. */
    public void setPassword(String password) { this.password = password; }

    /** @return verknüpfte Reservierung (falls vorhanden) */
    public Reservation getReservation() { return reservation; }

    public void setReservation(Reservation reservation) { this.reservation = reservation; }

    // --- Normalisierung & Lifecycle ---

    /** Normalisiert E-Mail-Adressen (Trim + Kleinschreibung). */
    private static String normalize(String s) {
        return s == null ? null : s.trim().toLowerCase();
    }

    /**
     * JPA-Lifecycle-Hook: Stellt sicher, dass die E-Mail vor dem Speichern konsistent
     * normalisiert ist.
     */
    @PrePersist @PreUpdate
    private void prePersist() {
        this.email = normalize(this.email);
    }
}