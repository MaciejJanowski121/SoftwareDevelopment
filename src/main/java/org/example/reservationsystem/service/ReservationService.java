// src/main/java/org/example/reservationsystem/service/ReservationService.java
package org.example.reservationsystem.service;

import jakarta.persistence.EntityNotFoundException;
import org.example.reservationsystem.DTO.TableViewDTO;
import org.example.reservationsystem.exceptions.*;
import org.example.reservationsystem.model.Reservation;
import org.example.reservationsystem.model.RestaurantTable;
import org.example.reservationsystem.model.User;
import org.example.reservationsystem.repository.ReservationRepository;
import org.example.reservationsystem.repository.TableRepository;
import org.example.reservationsystem.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class ReservationService {

    private static final Duration MIN_DURATION     = Duration.ofMinutes(30);
    private static final Duration MAX_DURATION     = Duration.ofHours(5);
    private static final Duration DEFAULT_DURATION = Duration.ofHours(2);

    private static final int MIN_MINUTES = 30;
    private static final int MAX_MINUTES = 300;

    private final ReservationRepository reservationRepository;
    private final TableRepository tableRepository;
    private final UserRepository userRepository;

    public ReservationService(ReservationRepository reservationRepository,
                              TableRepository tableRepository,
                              UserRepository userRepository) {
        this.reservationRepository = reservationRepository;
        this.tableRepository = tableRepository;
        this.userRepository = userRepository;
    }

    /** Tworzy rezerwację dla użytkownika identyfikowanego e-mailem. */
    public Reservation addReservation(Reservation reservation, int tableNumber, String email) {
        User user = userRepository.findByEmail(normalize(email))
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        // 1:1 – dedykowany wyjątek (ładny komunikat i własny type w ProblemDetail)
        if (user.getReservation() != null) {
            throw new UserAlreadyHasReservationException();
        }

        RestaurantTable table = tableRepository.findTableByTableNumber(tableNumber)
                .orElseThrow(() -> new TableNotFoundException("Table with number " + tableNumber + " does not exist."));

        // endTime domyślnie start + 2h
        if (reservation.getStartTime() != null && reservation.getEndTime() == null) {
            reservation.setEndTime(reservation.getStartTime().plus(DEFAULT_DURATION));
        }

        validateReservationInput(reservation);

        // kolizja slotu – dedykowany wyjątek
        boolean overlaps = reservationRepository
                .existsByTable_IdAndStartTimeLessThanAndEndTimeGreaterThan(
                        table.getId(),
                        reservation.getEndTime(),
                        reservation.getStartTime()
                );
        if (overlaps) {
            throw new TableAlreadyReservedException(tableNumber);
        }

        // powiązania
        reservation.setUser(user);
        reservation.setTable(table);
        user.setReservation(reservation);
        if (table.getReservations() != null) {
            table.getReservations().add(reservation);
        }

        Reservation saved = reservationRepository.save(reservation);
        userRepository.save(user);
        return saved;
    }

    /** Zwraca rezerwację użytkownika lub `null`, jeśli jej nie ma (kontroler zwróci 204). */
    @Transactional(readOnly = true)
    public Reservation getUserReservation(String email) {
        return reservationRepository.findByUser_Email(normalize(email)).orElse(null);
    }

    public void deleteReservation(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Reservation not found"));

        RestaurantTable table = reservation.getTable();
        if (table != null && table.getReservations() != null) {
            table.getReservations().remove(reservation);
        }
        reservation.setTable(null);

        User user = reservation.getUser();
        if (user != null) {
            user.setReservation(null);
            reservation.setUser(null);
            userRepository.save(user);
        }

        reservationRepository.delete(reservation);
    }

    @Transactional(readOnly = true)
    public List<Reservation> getAllReservations() {
        return reservationRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<TableViewDTO> findAvailableTables(LocalDateTime start, Integer minutes) {
        int clamped = clampMinutes(minutes);
        LocalDateTime end = start.plusMinutes(clamped);

        return tableRepository.findAll().stream()
                .filter(table -> !reservationRepository
                        .existsByTable_IdAndStartTimeLessThanAndEndTimeGreaterThan(
                                table.getId(), end, start))
                .map(t -> new TableViewDTO(t.getId(), t.getTableNumber(), t.getNumberOfSeats()))
                .toList();
    }

    // ---- helpers ----

    private static int clampMinutes(Integer minutes) {
        if (minutes == null) return MIN_MINUTES;
        if (minutes < MIN_MINUTES) return MIN_MINUTES;
        if (minutes > MAX_MINUTES) return MAX_MINUTES;
        return minutes;
    }

    private void validateReservationInput(Reservation r) {
        if (r == null) throw new IllegalArgumentException("Reservation cannot be null.");
        if (r.getStartTime() == null) throw new IllegalArgumentException("Reservation must have start time.");
        if (r.getEndTime() == null) throw new IllegalArgumentException("Reservation must have end time.");
        if (!r.getEndTime().isAfter(r.getStartTime())) {
            throw new IllegalArgumentException("End time must be after start time.");
        }
        LocalDateTime now = LocalDateTime.now();
        if (r.getStartTime().isBefore(now)) {
            throw new IllegalArgumentException("Reservation start time cannot be in the past.");
        }
        long minutes = Duration.between(r.getStartTime(), r.getEndTime()).toMinutes();
        if (minutes < MIN_DURATION.toMinutes() || minutes > MAX_DURATION.toMinutes()) {
            throw new IllegalArgumentException("Reservation must be between 30 minutes and 5 hours.");
        }
        LocalDateTime latestAllowed = r.getStartTime().toLocalDate().atTime(22, 0);
        if (r.getEndTime().isAfter(latestAllowed)) {
            throw new IllegalArgumentException("Reservations are only allowed until 22:00.");
        }
    }

    private static String normalize(String s) {
        return s == null ? null : s.trim().toLowerCase();
    }
}