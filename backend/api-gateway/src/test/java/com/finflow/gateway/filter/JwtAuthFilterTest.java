package com.finflow.gateway.filter;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ServerWebExchange;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for JWT validation logic within JwtAuthFilter.
 * Full routing-layer integration is covered by WebFlux test-slice if needed.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthFilter — JWT Validation Logic")
class JwtAuthFilterTest {

    private static final String SECRET =
            "test-secret-key-which-is-at-least-32-characters-long";

    private JwtAuthFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthFilter();
        ReflectionTestUtils.setField(filter, "secret", SECRET);
    }

    @Test
    @DisplayName("validToken: isTokenValid returns true for fresh, signed token")
    void isTokenValid_validToken_returnsTrue() {
        String token = buildToken(System.currentTimeMillis() + 3_600_000);

        assertTrue(filter.isTokenValid(token));
    }

    @Test
    @DisplayName("expiredToken: isTokenValid returns false")
    void isTokenValid_expiredToken_returnsFalse() {
        String token = buildToken(System.currentTimeMillis() - 1_000);

        assertFalse(filter.isTokenValid(token));
    }

    @Test
    @DisplayName("malformedToken: isTokenValid returns false without throwing")
    void isTokenValid_malformedToken_returnsFalse() {
        assertFalse(filter.isTokenValid("not.a.jwt.token"));
    }

    @Test
    @DisplayName("blankToken: isTokenValid returns false")
    void isTokenValid_blankString_returnsFalse() {
        assertFalse(filter.isTokenValid(""));
    }

    @Test
    @DisplayName("extractClaim: retrieves email from valid token")
    void extractClaim_emailClaim_returnsExpectedValue() {
        String token = buildToken(System.currentTimeMillis() + 3_600_000);

        String email = filter.extractClaim(token, claims -> claims.get("email", String.class));

        assertEquals("user@example.com", email);
    }

    @Test
    @DisplayName("extractClaim: retrieves role from valid token")
    void extractClaim_roleClaim_returnsExpectedValue() {
        String token = buildToken(System.currentTimeMillis() + 3_600_000);

        String role = filter.extractClaim(token, claims -> claims.get("role", String.class));

        assertEquals("APPLICANT", role);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private String buildToken(long expirationMillis) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject("1")
                .claim("email", "user@example.com")
                .claim("role", "APPLICANT")
                .issuedAt(new Date())
                .expiration(new Date(expirationMillis))
                .signWith(key)
                .compact();
    }
}
