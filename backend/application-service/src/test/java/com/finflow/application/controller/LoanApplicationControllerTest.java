package com.finflow.application.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finflow.application.config.SecurityConfig;
import com.finflow.application.dto.LoanApplicationRequest;
import com.finflow.application.dto.SubmitResponse;
import com.finflow.application.entity.ApplicationStatus;
import com.finflow.application.entity.LoanApplication;
import com.finflow.application.entity.LoanType;
import com.finflow.application.exception.ForbiddenException;
import com.finflow.application.exception.InvalidOperationException;
import com.finflow.application.exception.ResourceNotFoundException;
import com.finflow.application.service.LoanApplicationService;
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

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LoanApplicationController.class)
@Import(SecurityConfig.class)
@DisplayName("LoanApplicationController — Web Layer Tests")
class LoanApplicationControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean  private LoanApplicationService service;

    // ── POST /applications ────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /applications")
    class CreateEndpoint {

        @Test
        @DisplayName("returns 200 with DRAFT application when APPLICANT role is present")
        void create_validRequest_returnsDraft() throws Exception {
            LoanApplication app = draftApp(1L, 10L);
            when(service.create(eq(10L), org.mockito.ArgumentMatchers.any(LoanApplicationRequest.class)))
            .thenReturn(app);

            mockMvc.perform(post("/applications")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest()))
                            .header("X-User-Id",   "10")
                            .header("X-User-Role", "APPLICANT"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.status").value("DRAFT"));
        }

        @Test
        @DisplayName("returns 403 when authentication headers are absent")
        void create_noAuthHeaders_returns403() throws Exception {
            mockMvc.perform(post("/applications")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest())))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("returns 403 when role is ADMIN (endpoint requires APPLICANT)")
        void create_adminRole_returns403() throws Exception {
            mockMvc.perform(post("/applications")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest()))
                            .header("X-User-Id",   "1")
                            .header("X-User-Role", "ADMIN"))
                    .andExpect(status().isForbidden());
        }
    }

    // ── GET /applications ─────────────────────────────────────────────────────
    // NOTE: GET /applications is @PreAuthorize("hasRole('ADMIN')") only.
    //       Applicants use GET /applications/my for their own list.

    @Nested
    @DisplayName("GET /applications  (ADMIN only)")
    class GetAllEndpoint {

        @Test
        @DisplayName("ADMIN role returns all applications")
        void getAll_adminRole_returnsAllApplications() throws Exception {
            when(service.getAll()).thenReturn(List.of(draftApp(1L, 10L), draftApp(2L, 11L)));

            mockMvc.perform(get("/applications")
                            .header("X-User-Id",   "1")
                            .header("X-User-Role", "ADMIN"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)));
        }

        @Test
        @DisplayName("APPLICANT role is denied — returns 403 (ADMIN-only endpoint)")
        void getAll_applicantRole_returns403() throws Exception {
            // FIX: GET /applications is @PreAuthorize("hasRole('ADMIN')").
            // The old test expected APPLICANT to receive their own list from this endpoint,
            // but the controller routes applicants to GET /applications/my instead.
            mockMvc.perform(get("/applications")
                            .header("X-User-Id",   "10")
                            .header("X-User-Role", "APPLICANT"))
                    .andExpect(status().isForbidden());
        }
    }

    // ── GET /applications/my ──────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /applications/my  (APPLICANT only)")
    class MyApplicationsEndpoint {

        @Test
        @DisplayName("APPLICANT receives their own applications")
        void myApplications_applicantRole_returnsOwnList() throws Exception {
            when(service.getByApplicant(10L)).thenReturn(List.of(draftApp(1L, 10L)));

            mockMvc.perform(get("/applications/my")
                            .header("X-User-Id",   "10")
                            .header("X-User-Role", "APPLICANT"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].applicantId").value(10));
        }

        @Test
        @DisplayName("returns 403 when no auth headers supplied")
        void myApplications_noAuth_returns403() throws Exception {
            mockMvc.perform(get("/applications/my"))
                    .andExpect(status().isForbidden());
        }
    }

    // ── GET /applications/{id} ────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /applications/{id}")
    class GetByIdEndpoint {

        @Test
        @DisplayName("returns 200 with application body when found")
        void getById_existingId_returns200() throws Exception {
            when(service.getById(1L)).thenReturn(draftApp(1L, 10L));

            mockMvc.perform(get("/applications/1")
                            .header("X-User-Id",   "10")
                            .header("X-User-Role", "APPLICANT"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.status").value("DRAFT"));
        }

        @Test
        @DisplayName("returns 404 when application is not found")
        void getById_notFound_returns404() throws Exception {
            when(service.getById(99L))
                    .thenThrow(new ResourceNotFoundException("LoanApplication", 99L));

            mockMvc.perform(get("/applications/99")
                            .header("X-User-Id",   "10")
                            .header("X-User-Role", "APPLICANT"))
                    .andExpect(status().isNotFound());
        }
    }

    // ── GET /applications/{id}/status ─────────────────────────────────────────

    @Nested
    @DisplayName("GET /applications/{id}/status")
    class GetStatusEndpoint {

        @Test
        @DisplayName("returns 200 with status field in JSON body")
        void getStatus_existingId_returnsStatusJson() throws Exception {
            when(service.getById(1L)).thenReturn(draftApp(1L, 10L));

            mockMvc.perform(get("/applications/1/status")
                            .header("X-User-Id",   "10")
                            .header("X-User-Role", "APPLICANT"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("DRAFT"));
        }

        @Test
        @DisplayName("returns 404 when application not found")
        void getStatus_notFound_returns404() throws Exception {
            when(service.getById(99L))
                    .thenThrow(new ResourceNotFoundException("LoanApplication", 99L));

            mockMvc.perform(get("/applications/99/status")
                            .header("X-User-Id",   "10")
                            .header("X-User-Role", "APPLICANT"))
                    .andExpect(status().isNotFound());
        }
    }

    // ── PUT /applications/{id} ────────────────────────────────────────────────

    @Nested
    @DisplayName("PUT /applications/{id}")
    class UpdateEndpoint {

        @Test
        @DisplayName("returns 200 with updated application when DRAFT and owner")
        void update_draftApp_returns200() throws Exception {
            LoanApplication updated = draftApp(1L, 10L);
            when(service.update(eq(1L), eq(10L), any())).thenReturn(updated);

            mockMvc.perform(put("/applications/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest()))
                            .header("X-User-Id",   "10")
                            .header("X-User-Role", "APPLICANT"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1));
        }

        @Test
        @DisplayName("returns 422 when application is not in DRAFT status")
        void update_nonDraftApp_returns422() throws Exception {
            // FIX: InvalidOperationException is mapped to 422 UNPROCESSABLE_ENTITY by
            // GlobalExceptionHandler, NOT 409 Conflict.
            when(service.update(anyLong(), anyLong(), any()))
                    .thenThrow(new InvalidOperationException("Only DRAFT applications can be modified."));

            mockMvc.perform(put("/applications/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest()))
                            .header("X-User-Id",   "10")
                            .header("X-User-Role", "APPLICANT"))
                    .andExpect(status().isUnprocessableEntity());  // 422, not 409
        }

        @Test
        @DisplayName("returns 403 when applicant does not own the application")
        void update_wrongApplicant_returns403() throws Exception {
            when(service.update(anyLong(), anyLong(), any()))
                    .thenThrow(new ForbiddenException("You do not have access to this application."));

            mockMvc.perform(put("/applications/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest()))
                            .header("X-User-Id",   "99")
                            .header("X-User-Role", "APPLICANT"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("returns 404 when application does not exist")
        void update_notFound_returns404() throws Exception {
            when(service.update(anyLong(), anyLong(), any()))
                    .thenThrow(new ResourceNotFoundException("LoanApplication", 99L));

            mockMvc.perform(put("/applications/99")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest()))
                            .header("X-User-Id",   "10")
                            .header("X-User-Role", "APPLICANT"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("returns 403 when role is not APPLICANT")
        void update_adminRole_returns403() throws Exception {
            mockMvc.perform(put("/applications/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest()))
                            .header("X-User-Id",   "1")
                            .header("X-User-Role", "ADMIN"))
                    .andExpect(status().isForbidden());
        }
    }

    // ── POST /applications/{id}/submit ────────────────────────────────────────

    @Nested
    @DisplayName("POST /applications/{id}/submit")
    class SubmitEndpoint {

        @Test
        @DisplayName("returns 200 with SubmitResponse on successful submission")
        void submit_draftApp_returns200WithSubmitResponse() throws Exception {
            LoanApplication submitted = draftApp(1L, 10L);
            submitted.setStatus(ApplicationStatus.SUBMITTED);
            SubmitResponse response = new SubmitResponse(
                    submitted,
                    "Step 2 of 3: Please upload your supporting documents.",
                    "/documents/upload?applicationId=1",
                    List.of("IDENTITY_PROOF", "INCOME_PROOF", "ADDRESS_PROOF", "BANK_STATEMENT")
            );
            when(service.submit(1L, 10L)).thenReturn(response);

            mockMvc.perform(post("/applications/1/submit")
                            .header("X-User-Id",   "10")
                            .header("X-User-Role", "APPLICANT"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.application.status").value("SUBMITTED"))
                    .andExpect(jsonPath("$.workflowStep").value(2))
                    .andExpect(jsonPath("$.totalWorkflowSteps").value(3))
                    .andExpect(jsonPath("$.documentUploadUrl").value(containsString("applicationId=1")));
        }

        @Test
        @DisplayName("returns 422 when application is not in DRAFT status")
        void submit_nonDraftApp_returns422() throws Exception {
            // FIX: InvalidOperationException maps to 422, not 409
            when(service.submit(anyLong(), anyLong()))
                    .thenThrow(new InvalidOperationException("Only DRAFT applications can be submitted."));

            mockMvc.perform(post("/applications/1/submit")
                            .header("X-User-Id",   "10")
                            .header("X-User-Role", "APPLICANT"))
                    .andExpect(status().isUnprocessableEntity()); // 422, not 409
        }

        @Test
        @DisplayName("returns 403 when wrong applicant tries to submit")
        void submit_wrongApplicant_returns403() throws Exception {
            when(service.submit(anyLong(), anyLong()))
                    .thenThrow(new ForbiddenException("You do not have access to this application."));

            mockMvc.perform(post("/applications/1/submit")
                            .header("X-User-Id",   "99")
                            .header("X-User-Role", "APPLICANT"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("returns 404 when application does not exist")
        void submit_notFound_returns404() throws Exception {
            when(service.submit(anyLong(), anyLong()))
                    .thenThrow(new ResourceNotFoundException("LoanApplication", 99L));

            mockMvc.perform(post("/applications/99/submit")
                            .header("X-User-Id",   "10")
                            .header("X-User-Role", "APPLICANT"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("returns 403 when auth headers are missing")
        void submit_noAuth_returns403() throws Exception {
            mockMvc.perform(post("/applications/1/submit"))
                    .andExpect(status().isForbidden());
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    /**
     * FIX: LoanType.PERSONAL does not exist. Valid values: PRIVATE, BUSINESS, HOME, EDUCATION.
     * Using LoanType.HOME here.
     */
    private LoanApplication draftApp(Long id, Long applicantId) {
        LoanApplication app = new LoanApplication();
        app.setId(id);
        app.setApplicantId(applicantId);
        app.setFullName("Jane Doe");
        app.setEmail("jane@example.com");
        app.setStatus(ApplicationStatus.DRAFT);
        app.setLoanAmount(new BigDecimal("500000"));
        app.setTenureMonths(24);
        app.setLoanType(LoanType.HOME);   // FIX: was LoanType.PERSONAL (does not exist)
        return app;
    }

    private LoanApplicationRequest validRequest() {
        LoanApplicationRequest req = new LoanApplicationRequest();
        req.setFullName("Jane Doe");
        req.setEmail("jane@example.com");
        req.setPhone("9876543210");
        req.setAddress("12 Baker Street");
        req.setDateOfBirth("1990-05-15");
        req.setEmploymentType("SALARIED");
        req.setEmployerName("Acme Corp");
        req.setMonthlyIncome(new BigDecimal("80000"));
        req.setLoanType(LoanType.HOME);   // FIX: was LoanType.PERSONAL (does not exist)
        req.setLoanAmount(new BigDecimal("500000"));
        req.setTenureMonths(24);
        req.setPurpose("Home purchase");
        return req;
    }
}