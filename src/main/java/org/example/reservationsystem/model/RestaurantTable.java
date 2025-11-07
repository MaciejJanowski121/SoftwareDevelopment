package org.example.reservationsystem.model;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Repräsentiert einen physischen Tisch im Restaurant.
 *
 * <p>Jeder {@code RestaurantTable} besitzt eine eindeutige Tischnummer,
 * eine bestimmte Anzahl an Sitzplätzen sowie eine Liste von zugehörigen {@link Reservation}-Objekten.
 * Über die Beziehung {@code OneToMany} können mehrere Reservierungen
 * demselben Tisch zugeordnet werden.</p>
 *
 * <p>Die Entität wird von JPA verwaltet und in der Tabelle {@code restaurant_tables} gespeichert.
 * Sie verwendet {@link GenerationType#IDENTITY} für die ID-Generierung.</p>
 *
 * <p>Durch {@link JsonIdentityInfo} werden zirkuläre JSON-Referenzen
 * zwischen {@link Reservation} und {@link RestaurantTable} verhindert.</p>
 *
 *
 * @author Maciej Janowski
 */
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
@Entity
@Table(name = "restaurant_tables")
public class RestaurantTable {

    /** Eindeutige ID des Tisches (automatisch generiert). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Eindeutige Tischnummer im Restaurant. */
    @Column(name = "table_number", nullable = false, unique = true)
    private int tableNumber;

    /** Anzahl der verfügbaren Sitzplätze an diesem Tisch. */
    @Column(name = "number_of_seats", nullable = false)
    private int numberOfSeats;

    /**
     * Liste aller Reservierungen, die diesem Tisch zugeordnet sind.
     *
     * <p>Beziehung: Ein Tisch kann viele {@link Reservation}-Einträge besitzen,
     * wobei jede Reservierung genau einem Tisch zugeordnet ist.</p>
     *
     * <p>Die Option {@code orphanRemoval = true} sorgt dafür,
     * dass verwaiste Reservierungen automatisch gelöscht werden,
     * wenn sie aus der Liste entfernt werden.</p>
     */
    @OneToMany(mappedBy = "table", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Reservation> reservations = new ArrayList<>();

    /** Standardkonstruktor (erforderlich für JPA). */
    public RestaurantTable() {
    }

    /**
     * Erstellt einen neuen Tisch mit angegebener Sitzanzahl und Tischnummer.
     *
     * @param numberOfSeats Anzahl der Sitzplätze
     * @param tableNumber   eindeutige Tischnummer
     */
    public RestaurantTable(int numberOfSeats, int tableNumber) {
        this.numberOfSeats = numberOfSeats;
        this.tableNumber = tableNumber;
    }

    // --- Getter & Setter ---

    /** @return eindeutige ID des Tisches */
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    /** @return eindeutige Tischnummer */
    public int getTableNumber() {
        return tableNumber;
    }

    public void setTableNumber(int tableNumber) {
        this.tableNumber = tableNumber;
    }

    /** @return Anzahl der Sitzplätze */
    public int getNumberOfSeats() {
        return numberOfSeats;
    }

    public void setNumberOfSeats(int numberOfSeats) {
        this.numberOfSeats = numberOfSeats;
    }

    /** @return Liste aller Reservierungen für diesen Tisch */
    public List<Reservation> getReservations() {
        return reservations;
    }

    public void setReservations(List<Reservation> reservations) {
        this.reservations = reservations;
    }

    /**
     * Fügt eine neue {@link Reservation} diesem Tisch hinzu und setzt die Referenz.
     *
     * <pre>{@code
     * RestaurantTable t = new RestaurantTable(4, 7);
     * Reservation r = new Reservation(start, end);
     * t.addReservation(r);
     * }</pre>
     *
     * @param reservation die hinzuzufügende Reservierung
     */
    public void addReservation(Reservation reservation) {
        reservations.add(reservation);
        reservation.setTable(this);
    }

    /**
     * Entfernt eine {@link Reservation} aus der Liste und hebt die Tischreferenz auf.
     *
     * @param reservation die zu entfernende Reservierung
     */
    public void removeReservation(Reservation reservation) {
        reservations.remove(reservation);
        reservation.setTable(null);
    }
}