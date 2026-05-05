package com.finflow.auth.service;

import com.finflow.auth.client.NotificationClient;
import com.finflow.auth.dto.*;
import com.finflow.auth.entity.Role;
import com.finflow.auth.entity.User;
import com.finflow.auth.exception.DuplicateResourceException;
import com.finflow.auth.exception.InvalidOperationException;
import com.finflow.auth.exception.ResourceNotFoundException;
import com.finflow.auth.repository.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final NotificationClient notificationClient;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtService jwtService, NotificationClient notificationClient) {
        this.userRepository     = userRepository;
        this.passwordEncoder    = passwordEncoder;
        this.jwtService         = jwtService;
        this.notificationClient = notificationClient;
    }

    /**
     * Registers user and immediately returns a JWT (auto-login after signup).
     * Fires SIGNUP_SUCCESS notifications (in-app + email).
     */
    public AuthResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("User", "email", request.getEmail());
        }
        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhone(request.getPhone());
        user.setRole(Role.APPLICANT);
        user = userRepository.save(user);

        // Auto-login: generate token immediately after registration
        String token = jwtService.generateToken(user.getEmail(), user.getId(), user.getRole().name());

        // Trigger signup notifications (in-app + email)
        notificationClient.sendBoth(
                user.getId(), user.getEmail(),
                "Welcome to FinFlow!",
                "Hi " + user.getFullName() + ", your account has been created successfully. "
                        + "You are now logged in and can start your loan application.",
                "SIGNUP_SUCCESS", null
        );

        return new AuthResponse(token, user.getEmail(), user.getRole().name(), user.getId());
    }

    /**
     * Authenticates the user and returns a JWT.
     * Fires LOGIN_SUCCESS notifications (in-app + email).
     */
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Invalid email or password");
        }
        if (!user.isActive()) {
            throw new InvalidOperationException("Account is deactivated. Please contact support.");
        }
        String token = jwtService.generateToken(user.getEmail(), user.getId(), user.getRole().name());

        // Trigger login notifications (in-app + email)
        notificationClient.sendBoth(
                user.getId(), user.getEmail(),
                "New login to your FinFlow account",
                "Hi " + user.getFullName() + ", a new login was detected on your FinFlow account. "
                        + "If this was not you, please contact support immediately.",
                "LOGIN_SUCCESS", null
        );

        return new AuthResponse(token, user.getEmail(), user.getRole().name(), user.getId());
    }

    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    public UserDto updateUserStatus(Long userId, boolean active) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        user.setActive(active);
        return toDto(userRepository.save(user));
    }

    public UserDto updateUserRole(Long userId, String roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        try {
            user.setRole(Role.valueOf(roleName.toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid role: " + roleName + ". Allowed values: APPLICANT, ADMIN");
        }
        return toDto(userRepository.save(user));
    }

    private UserDto toDto(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setFullName(user.getFullName());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setRole(user.getRole().name());
        dto.setActive(user.isActive());
        dto.setCreatedAt(user.getCreatedAt());
        return dto;
    }
}