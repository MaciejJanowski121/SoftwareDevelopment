package org.example.reservationsystem.repository;

import org.example.reservationsystem.model.Reservation;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    boolean existsByTable_IdAndStartTimeLessThanAndEndTimeGreaterThan(
            Long tableId,
            LocalDateTime end,
            LocalDateTime start
    );

    // Ładuje rezerwację razem z użytkownikiem i stołem po e-mailu użytkownika
    @EntityGraph(attributePaths = {"user", "table"})
    Optional<Reservation> findByUser_Email(String email);
}