package com.finflow.application.controller;

import com.finflow.application.dto.LoanApplicationRequest;
import com.finflow.application.dto.SubmitResponse;
import com.finflow.application.entity.LoanApplication;
import com.finflow.application.service.LoanApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/applications")
@Tag(name = "Loan Applications", description = "Create, update, submit and query loan applications")
public class LoanApplicationController {

    private final LoanApplicationService service;

    public LoanApplicationController(LoanApplicationService service) {
        this.service = service;
    }

    @Operation(
        summary = "Create a new loan application (DRAFT)",
        description = "Creates a loan application in DRAFT status. " +
                      "Automatically fires APPLICATION_CREATED in-app + email notification."
    )
    @PostMapping
    @PreAuthorize("hasRole('APPLICANT')")
    public ResponseEntity<LoanApplication> create(
            @Parameter(description = "Injected by API Gateway from JWT", required = true)
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody LoanApplicationRequest request) {
        return ResponseEntity.ok(service.create(userId, request));
    }

    @Operation(
        summary = "Update a DRAFT application",
        description = "Updates fields of an existing DRAFT application. Only DRAFT status applications can be modified."
    )
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('APPLICANT')")
    public ResponseEntity<LoanApplication> update(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody LoanApplicationRequest request) {
        return ResponseEntity.ok(service.update(id, userId, request));
    }

    @Operation(
        summary = "Submit a loan application",
        description = "Submits the application for review (status: DRAFT → SUBMITTED). " +
                      "Automatically fires APPLICATION_SUBMITTED notifications. " +
                      "**Response includes guided next-step:** document upload URL and list of required documents."
    )
    @PostMapping("/{id}/submit")
    @PreAuthorize("hasRole('APPLICANT')")
    public ResponseEntity<SubmitResponse> submit(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(service.submit(id, userId));
    }

    @Operation(summary = "Get my loan applications", description = "Returns all applications for the authenticated applicant.")
    @GetMapping("/my")
    @PreAuthorize("hasRole('APPLICANT')")
    public ResponseEntity<List<LoanApplication>> myApplications(
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(service.getByApplicant(userId));
    }

    @Operation(summary = "Get application status")
    @GetMapping("/{id}/status")
    public ResponseEntity<Map<String, String>> getStatus(@PathVariable Long id) {
        LoanApplication app = service.getById(id);
        return ResponseEntity.ok(Map.of("status", app.getStatus().name()));
    }

    @Operation(summary = "Get a single application by ID")
    @GetMapping("/{id}")
    public ResponseEntity<LoanApplication> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @Operation(summary = "Get all applications (Admin only)")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<LoanApplication>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }
}
