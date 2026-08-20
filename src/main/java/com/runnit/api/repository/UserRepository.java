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

    java.util.List<User> findByRole(String role);

    Optional<User> findByStripeCustomerId(String stripeCustomerId);
    Optional<User> findByStravaAthleteId(Long stravaAthleteId);
    Optional<User> findByStravaOauthState(String state);
    Optional<User> findByGarminRequestToken(String requestToken);
    Optional<User> findByGarminAccessToken(String accessToken);
    Optional<User> findByCorosOauthState(String state);
    Optional<User> findByCorosUserId(String corosUserId);
    Optional<User> findByGoogleCalendarOauthState(String state);
    Optional<User> findByWhoopOauthState(String state);
    Optional<User> findByWhoopUserId(Long whoopUserId);

    @org.springframework.data.jpa.repository.Query(
        "SELECT u FROM User u WHERE u.whoopAccessToken IS NOT NULL " +
        "AND (u.whoopLastSync IS NULL OR u.whoopLastSync < :cutoff)"
    )
    java.util.List<User> findWhoopConnectedUsersNeedingSync(
        @org.springframework.data.repository.query.Param("cutoff") java.time.Instant cutoff
    );
    Optional<User> findByInviteCode(String inviteCode);

    @org.springframework.data.jpa.repository.Query("SELECT u FROM User u WHERE LOWER(u.email) IN :emails")
    java.util.List<User> findByEmailInIgnoreCase(@org.springframework.data.repository.query.Param("emails") java.util.List<String> emails);

    java.util.List<User> findByLocationIgnoreCase(String location);

    java.util.List<User> findTop50ByIsPublicTrueOrderByCreatedAtDesc();
}