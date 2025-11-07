package org.example.reservationsystem.repository;

import org.example.reservationsystem.model.RestaurantTable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository für {@link RestaurantTable}-Entitäten.
 *
 * <p>Erweitert {@link JpaRepository}, um Standard-CRUD-Operationen für
 * Restauranttische bereitzustellen, und ergänzt eine benutzerdefinierte
 * Methode zum Suchen eines Tisches anhand seiner Tischnummer.</p>
 *
 * <p>Wird hauptsächlich im {@code ReservationService} und {@code TableService}
 * verwendet, um Tische nach ihrer eindeutigen Tischnummer zu finden oder zu verwalten.</p>
 *
 * @author Maciej Janowski
 */
@Repository
public interface TableRepository extends JpaRepository<RestaurantTable, Long> {

    /**
     * Sucht einen Tisch anhand seiner Tischnummer.
     *
     * @param tableNumber eindeutige Tischnummer im Restaurant
     * @return Optional mit dem gefundenen {@link RestaurantTable},
     *         oder leer, falls kein Tisch mit dieser Nummer existiert
     */
    Optional<RestaurantTable> findTableByTableNumber(int tableNumber);
}