package org.example.reservationsystem.repository;

import org.example.reservationsystem.model.Reservation;
import org.example.reservationsystem.model.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {


    boolean existsByTable_IdAndStartTimeLessThanAndEndTimeGreaterThan(
            Long tableId,
            LocalDateTime end,
            LocalDateTime start
    );

    @EntityGraph(attributePaths = {"user", "table"})
    Optional<Reservation> findByUser_Username(String username);



}