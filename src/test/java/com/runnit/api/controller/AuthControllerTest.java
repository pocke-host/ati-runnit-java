package com.runnit.api.controller;

import com.runnit.api.config.SecurityConfig;
import com.runnit.api.repository.FollowRepository;
import com.runnit.api.security.JwtAuthenticationFilter;
import com.runnit.api.security.JwtUtil;
import com.runnit.api.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockCookie;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Security slice tests verifying that /api/auth/** endpoints are accessible without
 * authentication (bug fix: logout was returning 403 before the permitAll wildcard fix).
 *
 * SameSite=None cookie attribute verification is in AuthCookieSameSiteTest — MockMvc's
 * MockHttpServletResponse does not render cookie attributes from jakarta.servlet.http.Cookie,
 * so that assertion requires a real Tomcat via RANDOM_PORT.
 */
@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // JwtUtil mocked so the real JwtAuthenticationFilter runs but validateToken() returns false —
    // requests reach the security chain as unauthenticated and hit the permitAll() rules
    @MockBean private JwtUtil jwtUtil;
    @MockBean private AuthService authService;
    @MockBean private FollowRepository followRepository;

    @Test
    void logout_withNoCredentials_returns200NotForbidden() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isOk());
    }

    @Test
    void logout_withAJwtCookie_returns200NotForbidden() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .cookie(new MockCookie("jwt", "header.payload.signature")))
                .andExpect(status().isOk());
    }
}
