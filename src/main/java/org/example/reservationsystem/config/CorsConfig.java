package org.example.reservationsystem.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Globale CORS-Konfiguration für das Spring Boot Backend.
 *
 * <p>Diese Konfiguration erlaubt dem React-Frontend (Standard: {@code http://localhost:3000})
 * den Zugriff auf die REST-API des Backends, einschließlich Authentifizierung über Cookies
 * oder JWT-Header. Sie ist notwendig, weil Browser standardmäßig Cross-Origin-Anfragen
 * blockieren, wenn Backend und Frontend auf unterschiedlichen Ports laufen.</p>
 *
 * <p><strong>Beispiel:</strong><br>
 * Das React-Frontend läuft auf Port 3000, das Spring Boot Backend auf Port 8080 –
 * ohne diese CORS-Konfiguration würde der Browser alle Anfragen an {@code http://localhost:8080}
 * mit einem CORS-Fehler blockieren.</p>
 *
 * <p>Diese globale Konfiguration ergänzt die in {@link org.example.reservationsystem.config.ApplicationConfiguration}
 * definierte Security-Konfiguration, indem sie CORS unabhängig von der Security-Kette
 * auf Anwendungsebene erlaubt.</p>
 *
 * <p>Zugelassene Einstellungen:
 * <ul>
 *   <li>Alle Endpunkte ({@code /**})</li>
 *   <li>Erlaubte Ursprünge: {@code http://localhost:3000}</li>
 *   <li>HTTP-Methoden: GET, POST, PUT, DELETE, OPTIONS</li>
 *   <li>Alle Header-Typen ({@code *})</li>
 *   <li>Cookies und Autorisierungs-Header erlaubt ({@code allowCredentials=true})</li>
 * </ul>
 * </p>
 *
 * @author Maciej Janowski
 */
@Configuration
public class CorsConfig {

    /**
     * Erstellt den globalen {@link WebMvcConfigurer}, der CORS-Regeln definiert.
     *
     * @return konfigurierter {@link WebMvcConfigurer} mit erlaubten Ursprüngen und Methoden
     */
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins("http://localhost:3000")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true);
            }
        };
    }
}