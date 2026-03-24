package com.runnit.api.service;

import com.runnit.api.exception.UnauthorizedException;
import com.runnit.api.model.RefreshToken;
import com.runnit.api.repository.RefreshTokenRepository;
import com.runnit.api.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthService authService;
    private final JwtUtil jwtUtil;

    @Value("${jwt.refresh-expiration:2592000000}") // 30 days default
    private Long refreshExpirationMs;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Generates a cryptographically random opaque refresh token,
     * persists it, and returns the token string.
     */
    @Transactional
    public String createRefreshToken(Long userId) {
        byte[] bytes = new byte[48];
        SECURE_RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        RefreshToken entity = RefreshToken.builder()
                .userId(userId)
                .token(token)
                .expiresAt(Instant.now().plusMillis(refreshExpirationMs))
                .build();

        refreshTokenRepository.save(entity);
        log.debug("Created refresh token for userId={}", userId);
        return token;
    }

    /**
     * Validates a refresh token, rotates it (revoke old, issue new),
     * and returns a new access JWT + new refresh token.
     *
     * @return Map with keys: token (new JWT), refreshToken (new opaque token), userId
     */
    @Transactional
    public Map<String, Object> rotate(String incomingToken) {
        RefreshToken stored = refreshTokenRepository.findByToken(incomingToken)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (stored.isRevoked()) {
            // Possible token reuse — revoke entire family for this user as a precaution
            log.warn("Revoked refresh token reuse detected for userId={}. Revoking all sessions.", stored.getUserId());
            refreshTokenRepository.revokeAllByUserId(stored.getUserId());
            throw new UnauthorizedException("Refresh token has been revoked");
        }

        if (stored.getExpiresAt().isBefore(Instant.now())) {
            stored.setRevoked(true);
            refreshTokenRepository.save(stored);
            throw new UnauthorizedException("Refresh token has expired");
        }

        // Revoke the used token (rotation — each refresh token is single-use)
        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        Long userId = stored.getUserId();
        var user = authService.getUserById(userId);
        String newJwt = jwtUtil.generateToken(userId, user.getEmail());
        String newRefreshToken = createRefreshToken(userId);

        log.debug("Rotated refresh token for userId={}", userId);
        return Map.of(
                "token", newJwt,
                "refreshToken", newRefreshToken,
                "userId", userId
        );
    }

    /**
     * Revokes all refresh tokens for a user (used on logout).
     */
    @Transactional
    public void revokeAllForUser(Long userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
        log.debug("Revoked all refresh tokens for userId={}", userId);
    }

    /**
     * Revokes a single refresh token (used on logout with a specific token).
     */
    @Transactional
    public void revoke(String token) {
        refreshTokenRepository.findByToken(token).ifPresent(rt -> {
            rt.setRevoked(true);
            refreshTokenRepository.save(rt);
        });
    }
}
