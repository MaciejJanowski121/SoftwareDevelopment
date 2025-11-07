package org.example.reservationsystem.service;

import org.example.reservationsystem.model.RestaurantTable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.example.reservationsystem.repository.TableRepository;

/**
 * Service-Klasse für grundlegende CRUD-Operationen auf {@link RestaurantTable}-Entitäten.
 *
 * <p>{@code TableService} stellt eine Abstraktionsschicht zwischen Controller und
 * Datenbank bereit und ermöglicht das Hinzufügen, Abrufen und Löschen von
 * Restauranttischen. Die eigentliche Persistenzlogik wird durch das
 * {@link TableRepository} umgesetzt.</p>
 *
 * <p>Dieser Service enthält keine komplexe Geschäftslogik, da Tische im
 * Reservierungssystem statisch oder selten verändert werden. Die
 * Hauptlogik für Reservierungen befindet sich im {@link ReservationService}.</p>
 *
 * <p><strong>Typische Verwendung im Controller:</strong></p>
 * <pre>{@code
 * @GetMapping("/tables/{id}")
 * public ResponseEntity<RestaurantTable> getTable(@PathVariable Long id) {
 *     RestaurantTable table = tableService.getTable(id);
 *     return table != null ? ResponseEntity.ok(table) : ResponseEntity.notFound().build();
 * }
 * }</pre>
 *
 * @author Maciej Janowski
 */
@Service
public class TableService {

    private final TableRepository tableRepository;

    /**
     * Erstellt einen neuen {@code TableService}.
     *
     * @param tableRepository Repository für {@link RestaurantTable}-Entitäten
     */
    @Autowired
    public TableService(TableRepository tableRepository) {
        this.tableRepository = tableRepository;
    }

    /**
     * Ruft einen Tisch anhand seiner ID ab.
     *
     * @param tableId eindeutige ID des Tisches
     * @return gefundener Tisch oder {@code null}, falls nicht vorhanden
     */
    public RestaurantTable getTable(Long tableId) {
        return tableRepository.findById(tableId).orElse(null);
    }

    /**
     * Fügt einen neuen Tisch hinzu oder aktualisiert einen bestehenden.
     *
     * @param restaurantTable Tischobjekt, das gespeichert werden soll
     * @return gespeichertes {@link RestaurantTable}-Objekt
     */
    public RestaurantTable addTable(RestaurantTable restaurantTable) {
        return tableRepository.save(restaurantTable);
    }

    /**
     * Löscht einen Tisch anhand seiner ID.
     *
     * @param tableId ID des zu löschenden Tisches
     */
    public void deleteTable(Long tableId) {
        tableRepository.deleteById(tableId);
    }
}