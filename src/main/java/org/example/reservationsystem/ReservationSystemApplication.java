package org.example.reservationsystem;

import org.example.reservationsystem.model.RestaurantTable;
import org.example.reservationsystem.model.Role;
import org.example.reservationsystem.model.User;
import org.example.reservationsystem.repository.TableRepository;
import org.example.reservationsystem.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Einstiegspunkt der Spring-Boot-Anwendung „Reservation System“.
 *
 * <p>Diese Klasse bootstrapped den Spring-Kontext und stellt einen
 * {@link CommandLineRunner} bereit, der Test-/Demo-Daten beim Start
 * initialisiert (Admin-Benutzer, Beispiel-Tische). Die Initialisierung
 * ist idempotent – Einträge werden nur erzeugt, wenn sie noch nicht existieren.</p>
 *
 * <p><strong>Hinweis (Sicherheit):</strong> Das Admin-Passwort ist hier
 * lediglich für die lokale Entwicklung gedacht. In produktiven Umgebungen
 * sollte es über Konfiguration/Secrets (z. B. Umgebungsvariablen) gesetzt
 * werden.</p>
 *
 * author Maciej Janowski
 */
@SpringBootApplication
public class ReservationSystemApplication {

    /**
     * Startet die Spring-Boot-Anwendung.
     *
     * @param args Programmargumente
     */
    public static void main(String[] args) {
        SpringApplication.run(ReservationSystemApplication.class, args);
    }

    /**
     * Initialisiert Demodaten beim Anwendungsstart.
     *
     * <p>Erstellt – falls nicht vorhanden – einen Admin-Benutzer (Login per E-Mail)
     * und eine kleine Menge an Restauranttischen. Jeder Eintrag wird nur
     * hinzugefügt, wenn er noch nicht existiert (idempotent).</p>
     *
     * @param userRepo  Repository für Benutzer
     * @param tableRepo Repository für Tische
     * @param encoder   Passwortencoder (BCrypt)
     * @return ein {@link CommandLineRunner}, der die Seed-Daten anlegt
     */
    @Bean
    public CommandLineRunner initData(UserRepository userRepo,
                                      TableRepository tableRepo,
                                      BCryptPasswordEncoder encoder) {
        return args -> {

            if (userRepo.findByEmail("admin@example.com").isEmpty()) {
                User admin = new User(
                        encoder.encode("admin123"), // nur für DEV! In PROD via Konfiguration setzen.
                        Role.ROLE_ADMIN,
                        "Administrator",
                        "admin@example.com",
                        "+49 160 0000000"
                );
                userRepo.save(admin);
                System.out.println("✅ Admin user created (email-based login).");
            }


            int[][] tables = {
                    {1, 2}, {2, 3}, {3, 4},
                    {4, 6}, {5, 2}, {6, 8}
            };

            for (int[] t : tables) {
                int tableNumber = t[0];
                int seats = t[1];
                if (tableRepo.findTableByTableNumber(tableNumber).isEmpty()) {
                    RestaurantTable table = new RestaurantTable(seats, tableNumber);
                    tableRepo.save(table);
                    System.out.println("🪑 Added table " + tableNumber + " (" + seats + " seats)");
                }
            }
        };
    }
}