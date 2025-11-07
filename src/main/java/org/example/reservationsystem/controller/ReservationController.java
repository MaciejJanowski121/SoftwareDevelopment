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

@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    // Erstellt eine Reservierung für den aktuell authentifizierten Benutzer
    @PostMapping
    public ResponseEntity<ReservationViewDTO> createReservation(
            @Valid @RequestBody ReservationRequestDTO dto
    ) {
        String email = currentEmail(); // aus SecurityContext (Subject = E-Mail)
        Reservation reservation = new Reservation(dto.getStartTime(), dto.getEndTime());
        Reservation saved = reservationService.addReservation(reservation, dto.getTableNumber(), email);
        return ResponseEntity.ok(toDto(saved));
    }

    // Löscht eine Reservierung (Security entscheidet, wer darf)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReservation(@PathVariable Long id) {
        reservationService.deleteReservation(id);
        return ResponseEntity.noContent().build();
    }

    // Gibt die Reservierung des aktuellen Benutzers zurück oder 204, wenn keine vorhanden
    @GetMapping("/userReservations")
    public ResponseEntity<ReservationViewDTO> getUserReservation() {
        String email = currentEmail();
        Reservation r = reservationService.getUserReservation(email);
        if (r == null) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(toDto(r));
    }

    // Admin: alle Reservierungen
    @GetMapping("/all")
    public ResponseEntity<List<ReservationViewDTO>> getAllReservations() {
        List<ReservationViewDTO> all = reservationService.getAllReservations()
                .stream()
                .map(this::toDto)
                .toList();
        return ResponseEntity.ok(all);
    }

    // Öffentlicher Check freier Tische
    @GetMapping("/available")
    public ResponseEntity<List<TableViewDTO>> getAvailableTables(
            @RequestParam("start") String startIso,
            @RequestParam("minutes") Integer minutes
    ) {
        LocalDateTime start = parseIsoLenient(startIso);
        List<TableViewDTO> free = reservationService.findAvailableTables(start, minutes);
        return ResponseEntity.ok(free);
    }

    // -------- helpers --------

    // Holt die E-Mail des eingeloggten Users aus dem SecurityContext
    private String currentEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        // Bei stateless JWT + erfolgreicher Auth ist auth nie null und getName() = E-Mail
        return auth.getName();
    }

    // Tolerante ISO-Parsing-Hilfe (mit und ohne Sekunden)
    private LocalDateTime parseIsoLenient(String iso) {
        try {
            return LocalDateTime.parse(iso, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (Exception e) {
            return LocalDateTime.parse(iso + ":00", DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }
    }

    // Baut das View-DTO inkl. fullName (für schöne Anzeige im Frontend)
    private ReservationViewDTO toDto(Reservation r) {
        return new ReservationViewDTO(
                r.getId(),
                // username im DTO bleibt für Abwärtskompatibilität die E-Mail
                r.getUser() != null ? r.getUser().getEmail() : null,
                r.getUser() != null ? r.getUser().getFullName() : null,
                r.getTable() != null ? r.getTable().getTableNumber() : null,
                r.getStartTime(),
                r.getEndTime()
        );
    }
}