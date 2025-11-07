// src/main/java/.../config/ApplicationConfiguration.java
package org.example.reservationsystem.config;

import org.example.reservationsystem.JWTServices.JwtAuthenticationFilter;
import org.example.reservationsystem.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.*;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Zentrale Sicherheitskonfiguration für die Anwendung.
 *
 * <p>Diese Konfiguration aktiviert Web Security, definiert die Benutzerauflösung
 * (Login-Identifier = E-Mail), setzt Passwort-Hashing (BCrypt), konfiguriert den
 * zuständigen {@link AuthenticationProvider} sowie die stateless Security-Kette
 * mit JWT-Verifizierung über {@link JwtAuthenticationFilter}.</p>
 *
 * <p>Sitzungen werden nicht serverseitig gehalten ({@link SessionCreationPolicy#STATELESS});
 * Authentifizierung erfolgt ausschließlich tokenbasiert.</p>
 *
 * <p>Autorisierungsregeln (Auszug):
 * <ul>
 *   <li>{@code /auth/register}, {@code /auth/login}, {@code /auth/auth_check} – öffentlich zugänglich</li>
 *   <li>{@code POST /api/reservations} – authentifiziert</li>
 *   <li>{@code GET /api/reservations/userReservations} – authentifiziert</li>
 *   <li>{@code DELETE /api/reservations/**} – authentifiziert</li>
 *   <li>{@code /api/reservations/all}, {@code /admin/**} – {@code ROLE_ADMIN}</li>
 *   <li>Alle übrigen Anfragen – authentifiziert</li>
 * </ul>
 * </p>
 *
 * <p>CORS ist aktiviert; CSRF ist für stateless JWT deaktiviert.</p>
 *
 * @author Maciej Janowski
 */
@Configuration
@EnableWebSecurity
public class ApplicationConfiguration {

    private final UserRepository userRepository;

    /**
     * Erstellt eine neue {@code ApplicationConfiguration}.
     *
     * @param userRepository Repository zum Laden von Benutzern anhand der E-Mail
     */
    public ApplicationConfiguration(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Liefert den {@link UserDetailsService}, der Benutzer anhand der E-Mail-Adresse lädt.
     *
     * <p>Die E-Mail wird vor der Abfrage normalisiert (trim + lowercase).</p>
     *
     * @return {@link UserDetailsService} für die Benutzerauflösung
     * @throws UsernameNotFoundException wenn kein Benutzer gefunden wird
     */
    @Bean
    public UserDetailsService userDetailsService() {
        return email -> userRepository.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    /**
     * Liefert den Passwortencoder auf Basis von BCrypt.
     *
     * @return {@link BCryptPasswordEncoder} für Hashing/Verifizierung
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    /**
     * Konfiguriert den {@link AuthenticationProvider} (DAO) mit
     * {@link #userDetailsService()} und {@link #passwordEncoder()}.
     *
     * @return konfigurierte {@link AuthenticationProvider}-Instanz
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider p = new DaoAuthenticationProvider();
        p.setUserDetailsService(userDetailsService());
        p.setPasswordEncoder(passwordEncoder());
        return p;
    }

    /**
     * Erstellt den {@link AuthenticationManager} auf Basis des konfigurierten Providers.
     *
     * @param provider verwendeter {@link AuthenticationProvider}
     * @return {@link AuthenticationManager} für Authentifizierungsoperationen
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationProvider provider) {
        return new ProviderManager(provider);
    }

    /**
     * Definiert die {@link SecurityFilterChain} inklusive CORS, CSRF, stateless Session-Policy,
     * Autorisierungsregeln und Einbindung des {@link JwtAuthenticationFilter}.
     *
     * <p>Der JWT-Filter wird vor dem {@link UsernamePasswordAuthenticationFilter} eingefügt,
     * sodass Token-basierte Authentifizierung für alle relevanten Endpunkte greift.</p>
     *
     * @param http                  HttpSecurity-Builder
     * @param jwtAuthenticationFilter Filter zur JWT-Validierung/Authentifizierung
     * @return aufgebaute {@link SecurityFilterChain}
     * @throws Exception bei Konfigurationsfehlern
     */
    @Bean
    public SecurityFilterChain securityFilterChain(
            org.springframework.security.config.annotation.web.builders.HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter
    ) throws Exception {
        return http
                .cors(cors -> cors.configure(http))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/auth/register", "/auth/login", "/auth/auth_check", "/auth/logout").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/reservations").authenticated()
                        .requestMatchers(HttpMethod.GET,  "/api/reservations/userReservations").authenticated()
                        .requestMatchers(HttpMethod.DELETE,"/api/reservations/**").authenticated()
                        .requestMatchers("/api/reservations/all").hasAuthority("ROLE_ADMIN")
                        .requestMatchers("/admin/**").hasAuthority("ROLE_ADMIN")
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex.authenticationEntryPoint((req, res, e) -> res.sendError(401, "Unauthorized")))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}