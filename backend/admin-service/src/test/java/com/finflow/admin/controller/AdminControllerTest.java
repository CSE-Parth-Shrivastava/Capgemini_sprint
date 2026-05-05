package com.finflow.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finflow.admin.config.SecurityConfig;
import com.finflow.admin.dto.DecisionRequest;
import com.finflow.admin.entity.Decision;
import com.finflow.admin.entity.DecisionType;
import com.finflow.admin.service.AdminService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminController.class)
@Import(SecurityConfig.class)
@DisplayName("AdminController")
class AdminControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean  private AdminService adminService;

    // ── POST /admin/applications/{id}/decision ────────────────────────────────

    @Nested
    @DisplayName("POST /admin/applications/{id}/decision")
    class MakeDecisionEndpoint {

        @Test
        @DisplayName("returns 200 with APPROVED decision for ADMIN role")
        void makeDecision_adminRole_returns200() throws Exception {
            Decision decision = decisionOf(1L, DecisionType.APPROVED);
            when(adminService.makeDecision(eq(1L), eq(5L), any()))
            .thenReturn(decision);

            mockMvc.perform(post("/admin/applications/1/decision")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(approvedRequest()))
                            .header("X-User-Id", "5")
                            .header("X-User-Role", "ADMIN"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.decision").value("APPROVED"))
                    .andExpect(jsonPath("$.applicationId").value(1));
        }

        @Test
        @DisplayName("returns 403 when APPLICANT tries to make decision")
        void makeDecision_applicantRole_returns403() throws Exception {
            mockMvc.perform(post("/admin/applications/1/decision")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(approvedRequest()))
                            .header("X-User-Id", "10")
                            .header("X-User-Role", "APPLICANT"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("returns 403 when no auth headers present")
        void makeDecision_noAuth_returns403() throws Exception {
            mockMvc.perform(post("/admin/applications/1/decision")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(approvedRequest())))
                    .andExpect(status().isForbidden());
        }
    }

    // ── GET /admin/applications/{id}/decision ─────────────────────────────────

    @Nested
    @DisplayName("GET /admin/applications/{id}/decision")
    class GetDecisionEndpoint {

        @Test
        @DisplayName("returns 200 with decision when decision exists — ADMIN role")
        void getDecision_adminRole_decisionExists_returns200() throws Exception {
            Decision d = decisionOf(1L, DecisionType.APPROVED);
            d.setRemarks("Strong financial profile.");
            when(adminService.getDecisionByApplicationId(1L)).thenReturn(Optional.of(d));

            mockMvc.perform(get("/admin/applications/1/decision")
                            .header("X-User-Id", "1")
                            .header("X-User-Role", "ADMIN"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.decision").value("APPROVED"))
                    .andExpect(jsonPath("$.remarks").value("Strong financial profile."));
        }

        @Test
        @DisplayName("returns 200 with decision when decision exists — APPLICANT role")
        void getDecision_applicantRole_decisionExists_returns200() throws Exception {
            Decision d = decisionOf(1L, DecisionType.REJECTED);
            d.setRemarks("Low credit score.");
            when(adminService.getDecisionByApplicationId(1L)).thenReturn(Optional.of(d));

            mockMvc.perform(get("/admin/applications/1/decision")
                            .header("X-User-Id", "10")
                            .header("X-User-Role", "APPLICANT"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.decision").value("REJECTED"))
                    .andExpect(jsonPath("$.remarks").value("Low credit score."));
        }

        @Test
        @DisplayName("returns 404 when no decision has been made yet")
        void getDecision_noDecision_returns404() throws Exception {
            when(adminService.getDecisionByApplicationId(99L)).thenReturn(Optional.empty());

            mockMvc.perform(get("/admin/applications/99/decision")
                            .header("X-User-Id", "10")
                            .header("X-User-Role", "APPLICANT"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("returns 403 when no auth headers present")
        void getDecision_noAuth_returns403() throws Exception {
            mockMvc.perform(get("/admin/applications/1/decision"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("returns decision with all loan terms for APPROVED application")
        void getDecision_approved_returnsAllLoanTerms() throws Exception {
            Decision d = decisionOf(1L, DecisionType.APPROVED);
            d.setApprovedAmount(new BigDecimal("500000"));
            d.setInterestRate(new BigDecimal("10.5"));
            d.setTenureMonths(24);
            when(adminService.getDecisionByApplicationId(1L)).thenReturn(Optional.of(d));

            mockMvc.perform(get("/admin/applications/1/decision")
                            .header("X-User-Id", "10")
                            .header("X-User-Role", "APPLICANT"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.approvedAmount").value(500000))
                    .andExpect(jsonPath("$.interestRate").value(10.5))
                    .andExpect(jsonPath("$.tenureMonths").value(24));
        }
    }

    // ── GET /admin/reports ────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /admin/reports")
    class ReportsEndpoint {

        @Test
        @DisplayName("returns 200 with report statistics for ADMIN")
        void getReports_adminRole_returns200() throws Exception {
            when(adminService.getReports()).thenReturn(
                    Map.of("totalDecisions", 10L, "approved", 7L,
                           "rejected", 3L, "approvalRate", "70.0%"));

            mockMvc.perform(get("/admin/reports")
                            .header("X-User-Id", "1")
                            .header("X-User-Role", "ADMIN"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalDecisions").value(10))
                    .andExpect(jsonPath("$.approvalRate").value("70.0%"));
        }

        @Test
        @DisplayName("returns 403 for APPLICANT role")
        void getReports_applicantRole_returns403() throws Exception {
            mockMvc.perform(get("/admin/reports")
                            .header("X-User-Id", "10")
                            .header("X-User-Role", "APPLICANT"))
                    .andExpect(status().isForbidden());
        }
    }

    // ── GET /admin/decisions ──────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /admin/decisions")
    class DecisionsEndpoint {

        @Test
        @DisplayName("returns 200 with all decisions for ADMIN")
        void getAllDecisions_adminRole_returns200() throws Exception {
            when(adminService.getAllDecisions()).thenReturn(List.of(
                    decisionOf(1L, DecisionType.APPROVED),
                    decisionOf(2L, DecisionType.REJECTED)
            ));

            mockMvc.perform(get("/admin/decisions")
                            .header("X-User-Id", "1")
                            .header("X-User-Role", "ADMIN"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].decision").value("APPROVED"))
                    .andExpect(jsonPath("$[1].decision").value("REJECTED"));
        }

        @Test
        @DisplayName("returns empty list when no decisions exist")
        void getAllDecisions_empty_returnsEmptyList() throws Exception {
            when(adminService.getAllDecisions()).thenReturn(List.of());

            mockMvc.perform(get("/admin/decisions")
                            .header("X-User-Id", "1")
                            .header("X-User-Role", "ADMIN"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Decision decisionOf(Long appId, DecisionType type) {
        Decision d = new Decision();
        d.setApplicationId(appId);
        d.setDecision(type);
        d.setAdminId(1L);
        d.setRemarks(type == DecisionType.REJECTED ? "Low income" : "Good profile");
        return d;
    }

    private DecisionRequest approvedRequest() {
        DecisionRequest req = new DecisionRequest();
        req.setDecision("APPROVED");
        req.setRemarks("Good profile");
        req.setApprovedAmount(new BigDecimal("500000"));
        req.setInterestRate(new BigDecimal("10.5"));
        req.setTenureMonths(24);
        return req;
    }}