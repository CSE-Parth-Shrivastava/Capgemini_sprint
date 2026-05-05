package com.finflow.document.config;

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
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    // FIX: The gateway strips the /documents prefix before forwarding,
                    // so this service only ever sees /v3/api-docs — not /documents/v3/api-docs.
                    "/v3/api-docs",
                    "/v3/api-docs/**",
                    "/swagger-ui.html",
                    "/swagger-ui/**",
                    "/webjars/**"
                ).permitAll()
                .anyRequest().authenticated())
            .addFilterBefore(gatewayHeaderFilter(), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public OncePerRequestFilter gatewayHeaderFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                            FilterChain chain) throws ServletException, IOException {
                String userId = request.getHeader("X-User-Id");
                String role = request.getHeader("X-User-Role");
                if (userId != null && role != null) {
                    List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(userId, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
                chain.doFilter(request, response);
            }
        };
    }
}