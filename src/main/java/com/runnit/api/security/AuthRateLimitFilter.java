package com.runnit.api.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sliding-window rate limiter for auth endpoints.
 * In-memory only — sufficient for single-instance beta deployment.
 * Replace with Redis-backed solution before horizontal scaling.
 */
@Slf4j
@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {

    @Value("${auth.rate-limit.enabled:true}")
    private boolean rateLimitEnabled;

    // [maxRequests, windowSeconds] per endpoint
    private static final Map<String, int[]> LIMITS = Map.of(
        "/api/auth/login",           new int[]{10, 900},   // 10 per 15 min
        "/api/auth/register",        new int[]{5,  3600},  // 5  per hour
        "/api/auth/forgot-password", new int[]{3,  3600},  // 3  per hour
        "/api/auth/google",          new int[]{10, 900},   // 10 per 15 min
        "/api/auth/apple",           new int[]{10, 900}    // 10 per 15 min
    );

    // Sliding window: timestamps of recent requests keyed by "ip:path"
    private final ConcurrentHashMap<String, Deque<Long>> windows = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        String path = req.getServletPath();
        int[] config = LIMITS.get(path);

        if (!rateLimitEnabled || config == null || !HttpMethod.POST.matches(req.getMethod())) {
            chain.doFilter(req, res);
            return;
        }

        String ip  = resolveClientIp(req);
        String key = ip + ":" + path;
        int    limit      = config[0];
        long   windowMs   = config[1] * 1000L;
        long   now        = System.currentTimeMillis();

        Deque<Long> timestamps = windows.computeIfAbsent(key, k -> new ArrayDeque<>());

        synchronized (timestamps) {
            // Evict expired entries from the front of the window
            while (!timestamps.isEmpty() && now - timestamps.peekFirst() > windowMs) {
                timestamps.pollFirst();
            }

            if (timestamps.size() >= limit) {
                log.warn("[rate-limit] {} blocked on {} ({} requests in window)", ip, path, timestamps.size());
                res.setStatus(429);
                res.setContentType("application/json");
                res.getWriter().write("{\"error\":\"Too many requests. Please try again later.\"}");
                return;
            }

            timestamps.addLast(now);
        }

        chain.doFilter(req, res);
    }

    private String resolveClientIp(HttpServletRequest req) {
        // Render and most reverse proxies set X-Forwarded-For
        String forwarded = req.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }
}
