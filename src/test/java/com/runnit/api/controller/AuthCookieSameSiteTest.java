package com.runnit.api.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests against real embedded Tomcat (RANDOM_PORT) to verify that
 * the JWT cookie carries SameSite=None. MockMvc's MockHttpServletResponse does not
 * render cookie attributes from jakarta.servlet.http.Cookie, so these assertions
 * require going through the real Tomcat cookie serialization path.
 *
 * These tests use a real H2 database with Hibernate create-drop.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthCookieSameSiteTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void logout_setCookieHeader_containsSameSiteNoneAndMaxAgeZero() {
        ResponseEntity<Map> response = restTemplate.postForEntity("/api/auth/logout", null, Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        List<String> setCookieHeaders = response.getHeaders().get(HttpHeaders.SET_COOKIE);
        assertNotNull(setCookieHeaders, "Set-Cookie must be present on logout");

        String jwtClear = setCookieHeaders.stream()
                .filter(h -> h.startsWith("jwt="))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No jwt cookie in Set-Cookie headers: " + setCookieHeaders));

        assertTrue(jwtClear.contains("Max-Age=0"),
                "Logout cookie must expire the jwt. Got: " + jwtClear);
        assertTrue(jwtClear.contains("SameSite=None"),
                "Logout clear-cookie must be SameSite=None or browser won't delete the original. Got: " + jwtClear);
    }

    @Test
    void register_setCookieHeader_containsSameSiteNone() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>("""
                {"email":"samesite_reg@example.com","password":"SecurePass1!","displayName":"SameSite Reg","role":"athlete"}
                """, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity("/api/auth/register", request, Map.class);
        assertEquals(HttpStatus.CREATED, response.getStatusCode(), "Register should return 201");

        String jwtCookie = getJwtSetCookieHeader(response);
        assertTrue(jwtCookie.contains("SameSite=None"),
                "JWT cookie must be SameSite=None for cross-origin AJAX (runnit.live → onrender.com). Got: " + jwtCookie);
        assertTrue(jwtCookie.contains("HttpOnly"),
                "JWT cookie must be HttpOnly. Got: " + jwtCookie);
        assertTrue(jwtCookie.contains("Secure"),
                "JWT cookie must be Secure. Got: " + jwtCookie);
    }

    @Test
    void login_setCookieHeader_containsSameSiteNone() {
        // Register first
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        restTemplate.postForEntity("/api/auth/register",
                new HttpEntity<>("""
                        {"email":"samesite_login@example.com","password":"SecurePass1!","displayName":"SameSite Login","role":"athlete"}
                        """, headers),
                Map.class);

        // Login
        ResponseEntity<Map> response = restTemplate.postForEntity("/api/auth/login",
                new HttpEntity<>("""
                        {"email":"samesite_login@example.com","password":"SecurePass1!"}
                        """, headers),
                Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode(), "Login should return 200");

        String jwtCookie = getJwtSetCookieHeader(response);
        assertTrue(jwtCookie.contains("SameSite=None"),
                "JWT cookie must be SameSite=None for cross-origin AJAX. Got: " + jwtCookie);
    }

    private String getJwtSetCookieHeader(ResponseEntity<?> response) {
        List<String> headers = response.getHeaders().get(HttpHeaders.SET_COOKIE);
        assertNotNull(headers, "Set-Cookie header must be present");
        return headers.stream()
                .filter(h -> h.startsWith("jwt="))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No jwt cookie in Set-Cookie: " + headers));
    }
}
