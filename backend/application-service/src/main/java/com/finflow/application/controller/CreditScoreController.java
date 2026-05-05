package com.finflow.application.controller;

import com.finflow.application.dto.CreditScoreRequest;
import com.finflow.application.entity.CreditScore;
import com.finflow.application.service.CreditScoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/applications")
@Tag(name = "Credit Score", description = "AI-powered credit score assessment for loan applications")
public class CreditScoreController {

    private final CreditScoreService creditScoreService;

    public CreditScoreController(CreditScoreService creditScoreService) {
        this.creditScoreService = creditScoreService;
    }

    @Operation(
        summary = "Assess credit score for a loan application",
        description = "Runs a multi-factor credit assessment and returns a CIBIL-style score (300–850) " +
                      "with sub-scores, grade, recommendation, and AI-generated narrative analysis. " +
                      "Available to both ADMIN (can assess any) and APPLICANT (own applications only)."
    )
    @PostMapping("/{id}/credit-score")
    @PreAuthorize("hasRole('ADMIN') or hasRole('APPLICANT')")
    public ResponseEntity<CreditScore> assess(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-User-Role", defaultValue = "APPLICANT") String role,
            @RequestBody CreditScoreRequest request) {
        request.setApplicationId(id);
        // Applicants get assessed with their own ID; admins can assess any
        Long applicantId = "ADMIN".equals(role) ? null : userId;
        if (applicantId == null) applicantId = userId;
        return ResponseEntity.ok(creditScoreService.assess(id, applicantId, request));
    }

    @Operation(summary = "Get latest credit score for an application")
    @GetMapping("/{id}/credit-score")
    @PreAuthorize("hasRole('ADMIN') or hasRole('APPLICANT')")
    public ResponseEntity<CreditScore> getLatest(@PathVariable Long id) {
        return creditScoreService.getLatest(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Get credit score history for an application (Admin only)")
    @GetMapping("/{id}/credit-score/history")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CreditScore>> getHistory(@PathVariable Long id) {
        return ResponseEntity.ok(creditScoreService.getHistory(id));
    }
}
