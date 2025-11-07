package org.example.reservationsystem.controller;

import jakarta.validation.Valid;
import org.example.reservationsystem.DTO.ReservationRequestDTO;
import org.example.reservationsystem.DTO.ReservationViewDTO;
import org.example.reservationsystem.DTO.TableViewDTO;
import org.example.reservationsystem.model.Reservation;
import org.example.reservationsystem.service.ReservationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * REST-Controller zur Verwaltung von Reservierungen im System.
 *
 * <p>Dieser Controller stellt die API-Endpunkte für das Erstellen,
 * Abrufen und Löschen von Reservierungen bereit. Darüber hinaus
 * können verfügbare Tische abgefragt werden. Der Zugriff auf bestimmte
 * Endpunkte hängt von der Authentifizierung bzw. Benutzerrolle ab
 * (z. B. {@code ROLE_ADMIN} für {@code /all}).</p>
 *
 * <p>Die Authentifizierung erfolgt über ein JWT, dessen Benutzer-E-Mail
 * als Identifier dient. Bei Aufrufen durch nicht authentifizierte Benutzer
 * wird eine HTTP-Antwort mit {@code 401 Unauthorized} zurückgegeben.</p>
 *
 * <p><strong>Basis-URL:</strong> {@code /api/reservations}</p>
 *
 * <p>CORS ist für {@code http://localhost:3000} aktiviert, um
 * Cookie-basierte Kommunikation mit dem React-Frontend zu ermöglichen.</p>
 *
 * <ul>
 *   <li>{@code POST   /api/reservations} – erstellt eine neue Reservierung für den eingeloggten Benutzer</li>
 *   <li>{@code DELETE /api/reservations/{id}} – löscht eine bestehende Reservierung</li>
 *   <li>{@code GET    /api/reservations/userReservations} – gibt die eigene Reservierung zurück</li>
 *   <li>{@code GET    /api/reservations/all} – gibt alle Reservierungen (Admin)</li>
 *   <li>{@code GET    /api/reservations/available} – gibt verfügbare Tische im Zeitraum zurück</li>
 * </ul>
 *
 * @author Maciej Janowski
 */
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    /**
     * Erstellt einen neuen {@code ReservationController}.
     *
     * @param reservationService Service für Geschäftslogik der Reservierungen
     */
    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }


    /**
     * Erstellt eine neue Reservierung für den aktuell authentifizierten Benutzer.
     *
     * <p>Falls kein Benutzer im {@link SecurityContextHolder} vorhanden ist,
     * wird {@code 401 Unauthorized} zurückgegeben.</p>
     *
     * @param dto Eingabedaten der Reservierung (Tischnummer, Start- und Endzeit)
     * @return {@link ReservationViewDTO} der erstellten Reservierung oder {@code 401}, falls keine Authentifizierung
     */
    @PostMapping
    public ResponseEntity<ReservationViewDTO> createReservation(
            @Valid @RequestBody ReservationRequestDTO dto
    ) {
        String email = currentEmailOrNull();
        if (email == null) {
            return ResponseEntity.status(401).build();
        }

        Reservation reservation = new Reservation(dto.getStartTime(), dto.getEndTime());
        Reservation saved = reservationService.addReservation(reservation, dto.getTableNumber(), email);
        return ResponseEntity.ok(toDto(saved));
    }



    /**
     * Löscht eine Reservierung anhand ihrer ID.
     *
     * @param id Primärschlüssel der Reservierung
     * @return {@code 204 No Content} nach erfolgreichem Löschen
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReservation(@PathVariable Long id) {
        reservationService.deleteReservation(id);
        return ResponseEntity.noContent().build();
    }



    /**
     * Gibt die aktuelle Reservierung des eingeloggten Benutzers zurück.
     *
     * <ul>
     *   <li>{@code 200 OK} mit {@link ReservationViewDTO}, wenn eine Reservierung existiert</li>
     *   <li>{@code 204 No Content}, wenn keine Reservierung vorhanden</li>
     *   <li>{@code 401 Unauthorized}, wenn kein Benutzer authentifiziert</li>
     * </ul>
     *
     * @return HTTP-Antwort mit passendem Statuscode und ggf. Reservierungsdaten
     */
    @GetMapping("/userReservations")
    public ResponseEntity<ReservationViewDTO> getUserReservation() {
        String email = currentEmailOrNull();
        if (email == null) {
            return ResponseEntity.status(401).build();
        }

        Reservation r = reservationService.getUserReservation(email);
        if (r == null) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(toDto(r));
    }



    /**
     * Gibt alle Reservierungen zurück (nur für Administratoren).
     *
     * <p>Dieser Endpunkt wird im Sicherheitskontext auf {@code ROLE_ADMIN} beschränkt.</p>
     *
     * @return Liste aller Reservierungen als {@link ReservationViewDTO}
     */
    @GetMapping("/all")
    public ResponseEntity<List<ReservationViewDTO>> getAllReservations() {
        List<ReservationViewDTO> all = reservationService.getAllReservations()
                .stream()
                .map(this::toDto)
                .toList();
        return ResponseEntity.ok(all);
    }



    /**
     * Gibt eine Liste verfügbarer Tische für ein bestimmtes Zeitfenster zurück.
     *
     * <p>Die Dauer wird in Minuten übergeben, die Startzeit als ISO-String.
     * Der Controller verwendet einen toleranten Parser, der Eingaben mit
     * oder ohne Sekunden akzeptiert (z. B. {@code 2025-06-09T18:00}).</p>
     *
     * @param startIso ISO-String der Startzeit
     * @param minutes  gewünschte Dauer in Minuten
     * @return Liste freier Tische als {@link TableViewDTO}
     */
    @GetMapping("/available")
    public ResponseEntity<List<TableViewDTO>> getAvailableTables(
            @RequestParam("start") String startIso,
            @RequestParam("minutes") Integer minutes
    ) {
        LocalDateTime start = parseIsoLenient(startIso);
        List<TableViewDTO> free = reservationService.findAvailableTables(start, minutes);
        return ResponseEntity.ok(free);
    }



    /**
     * Gibt die E-Mail des aktuell eingeloggten Benutzers aus dem SecurityContext zurück.
     * Falls kein Benutzer authentifiziert ist, wird {@code null} geliefert.
     *
     * @return E-Mail-Adresse oder {@code null}, falls nicht authentifiziert
     */
    private String currentEmailOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;
        String name = auth.getName();
        return (name == null || name.isBlank()) ? null : name;
    }

    /**
     * Toleranter ISO-Parser, der Datumsstrings mit oder ohne Sekunden verarbeitet.
     * Beispiel: {@code 2025-06-09T18:00} oder {@code 2025-06-09T18:00:00}.
     *
     * @param iso ISO-Datumsstring
     * @return geparstes {@link LocalDateTime}-Objekt
     */
    private LocalDateTime parseIsoLenient(String iso) {
        try {
            return LocalDateTime.parse(iso, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (Exception e) {
            return LocalDateTime.parse(iso + ":00", DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }
    }

    /**
     * Wandelt eine {@link Reservation}-Entität in ein {@link ReservationViewDTO} um,
     * um eine flache Struktur für die API-Antwort zu liefern.
     *
     * @param r Reservierungsentität
     * @return {@link ReservationViewDTO} mit Benutzerdaten, Tisch und Zeitraum
     */
    private ReservationViewDTO toDto(Reservation r) {
        return new ReservationViewDTO(
                r.getId(),
                r.getUser() != null ? r.getUser().getEmail() : null,
                r.getUser() != null ? r.getUser().getFullName() : null,
                r.getTable() != null ? r.getTable().getTableNumber() : null,
                r.getStartTime(),
                r.getEndTime()
        );
    }
}