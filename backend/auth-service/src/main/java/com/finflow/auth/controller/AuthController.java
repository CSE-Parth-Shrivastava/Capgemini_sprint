package com.finflow.auth.controller;

import com.finflow.auth.dto.*;
import com.finflow.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@Tag(name = "Auth", description = "User registration, login, and account management")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Registers a new user and immediately returns a JWT (auto-login after signup).
     * Automatically fires SIGNUP_SUCCESS in-app + email notification.
     */
    @Operation(
        summary = "Register a new user (auto-login)",
        description = "Creates a new APPLICANT account and immediately returns a JWT — no separate " +
                      "login step required. Automatically sends a SIGNUP_SUCCESS in-app notification " +
                      "and a welcome email via the notification-service."
    )
    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.ok(authService.signup(request));
    }

    /**
     * Authenticates an existing user and returns a JWT.
     * Automatically fires LOGIN_SUCCESS in-app + email notification.
     */
    @Operation(
        summary = "Login (returns JWT)",
        description = "Authenticates the user and returns a JWT token. " +
                      "Automatically sends a LOGIN_SUCCESS in-app notification and a security " +
                      "alert email via the notification-service."
    )
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @Operation(summary = "List all users", description = "Admin only.")
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserDto>> getAllUsers() {
        return ResponseEntity.ok(authService.getAllUsers());
    }

    @Operation(summary = "Activate or deactivate a user", description = "Admin only.")
    @PutMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto> updateUser(@PathVariable Long id,
                                               @RequestBody Map<String, Boolean> request) {
        return ResponseEntity.ok(authService.updateUserStatus(id, request.get("active")));
    }

    @Operation(summary = "Change a user's role", description = "Admin only. Allowed roles: APPLICANT, ADMIN.")
    @PutMapping("/users/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto> updateUserRole(@PathVariable Long id,
                                                   @RequestBody Map<String, String> request) {
        return ResponseEntity.ok(authService.updateUserRole(id, request.get("role")));
    }
}
