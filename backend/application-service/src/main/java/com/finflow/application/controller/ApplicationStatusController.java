package com.finflow.application.controller;

import com.finflow.application.entity.ApplicationStatus;
import com.finflow.application.entity.LoanApplication;
import com.finflow.application.service.LoanApplicationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/applications")
public class ApplicationStatusController {

    private final LoanApplicationService service;

    public ApplicationStatusController(LoanApplicationService service) {
        this.service = service;
    }

    // Only admins (or internal service calls from admin-service) may update status
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LoanApplication> updateStatus(@PathVariable Long id,
                                                         @RequestBody Map<String, String> request) {
        ApplicationStatus status = ApplicationStatus.valueOf(request.get("status").toUpperCase());
        String remarks = request.get("remarks");
        return ResponseEntity.ok(service.updateStatus(id, status, remarks));
    }
}