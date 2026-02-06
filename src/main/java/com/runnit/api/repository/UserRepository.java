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

    Optional<User> findByGarminAccessToken(String garminAccessToken);
    Optional<User> findByStravaAthleteId(Long stravaAthleteId); 
}