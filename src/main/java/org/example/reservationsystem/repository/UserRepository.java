package org.example.reservationsystem.repository;

import org.example.reservationsystem.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository für {@link User}-Entitäten.
 *
 * <p>Erweitert {@link JpaRepository}, um Standard-CRUD-Operationen
 * für Benutzer bereitzustellen, und definiert zusätzliche Methoden
 * zur Suche und Existenzprüfung anhand der E-Mail-Adresse.</p>
 *
 * <p>Dieses Repository wird in {@code AuthService} und {@code UserService}
 * verwendet, um Benutzer während der Authentifizierung und Profilverwaltung
 * zu laden oder die Eindeutigkeit der E-Mail zu prüfen.</p>
 *
 * @author Maciej Janowski
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Sucht einen Benutzer anhand seiner E-Mail-Adresse.
     *
     * <p>Die E-Mail dient als eindeutiger Login-Identifier
     * und wird serverseitig normalisiert (trim + lowercase),
     * bevor sie an diese Methode übergeben wird.</p>
     *
     * @param email E-Mail-Adresse des Benutzers (Login-Identifier)
     * @return Optional mit dem gefundenen {@link User},
     *         oder leer, falls kein Benutzer existiert
     */
    Optional<User> findByEmail(String email);

    /**
     * Prüft, ob ein Benutzer mit der angegebenen E-Mail-Adresse existiert.
     *
     * @param email E-Mail-Adresse, die überprüft werden soll
     * @return {@code true}, wenn die E-Mail bereits registriert ist,
     *         andernfalls {@code false}
     */
    boolean existsByEmail(String email);
}