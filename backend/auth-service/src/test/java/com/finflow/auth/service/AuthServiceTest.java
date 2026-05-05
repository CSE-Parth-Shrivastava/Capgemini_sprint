package com.finflow.auth.service;

import com.finflow.auth.client.NotificationClient;
import com.finflow.auth.dto.AuthResponse;
import com.finflow.auth.dto.LoginRequest;
import com.finflow.auth.dto.SignupRequest;
import com.finflow.auth.dto.UserDto;
import com.finflow.auth.entity.Role;
import com.finflow.auth.entity.User;
import com.finflow.auth.exception.DuplicateResourceException;
import com.finflow.auth.exception.InvalidOperationException;
import com.finflow.auth.exception.ResourceNotFoundException;
import com.finflow.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private NotificationClient notificationClient;

    @InjectMocks
    private AuthService authService;

    private User activeUser;
    private SignupRequest signupRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        activeUser = new User();
        activeUser.setId(1L);
        activeUser.setEmail("user@example.com");
        activeUser.setFullName("John Doe");
        activeUser.setPassword("encoded-password");
        activeUser.setRole(Role.APPLICANT);
        activeUser.setActive(true);

        signupRequest = new SignupRequest();
        signupRequest.setEmail("newuser@example.com");
        signupRequest.setFullName("New User");
        signupRequest.setPassword("SecurePass123!");
        signupRequest.setPhone("9876543210");

        loginRequest = new LoginRequest();
        loginRequest.setEmail("user@example.com");
        loginRequest.setPassword("plainPassword");
    }

    // ── signup ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("signup: creates user and returns JWT response")
    void signup_newEmail_createsUserAndReturnsAuthResponse() {
        when(userRepository.existsByEmail(signupRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(10L);
            return u;
        });
        when(jwtService.generateToken(anyString(), anyLong(), anyString())).thenReturn("jwt-token");

        AuthResponse response = authService.signup(signupRequest);

        assertAll(
            () -> assertThat(response.getToken()).isEqualTo("jwt-token"),
            () -> assertThat(response.getEmail()).isEqualTo("newuser@example.com"),
            () -> assertThat(response.getRole()).isEqualTo("APPLICANT")
        );
        verify(userRepository).save(any(User.class));
        verify(notificationClient).sendBoth(anyLong(), anyString(), anyString(), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("signup: duplicate email throws DuplicateResourceException")
    void signup_duplicateEmail_throwsDuplicateResourceException() {
        when(userRepository.existsByEmail(signupRequest.getEmail())).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> authService.signup(signupRequest));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("signup: password is encoded before saving")
    void signup_passwordIsEncoded() {
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(passwordEncoder.encode("SecurePass123!")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });
        when(jwtService.generateToken(any(), any(), any())).thenReturn("token");

        authService.signup(signupRequest);

        verify(passwordEncoder).encode("SecurePass123!");
    }

    // ── login ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("login: valid credentials return JWT response")
    void login_validCredentials_returnsAuthResponse() {
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("plainPassword", "encoded-password")).thenReturn(true);
        when(jwtService.generateToken(anyString(), anyLong(), anyString())).thenReturn("jwt-token");

        AuthResponse response = authService.login(loginRequest);

        assertThat(response.getToken()).isEqualTo("jwt-token");
        verify(notificationClient).sendBoth(anyLong(), anyString(), anyString(), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("login: unknown email throws BadCredentialsException")
    void login_unknownEmail_throwsBadCredentials() {
        when(userRepository.findByEmail(any())).thenReturn(Optional.empty());

        assertThrows(BadCredentialsException.class, () -> authService.login(loginRequest));
    }

    @Test
    @DisplayName("login: wrong password throws BadCredentialsException")
    void login_wrongPassword_throwsBadCredentials() {
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches(any(), any())).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> authService.login(loginRequest));
    }

    @Test
    @DisplayName("login: inactive account throws InvalidOperationException")
    void login_inactiveAccount_throwsInvalidOperationException() {
        activeUser.setActive(false);
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches(any(), any())).thenReturn(true);

        assertThrows(InvalidOperationException.class, () -> authService.login(loginRequest));
    }

    // ── getAllUsers ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAllUsers: returns mapped UserDto list")
    void getAllUsers_returnsMappedDtoList() {
        when(userRepository.findAll()).thenReturn(List.of(activeUser));

        List<UserDto> result = authService.getAllUsers();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEmail()).isEqualTo("user@example.com");
    }

    @Test
    @DisplayName("getAllUsers: empty repository returns empty list")
    void getAllUsers_emptyRepository_returnsEmptyList() {
        when(userRepository.findAll()).thenReturn(List.of());

        assertThat(authService.getAllUsers()).isEmpty();
    }

    // ── updateUserStatus ──────────────────────────────────────────────────────

    @Test
    @DisplayName("updateUserStatus: deactivates user successfully")
    void updateUserStatus_deactivatesUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
        when(userRepository.save(any(User.class))).thenReturn(activeUser);

        UserDto result = authService.updateUserStatus(1L, false);

        verify(userRepository).save(argThat(u -> !u.isActive()));
    }

    @Test
    @DisplayName("updateUserStatus: user not found throws ResourceNotFoundException")
    void updateUserStatus_userNotFound_throwsException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> authService.updateUserStatus(99L, true));
    }

    // ── updateUserRole ────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateUserRole: changes role to ADMIN")
    void updateUserRole_changesRoleToAdmin() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
        when(userRepository.save(any(User.class))).thenReturn(activeUser);

        authService.updateUserRole(1L, "ADMIN");

        verify(userRepository).save(argThat(u -> u.getRole() == Role.ADMIN));
    }

    @Test
    @DisplayName("updateUserRole: invalid role name throws IllegalArgumentException")
    void updateUserRole_invalidRole_throwsIllegalArgument() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));

        assertThrows(IllegalArgumentException.class, () -> authService.updateUserRole(1L, "SUPERUSER"));
    }

    @Test
    @DisplayName("updateUserRole: role name is case-insensitive")
    void updateUserRole_lowercaseRole_parsedCorrectly() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
        when(userRepository.save(any())).thenReturn(activeUser);

        assertDoesNotThrow(() -> authService.updateUserRole(1L, "admin"));
    }
}
