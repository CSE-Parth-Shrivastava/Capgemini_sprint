package com.finflow.admin.controller;

import com.finflow.admin.dto.DecisionRequest;
import com.finflow.admin.entity.Decision;
import com.finflow.admin.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@Tag(name = "Admin", description = "Admin decision-making on loan applications.")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    // ── Decision endpoints ────────────────────────────────────────────────────

    @Operation(
        summary = "Approve or Reject a loan application",
        description = "Records the admin's decision (APPROVED / REJECTED), syncs status to " +
                      "application-service, and fires IN_APP + EMAIL notifications to the applicant."
    )
    @PostMapping("/applications/{id}/decision")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Decision> makeDecision(
            @Parameter(description = "Loan application ID", required = true)
            @PathVariable Long id,
            @Parameter(description = "Injected by API Gateway from JWT", required = true)
            @RequestHeader("X-User-Id") Long adminId,
            @RequestBody DecisionRequest request) {
        return ResponseEntity.ok(adminService.makeDecision(id, adminId, request));
    }

    /**
     * Returns the decision for a given application.
     * Accessible to ADMIN (review) and APPLICANT (view own decision + remarks).
     * Returns 404 if no decision has been recorded yet.
     */
    @Operation(
        summary = "Get the decision for a specific application",
        description = "Returns the admin decision record including remarks, approved amount, " +
                      "interest rate, tenure, and decision date. Returns 404 if no decision exists yet."
    )
    @GetMapping("/applications/{id}/decision")
    @PreAuthorize("hasAnyRole('ADMIN','APPLICANT')")
    public ResponseEntity<Decision> getDecisionByApplication(
            @Parameter(description = "Loan application ID", required = true)
            @PathVariable Long id) {
        return adminService.getDecisionByApplicationId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ── Reports & listing ─────────────────────────────────────────────────────

    @Operation(summary = "Decision statistics report")
    @GetMapping("/reports")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getReports() {
        return ResponseEntity.ok(adminService.getReports());
    }

    @Operation(summary = "List all decisions made by all admins")
    @GetMapping("/decisions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Decision>> getAllDecisions() {
        return ResponseEntity.ok(adminService.getAllDecisions());
    }
}