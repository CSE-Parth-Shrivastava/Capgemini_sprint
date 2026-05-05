package com.finflow.application.controller;

import com.finflow.application.entity.StatusHistory;
import com.finflow.application.repository.StatusHistoryRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/applications")
@Tag(name = "Status Tracking", description = "Track the full status history of a loan application")
public class StatusHistoryController {

    private final StatusHistoryRepository statusHistoryRepository;

    public StatusHistoryController(StatusHistoryRepository statusHistoryRepository) {
        this.statusHistoryRepository = statusHistoryRepository;
    }

    @Operation(
        summary = "Get full status history for an application",
        description = "Returns an ordered list of all status transitions for the given application. " +
                      "Allows applicants to track every step of their approval journey."
    )
    @GetMapping("/{id}/status-history")
    @PreAuthorize("hasRole('ADMIN') or hasRole('APPLICANT')")
    public ResponseEntity<List<StatusHistory>> getHistory(@PathVariable Long id) {
        return ResponseEntity.ok(statusHistoryRepository.findByApplicationIdOrderByChangedAtAsc(id));
    }
}
