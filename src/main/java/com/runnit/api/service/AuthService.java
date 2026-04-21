package com.runnit.api.service;

import com.runnit.api.exception.BadRequestException;
import com.runnit.api.exception.ConflictException;
import com.runnit.api.exception.ResourceNotFoundException;
import com.runnit.api.exception.UnauthorizedException;
import com.runnit.api.model.PasswordResetToken;
import com.runnit.api.model.User;
import com.runnit.api.repository.PasswordResetTokenRepository;
import com.runnit.api.repository.UserRepository;
import com.runnit.api.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;

    @Value("${app.password-reset.expiry-minutes:60}")
    private int resetExpiryMinutes;
    
    @Transactional
    public Map<String, Object> registerWithEmail(String email, String password, String displayName, String role) {
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email already exists");
        }

        String safeRole = (role != null && role.equals("coach")) ? "coach" : "athlete";

        User user = User.builder()
                .email(email)
                .displayName(displayName)
                .user(displayName)
                .authProvider(User.AuthProvider.EMAIL)
                .passwordHash(passwordEncoder.encode(password))
                .role(safeRole)
                .build();
        
        user = userRepository.save(user);
        String token = jwtUtil.generateToken(user.getId(), user.getEmail());
        
        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("user", userToMap(user));
        return response;
    }
    
    public Map<String, Object> loginWithEmail(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid credentials");
        }
        
        String token = jwtUtil.generateToken(user.getId(), user.getEmail());
        
        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("user", userToMap(user));
        return response;
    }
    
    @Transactional
    public Map<String, Object> handleOAuthLogin(User.AuthProvider provider, String providerId,
                                                 String email, String displayName, String avatarUrl) {
        User user = userRepository.findByAuthProviderAndProviderId(provider, providerId)
                .orElseGet(() -> {
                    // If the same email exists (registered via email before), link provider to that account
                    if (email != null) {
                        Optional<User> existingByEmail = userRepository.findByEmail(email);
                        if (existingByEmail.isPresent()) {
                            User existing = existingByEmail.get();
                            existing.setAuthProvider(provider);
                            existing.setProviderId(providerId);
                            if (avatarUrl != null && existing.getAvatarUrl() == null) {
                                existing.setAvatarUrl(avatarUrl);
                            }
                            return userRepository.save(existing);
                        }
                    }
                    // Brand-new user via OAuth
                    return userRepository.save(User.builder()
                            .email(email)
                            .displayName(displayName != null ? displayName : email)
                            .avatarUrl(avatarUrl)
                            .authProvider(provider)
                            .providerId(providerId)
                            .role("athlete")
                            .build());
                });
        
        String token = jwtUtil.generateToken(user.getId(), user.getEmail());
        
        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("user", userToMap(user));
        return response;
    }
    
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    /**
     * Generates a password-reset token and emails the link to the user.
     * Always returns success (even if email not found) to prevent user enumeration.
     */
    @Transactional
    public void requestPasswordReset(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            log.info("[auth] Password reset requested for unknown email: {}", email);
            return; // silent — don't leak whether the email exists
        }
        User user = userOpt.get();

        // Invalidate any existing tokens for this user
        passwordResetTokenRepository.deleteByUserId(user.getId());

        String token = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plusSeconds(resetExpiryMinutes * 60L);
        passwordResetTokenRepository.save(new PasswordResetToken(user.getId(), token, expiresAt));

        emailService.sendPasswordReset(email, token);
        log.info("[auth] Password reset token issued for userId={}", user.getId());
    }

    /**
     * Validates the reset token and updates the user's password.
     */
    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new BadRequestException("Invalid or expired reset link"));

        if (resetToken.isUsed()) {
            throw new BadRequestException("This reset link has already been used");
        }
        if (resetToken.getExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("This reset link has expired — please request a new one");
        }
        if (newPassword == null || newPassword.length() < 8) {
            throw new BadRequestException("Password must be at least 8 characters");
        }

        User user = userRepository.findById(resetToken.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);
        log.info("[auth] Password reset complete for userId={}", user.getId());
    }
    
    private Map<String, Object> userToMap(User user) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", user.getId());
        map.put("email", user.getEmail() != null ? user.getEmail() : "");
        map.put("displayName", user.getDisplayName());
        map.put("avatarUrl", user.getAvatarUrl() != null ? user.getAvatarUrl() : "");
        return map;
    }
}