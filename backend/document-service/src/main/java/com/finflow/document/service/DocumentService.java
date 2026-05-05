package com.finflow.document.service;

import com.finflow.document.client.NotificationClient;
import com.finflow.document.entity.Document;
import com.finflow.document.entity.DocumentStatus;
import com.finflow.document.exception.InvalidFileException;
import com.finflow.document.exception.ResourceNotFoundException;
import com.finflow.document.repository.DocumentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class DocumentService {

    private final DocumentRepository repository;
    private final NotificationClient notificationClient;

    @Value("${document.upload-dir:uploads/documents}")
    private String uploadDir;

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "application/pdf", "image/jpeg", "image/png", "image/webp"
    );

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".pdf", ".jpg", ".jpeg", ".png", ".webp"
    );

    public DocumentService(DocumentRepository repository, NotificationClient notificationClient) {
        this.repository         = repository;
        this.notificationClient = notificationClient;
    }

    /**
     * Stores an uploaded document and fires DOCUMENT_UPLOADED notifications
     * (both IN_APP and EMAIL) to the applicant.
     */
    public Document upload(Long applicantId, Long applicationId,
                           String documentType, MultipartFile file) throws IOException {
        validateFile(file);

        // Ensure upload directory exists
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Store file with a UUID filename to avoid collisions
        String extension      = getExtension(file.getOriginalFilename());
        String storedFileName = UUID.randomUUID() + extension;
        Path   filePath       = uploadPath.resolve(storedFileName);

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, filePath);
        }

        // Persist document record
        Document doc = new Document();
        doc.setApplicantId(applicantId);
        doc.setApplicationId(applicationId);
        doc.setDocumentType(documentType);
        doc.setFileName(file.getOriginalFilename());
        doc.setFilePath(filePath.toString());
        doc.setFileSize(file.getSize());
        doc.setMimeType(file.getContentType());
        doc.setStatus(DocumentStatus.PENDING);
        doc = repository.save(doc);

        // Fire DOCUMENT_UPLOADED notification — in-app only (email address not available here;
        // the applicant's email lives in auth-service/application-service, not document-service).
        // We send an in-app notification so the user sees it in their dashboard.
        notificationClient.sendInApp(
                applicantId,
                "Document Uploaded: " + documentType,
                "Your document '" + documentType + "' for loan application #" + applicationId +
                " has been received and is pending verification. " +
                "You will be notified once it is reviewed.",
                "DOCUMENT_UPLOADED",
                applicationId
        );

        return doc;
    }

    /**
     * Verifies (or rejects) a document and fires DOCUMENT_VERIFIED notification to the applicant.
     */
    public Document verify(Long documentId, String verifiedBy, String remarks, boolean approved) {
        Document doc = repository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", documentId));

        doc.setStatus(approved ? DocumentStatus.VERIFIED : DocumentStatus.REJECTED);
        doc.setVerifiedBy(verifiedBy);
        doc.setVerificationRemarks(remarks);
        doc.setVerifiedAt(LocalDateTime.now());
        doc = repository.save(doc);

        // Notify applicant about the verification result
        String eventType = approved ? "DOCUMENT_VERIFIED" : "DOCUMENT_REJECTED";
        String subject   = approved
                ? "Document Verified: " + doc.getDocumentType()
                : "Document Requires Attention: " + doc.getDocumentType();
        String message   = approved
                ? "Your document '" + doc.getDocumentType() + "' for application #" +
                  doc.getApplicationId() + " has been verified successfully."
                  + (remarks != null && !remarks.isBlank() ? " Remarks: " + remarks : "")
                : "Your document '" + doc.getDocumentType() + "' for application #" +
                  doc.getApplicationId() + " was not accepted and needs to be re-uploaded."
                  + (remarks != null && !remarks.isBlank() ? " Reason: " + remarks : "");

        notificationClient.sendInApp(
                doc.getApplicantId(),
                subject,
                message,
                eventType,
                doc.getApplicationId()
        );

        return doc;
    }

    public List<Document> getByApplication(Long applicationId) {
        return repository.findByApplicationId(applicationId);
    }

    public List<Document> getByApplicant(Long applicantId) {
        return repository.findByApplicantId(applicantId);
    }

    public List<Document> getPendingDocuments() {
        return repository.findByStatus(DocumentStatus.PENDING);
    }

    // ── File validation helpers ───────────────────────────────────────────────

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("File must not be empty");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new InvalidFileException("File size exceeds the 10 MB limit");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType)) {
            throw new InvalidFileException(
                    "Unsupported file type: " + contentType + ". Allowed: PDF, JPEG, PNG, WEBP");
        }
        String extension = getExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new InvalidFileException(
                    "Unsupported file extension: " + extension + ". Allowed: .pdf, .jpg, .jpeg, .png, .webp");
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            throw new InvalidFileException("File must have a valid extension");
        }
        return filename.substring(filename.lastIndexOf("."));
    }
}
