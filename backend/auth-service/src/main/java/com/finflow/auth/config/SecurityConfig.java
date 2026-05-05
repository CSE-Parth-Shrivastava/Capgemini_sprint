package com.finflow.auth.config;

import com.finflow.auth.service.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtService jwtService;

    public SecurityConfig(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
            	    .requestMatchers("/auth/login", "/auth/signup",
            	        "/auth/v3/api-docs",
            	        "/auth/v3/api-docs/**",
            	        "/auth/swagger-ui.html",
            	        "/auth/swagger-ui/**",
            	        "/v3/api-docs/**",
            	        "/v3/api-docs.yaml",
            	        "/swagger-ui/**",
            	        "/swagger-ui.html",
            	        "/webjars/**"
            	    ).permitAll()
            	    .anyRequest().authenticated()
            	)
            .addFilterBefore(jwtFilter(), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public OncePerRequestFilter jwtFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                            FilterChain chain) throws ServletException, IOException {
                String authHeader = request.getHeader("Authorization");
                if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                    // Also check X-User-Id header (from gateway)
                    String userId = request.getHeader("X-User-Id");
                    String role = request.getHeader("X-User-Role");
                    if (userId != null && role != null) {
                        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
                        UsernamePasswordAuthenticationToken auth =
                                new UsernamePasswordAuthenticationToken(userId, null, authorities);
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    }
                    chain.doFilter(request, response);
                    return;
                }
                String token = authHeader.substring(7);
                if (jwtService.isTokenValid(token)) {
                    Claims claims = jwtService.extractClaims(token);
                    String role = claims.get("role", String.class);
                    List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(claims.getSubject(), null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
                chain.doFilter(request, response);
            }
        };
    }
}