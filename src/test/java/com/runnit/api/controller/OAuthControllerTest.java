package com.runnit.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.runnit.api.security.AppleTokenValidator;
import com.runnit.api.service.AuthService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OAuthControllerTest {

    private MockMvc mockMvc;
    private RestTemplate restTemplate;
    private AppleTokenValidator appleTokenValidator;

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        appleTokenValidator = mock(AppleTokenValidator.class);
        OAuthController controller = new OAuthController(
                mock(AuthService.class), new ObjectMapper(), restTemplate, appleTokenValidator);
        ReflectionTestUtils.setField(controller, "googleClientId", "google-client");
        ReflectionTestUtils.setField(controller, "googleClientSecret", "google-secret");
        ReflectionTestUtils.setField(controller, "googleRedirectUri", "https://runnit.test/google");
        ReflectionTestUtils.setField(controller, "appleClientId", "apple-client");
        ReflectionTestUtils.setField(controller, "appleRedirectUri", "https://runnit.test/apple");
        ReflectionTestUtils.setField(controller, "frontendUrl", "https://runnit.test");
        ReflectionTestUtils.setField(controller, "jwtExpiration", 900_000L);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void googleInitiationBindsRandomStateToHttpOnlyCookie() throws Exception {
        var response = mockMvc.perform(get("/api/auth/oauth/google"))
                .andExpect(status().isFound())
                .andReturn().getResponse();

        Cookie stateCookie = response.getCookie("runnit_oauth_state");
        assertNotNull(stateCookie);
        assertTrue(stateCookie.isHttpOnly());
        assertTrue(stateCookie.getMaxAge() > 0);
        assertTrue(response.getHeader("Location").contains("state=" + stateCookie.getValue()));
    }

    @Test
    void googleCallbackRejectsMissingOrMismatchedStateBeforeTokenExchange() throws Exception {
        mockMvc.perform(get("/api/auth/oauth/google/callback")
                        .param("code", "provider-code")
                        .param("state", "wrong-state"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://runnit.test/oauth-callback?error=google_auth_failed"));

        verifyNoInteractions(restTemplate);
    }

    @Test
    void appleCallbackRejectsMissingStateBeforeTokenValidation() throws Exception {
        mockMvc.perform(post("/api/auth/oauth/apple/callback")
                        .param("code", "provider-code")
                        .param("id_token", "untrusted-token")
                        .param("state", "wrong-state"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://runnit.test/oauth-callback?error=apple_auth_failed"));

        verifyNoInteractions(appleTokenValidator);
    }
}
