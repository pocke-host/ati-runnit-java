package com.runnit.api.repository;

import com.runnit.api.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByAuthProviderAndProviderId(User.AuthProvider provider, String providerId);
    boolean existsByEmail(String email);

    @org.springframework.data.jpa.repository.Query("SELECT u FROM User u WHERE LOWER(u.displayName) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%'))")
    java.util.List<User> searchByDisplayNameOrEmail(@org.springframework.data.repository.query.Param("query") String query, org.springframework.data.domain.Pageable pageable);

    // Optional<User> findByGarminAccessToken(String garminAccessToken);
    // Optional<User> findByStravaAthleteId(Long stravaAthleteId); 
}