package com.runnit.api.repository;

import com.runnit.api.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByAuthProviderAndProviderId(User.AuthProvider provider, String providerId);
    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.isMentorAvailable = true AND u.id != :excludeUserId")
    Page<User> findAvailableMentors(@Param("excludeUserId") Long excludeUserId, Pageable pageable);

    // Optional<User> findByGarminAccessToken(String garminAccessToken);
    // Optional<User> findByStravaAthleteId(Long stravaAthleteId);
}