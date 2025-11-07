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

@Configuration
@EnableWebSecurity
public class ApplicationConfiguration {

    private final UserRepository userRepository;

    public ApplicationConfiguration(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Ładowanie użytkownika po e-mailu
    @Bean
    public UserDetailsService userDetailsService() {
        return email -> userRepository.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider p = new DaoAuthenticationProvider();
        p.setUserDetailsService(userDetailsService());
        p.setPasswordEncoder(passwordEncoder());
        return p;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationProvider provider) {
        return new ProviderManager(provider);
    }

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
                        .requestMatchers("/auth/register", "/auth/login", "/auth/auth_check").permitAll()
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