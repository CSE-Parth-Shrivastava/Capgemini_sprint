package com.finflow.auth.service;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtService Unit Tests")
class JwtServiceTest {

    private JwtService jwtService;

    private static final String SECRET = "test-secret-key-which-is-at-least-32-characters-long";
    private static final long EXPIRATION = 86_400_000L;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", SECRET);
        ReflectionTestUtils.setField(jwtService, "expiration", EXPIRATION);
    }

    @Test
    @DisplayName("generateToken: creates a non-blank JWT")
    void generateToken_returnsNonBlankToken() {
        String token = jwtService.generateToken("user@example.com", 1L, "APPLICANT");

        assertThat(token).isNotBlank();
    }

    @Test
    @DisplayName("generateToken: token contains correct email claim")
    void generateToken_containsEmailClaim() {
        String token = jwtService.generateToken("user@example.com", 1L, "APPLICANT");
        Claims claims = jwtService.extractClaims(token);

        assertThat(claims.get("email", String.class)).isEqualTo("user@example.com");
    }

    @Test
    @DisplayName("generateToken: token subject matches user ID as string")
    void generateToken_subjectIsUserId() {
        String token = jwtService.generateToken("user@example.com", 42L, "ADMIN");
        Claims claims = jwtService.extractClaims(token);

        assertThat(claims.getSubject()).isEqualTo("42");
    }

    @Test
    @DisplayName("generateToken: token contains correct role claim")
    void generateToken_containsRoleClaim() {
        String token = jwtService.generateToken("admin@example.com", 5L, "ADMIN");
        Claims claims = jwtService.extractClaims(token);

        assertThat(claims.get("role", String.class)).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("isTokenValid: valid token returns true")
    void isTokenValid_validToken_returnsTrue() {
        String token = jwtService.generateToken("user@example.com", 1L, "APPLICANT");

        assertTrue(jwtService.isTokenValid(token));
    }

    @Test
    @DisplayName("isTokenValid: malformed token returns false")
    void isTokenValid_malformedToken_returnsFalse() {
        assertFalse(jwtService.isTokenValid("not.a.valid.token"));
    }

    @Test
    @DisplayName("isTokenValid: expired token returns false")
    void isTokenValid_expiredToken_returnsFalse() {
        // Set expiration to -1ms (already expired)
        ReflectionTestUtils.setField(jwtService, "expiration", -1L);
        String token = jwtService.generateToken("user@example.com", 1L, "APPLICANT");

        assertFalse(jwtService.isTokenValid(token));
    }

    @Test
    @DisplayName("extractClaims: extracts all expected fields")
    void extractClaims_returnsAllFields() {
        String token = jwtService.generateToken("test@example.com", 99L, "APPLICANT");
        Claims claims = jwtService.extractClaims(token);

        assertAll(
            () -> assertThat(claims.getSubject()).isEqualTo("99"),
            () -> assertThat(claims.get("email", String.class)).isEqualTo("test@example.com"),
            () -> assertThat(claims.get("role", String.class)).isEqualTo("APPLICANT"),
            () -> assertThat(claims.getIssuedAt()).isNotNull(),
            () -> assertThat(claims.getExpiration()).isNotNull()
        );
    }
}
