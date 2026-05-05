package com.finflow.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finflow.auth.dto.AuthResponse;
import com.finflow.auth.dto.LoginRequest;
import com.finflow.auth.dto.SignupRequest;
import com.finflow.auth.dto.UserDto;
import com.finflow.auth.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@DisplayName("AuthController Integration Tests")
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean  private AuthService authService;

    private static final AuthResponse SAMPLE_AUTH_RESPONSE =
            new AuthResponse("jwt-token", "user@example.com", "APPLICANT", 1L);

    // ── POST /auth/signup ─────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /auth/signup: valid request returns 200 with token")
    void signup_validRequest_returns200WithToken() throws Exception {
        SignupRequest req = validSignupRequest();
        when(authService.signup(any(SignupRequest.class))).thenReturn(SAMPLE_AUTH_RESPONSE);

        mockMvc.perform(post("/auth/signup")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.role").value("APPLICANT"));
    }

    @Test
    @DisplayName("POST /auth/signup: missing email returns 400")
    void signup_missingEmail_returns400() throws Exception {
        SignupRequest req = validSignupRequest();
        req.setEmail(null);

        mockMvc.perform(post("/auth/signup")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // ── POST /auth/login ──────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /auth/login: valid request returns 200 with token")
    void login_validRequest_returns200WithToken() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail("user@example.com");
        req.setPassword("Password123!");
        when(authService.login(any(LoginRequest.class))).thenReturn(SAMPLE_AUTH_RESPONSE);

        mockMvc.perform(post("/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"));
    }

    // ── GET /auth/users ───────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /auth/users: ADMIN returns user list")
    void getAllUsers_asAdmin_returnsUserList() throws Exception {
        UserDto dto = new UserDto();
        dto.setId(1L);
        dto.setEmail("user@example.com");
        when(authService.getAllUsers()).thenReturn(List.of(dto));

        mockMvc.perform(get("/auth/users").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("user@example.com"));
    }

    @Test
    @WithMockUser(roles = "APPLICANT")
    @DisplayName("GET /auth/users: APPLICANT returns 403")
    void getAllUsers_asApplicant_returns403() throws Exception {
        mockMvc.perform(get("/auth/users").with(csrf()))
                .andExpect(status().isForbidden());
    }

    // ── PUT /auth/users/{id} ──────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("PUT /auth/users/{id}: ADMIN can update user status")
    void updateUser_asAdmin_returns200() throws Exception {
        UserDto dto = new UserDto();
        dto.setId(1L);
        when(authService.updateUserStatus(eq(1L), anyBoolean())).thenReturn(dto);

        mockMvc.perform(put("/auth/users/1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("active", false))))
                .andExpect(status().isOk());
    }

    // ── PUT /auth/users/{id}/role ─────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("PUT /auth/users/{id}/role: ADMIN can change role")
    void updateUserRole_asAdmin_returns200() throws Exception {
        UserDto dto = new UserDto();
        dto.setId(1L);
        when(authService.updateUserRole(eq(1L), anyString())).thenReturn(dto);

        mockMvc.perform(put("/auth/users/1/role")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("role", "ADMIN"))))
                .andExpect(status().isOk());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private SignupRequest validSignupRequest() {
        SignupRequest req = new SignupRequest();
        req.setEmail("newuser@example.com");
        req.setFullName("New User");
        req.setPassword("SecurePass123!");
        req.setPhone("9876543210");
        return req;
    }
}
