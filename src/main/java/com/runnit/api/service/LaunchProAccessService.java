package com.runnit.api.service;

import com.runnit.api.model.User;
import com.runnit.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * Owns the launch pricing promise in one place.  The grant is persisted on the
 * account, so it remains active if the user later disconnects a provider or a
 * paid subscription changes state.
 */
@Service
@RequiredArgsConstructor
public class LaunchProAccessService {

    private final UserRepository userRepository;

    @Value("${runnit.launch-pro.enabled:true}")
    private boolean enabled;

    @Value("${runnit.launch-pro.limit:100}")
    private long launchLimit;

    @Value("${runnit.launch-pro.owner-email:}")
    private String ownerEmail;

    @Transactional
    public User grantIfEligible(User user) {
        if (!enabled || user == null || Boolean.TRUE.equals(user.getLaunchProGranted())) {
            return user;
        }

        if (isOwner(user) || isWithinLaunchCohort(user)) {
            user.setLaunchProGranted(true);
            return userRepository.save(user);
        }
        return user;
    }

    public boolean hasProAccess(User user) {
        if (user == null) return false;
        return Boolean.TRUE.equals(user.getLaunchProGranted())
                || "active".equalsIgnoreCase(user.getSubscriptionStatus())
                || "trialing".equalsIgnoreCase(user.getSubscriptionStatus());
    }

    private boolean isOwner(User user) {
        return ownerEmail != null && !ownerEmail.isBlank()
                && user.getEmail() != null
                && ownerEmail.trim().equalsIgnoreCase(user.getEmail().trim());
    }

    private boolean isWithinLaunchCohort(User user) {
        if (launchLimit <= 0 || user.getCreatedAt() == null || user.getId() == null) return false;
        return userRepository.countUsersThrough(user.getCreatedAt(), user.getId()) <= launchLimit;
    }
}
