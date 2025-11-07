package org.example.reservationsystem.model;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Repräsentiert eine einzelne Tischreservierung in einem Restaurant.
 *
 * <p>Eine {@code Reservation} enthält den Zeitraum (Start- und Endzeitpunkt),
 * den zugewiesenen {@link RestaurantTable} sowie den {@link User}, der die Reservierung vorgenommen hat.
 * Jeder Benutzer kann zu einem Zeitpunkt nur eine aktive Reservierung besitzen.</p>
 *
 * <p>Die Entität wird von JPA verwaltet und in der Tabelle {@code reservations} gespeichert.
 * Sie verwendet {@link GenerationType#IDENTITY} für die Primärschlüsselgenerierung.</p>
 *
 * <p>Serialisierungs-Hinweis: Durch {@link JsonIdentityInfo} wird eine zirkuläre
 * JSON-Referenz zwischen {@code Reservation}, {@code User} und {@code RestaurantTable} vermieden.</p>
 *
 *
 * @author Maciej Janowski
 */
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
@Entity
@Table(name = "reservations")
public class Reservation {

    /** Eindeutige ID der Reservierung (automatisch generiert). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Beginn der Reservierung (muss innerhalb der Öffnungszeiten liegen). */
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    /** Ende der Reservierung. Darf nicht vor {@link #startTime} liegen. */
    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    /**
     * Der Tisch, der für die Reservierung zugewiesen wurde.
     * <p>Beziehung: Viele Reservierungen können demselben Tisch zugeordnet sein.</p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "table_id", nullable = false)
    private RestaurantTable table;

    /**
     * Der Benutzer, der die Reservierung erstellt hat.
     * <p>Beziehung: Ein Benutzer kann immer nur eine aktive Reservierung haben.</p>
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    /** Standardkonstruktor (erforderlich für JPA). */
    public Reservation() {}

    /**
     * Erstellt eine neue Reservierung mit Start- und Endzeit.
     *
     * @param startTime Startzeitpunkt der Reservierung
     * @param endTime Endzeitpunkt der Reservierung
     */
    public Reservation(LocalDateTime startTime, LocalDateTime endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
    }

    // --- Getter & Setter ---

    /** @return eindeutige ID der Reservierung */
    public Long getId() { return id; }

    public void setId(Long id) { this.id = id; }

    /** @return Startzeitpunkt der Reservierung */
    public LocalDateTime getStartTime() { return startTime; }

    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    /** @return Endzeitpunkt der Reservierung */
    public LocalDateTime getEndTime() { return endTime; }

    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    /** @return zugewiesener Tisch */
    public RestaurantTable getTable() { return table; }

    public void setTable(RestaurantTable table) { this.table = table; }

    /** @return Benutzer, der die Reservierung erstellt hat */
    public User getUser() { return user; }

    public void setUser(User user) { this.user = user; }

    // --- equals & hashCode ---

    /**
     * Zwei {@code Reservation}-Objekte gelten als gleich,
     * wenn sie dieselbe Datenbank-ID besitzen.
     *
     * @param o zu vergleichendes Objekt
     * @return {@code true}, wenn beide dieselbe ID haben
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Reservation other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}