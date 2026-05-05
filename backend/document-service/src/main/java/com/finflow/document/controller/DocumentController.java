package com.finflow.document.controller;

import com.finflow.document.entity.Document;
import com.finflow.document.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/documents")
@Tag(name = "Documents", description = "Upload and manage supporting documents for loan applications. " +
        "Every upload automatically triggers a DOCUMENT_UPLOADED in-app notification.")
public class DocumentController {

    private final DocumentService service;

    public DocumentController(DocumentService service) {
        this.service = service;
    }

    @Operation(
        summary = "Upload a supporting document",
        description = "Uploads a document (PDF/JPEG/PNG/WEBP, max 10 MB) for a given loan application. " +
                      "**Workflow step 2 of 3** — call this after POST /applications/{id}/submit. " +
                      "Automatically fires a DOCUMENT_UPLOADED in-app notification. " +
                      "Required document types: IDENTITY_PROOF, INCOME_PROOF, ADDRESS_PROOF, BANK_STATEMENT."
    )
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('APPLICANT')")
    public ResponseEntity<Document> upload(
            @Parameter(description = "Injected by API Gateway from JWT", required = true)
            @RequestHeader("X-User-Id") Long userId,
            @Parameter(description = "ID of the submitted loan application", required = true)
            @RequestParam Long applicationId,
            @Parameter(description = "Type of document, e.g. IDENTITY_PROOF, INCOME_PROOF, ADDRESS_PROOF, BANK_STATEMENT",
                       required = true)
            @RequestParam String documentType,
            @Parameter(description = "File to upload (PDF, JPEG, PNG or WEBP, max 10 MB)", required = true)
            @RequestParam MultipartFile file) throws IOException {
        return ResponseEntity.ok(service.upload(userId, applicationId, documentType, file));
    }

    @Operation(summary = "Get all documents for an application")
    @GetMapping("/application/{applicationId}")
    public ResponseEntity<List<Document>> getByApplication(@PathVariable Long applicationId) {
        return ResponseEntity.ok(service.getByApplication(applicationId));
    }

    @Operation(summary = "Get my uploaded documents")
    @GetMapping("/my")
    @PreAuthorize("hasRole('APPLICANT')")
    public ResponseEntity<List<Document>> myDocuments(
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(service.getByApplicant(userId));
    }

    @Operation(summary = "Get all documents pending verification (Admin only)")
    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Document>> getPending() {
        return ResponseEntity.ok(service.getPendingDocuments());
    }

    @Operation(
        summary = "Verify or reject a document (Admin only)",
        description = "Sets the document status to VERIFIED or REJECTED and fires a " +
                      "DOCUMENT_VERIFIED / DOCUMENT_REJECTED in-app notification to the applicant."
    )
    @PutMapping("/{id}/verify")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Document> verify(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") String adminId,
            @RequestBody Map<String, Object> request) {
        String remarks  = (String) request.get("remarks");
        boolean approved = Boolean.parseBoolean(String.valueOf(request.get("approved")));
        return ResponseEntity.ok(service.verify(id, adminId, remarks, approved));
    }
}
