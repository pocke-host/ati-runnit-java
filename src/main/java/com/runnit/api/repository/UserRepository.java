package com.runnit.api.repository;

import com.runnit.api.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // Find a user by email (used for login or profile lookups)
    Optional<User> findByEmail(String email);

    // Check if an email already exists (used for signup validation)
    boolean existsByEmail(String email);
}
