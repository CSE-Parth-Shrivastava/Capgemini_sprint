package com.finflow.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

/**
 * JWT authentication filter for the Spring Cloud Gateway.
 *
 * <p>Validates the {@code Authorization: Bearer <token>} header on all
 * non-public routes. On success, it injects {@code X-User-Id} and
 * {@code X-User-Role} headers for downstream services to consume.
 *
 * <p>Open (unauthenticated) endpoints are defined in {@code OPEN_ENDPOINTS}.
 */
@Component
public class JwtAuthFilter extends AbstractGatewayFilterFactory<JwtAuthFilter.Config> {

    private static final List<String> OPEN_ENDPOINTS = List.of(
            "/auth/login", "/auth/signup", "/gateway/auth/login", "/gateway/auth/signup"
    );

    private static final List<String> SWAGGER_PATHS = List.of(
            "/v3/api-docs", "/swagger-ui", "/swagger-ui.html"
    );

    @Value("${jwt.secret}")
    String secret; // package-private for testability

    public JwtAuthFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String path = exchange.getRequest().getURI().getPath();

            if (isOpenPath(path)) {
                return chain.filter(exchange);
            }

            String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return unauthorized(exchange);
            }

            String token = authHeader.substring(7);
            if (!isTokenValid(token)) {
                return unauthorized(exchange);
            }

            String userId = extractClaim(token, Claims::getSubject);
            String role   = extractClaim(token, c -> c.get("role", String.class));

            ServerWebExchange mutated = exchange.mutate()
                    .request(r -> r.header("X-User-Id", userId).header("X-User-Role", role))
                    .build();

            return chain.filter(mutated);
        };
    }

    /**
     * Validates that the token is signed correctly and has not expired.
     *
     * @param token the raw JWT string
     * @return {@code true} if the token is valid and unexpired
     */
    public boolean isTokenValid(String token) {
        try {
            Claims claims = parseClaims(token);
            return !claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Extracts a single claim from the token using the provided resolver function.
     *
     * @param <T>           the return type of the claim
     * @param token         the raw JWT string
     * @param claimsResolver a function that maps {@link Claims} to the desired value
     * @return the resolved claim value
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        return claimsResolver.apply(parseClaims(token));
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private Claims parseClaims(String token) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token).getPayload();
    }

    private boolean isOpenPath(String path) {
        if ("OPTIONS".equals(path)) {
            return true;
        }
        boolean isSwagger = SWAGGER_PATHS.stream().anyMatch(path::contains);
        boolean isOpen    = OPEN_ENDPOINTS.stream().anyMatch(path::contains);
        return isSwagger || isOpen;
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    /** Gateway filter configuration (currently empty — routes are configured in YAML). */
    public static class Config {}
}
