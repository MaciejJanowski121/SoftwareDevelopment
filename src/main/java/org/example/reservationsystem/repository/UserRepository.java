package org.example.reservationsystem.repository;

import org.example.reservationsystem.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // Logujemy po e-mailu
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
