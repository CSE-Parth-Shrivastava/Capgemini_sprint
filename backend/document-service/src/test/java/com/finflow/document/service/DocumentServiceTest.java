package com.finflow.document.service;

import com.finflow.document.client.NotificationClient;
import com.finflow.document.entity.Document;
import com.finflow.document.entity.DocumentStatus;
import com.finflow.document.exception.InvalidFileException;
import com.finflow.document.exception.ResourceNotFoundException;
import com.finflow.document.repository.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DocumentService Unit Tests")
class DocumentServiceTest {

    @TempDir
    Path tempDir;

    @Mock private DocumentRepository repository;
    @Mock private NotificationClient notificationClient;

    @InjectMocks
    private DocumentService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "uploadDir", tempDir.toString());
    }

    // ── upload ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("upload: valid PDF is stored and notification sent")
    void upload_validPdf_storesDocumentAndNotifies() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "document.pdf", "application/pdf", new byte[100]);
        Document saved = new Document();
        saved.setId(1L);
        when(repository.save(any(Document.class))).thenReturn(saved);

        Document result = service.upload(10L, 1L, "IDENTITY_PROOF", file);

        assertThat(result.getId()).isEqualTo(1L);
        verify(notificationClient).sendInApp(eq(10L), anyString(), anyString(), eq("DOCUMENT_UPLOADED"), eq(1L));
    }

    @Test
    @DisplayName("upload: valid JPEG is accepted")
    void upload_validJpeg_storesSuccessfully() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", new byte[500]);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertDoesNotThrow(() -> service.upload(10L, 1L, "INCOME_PROOF", file));
    }

    @Test
    @DisplayName("upload: empty file throws InvalidFileException")
    void upload_emptyFile_throwsInvalidFileException() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "empty.pdf", "application/pdf", new byte[0]);

        assertThrows(InvalidFileException.class,
                () -> service.upload(10L, 1L, "IDENTITY_PROOF", file));
    }

    @Test
    @DisplayName("upload: unsupported MIME type throws InvalidFileException")
    void upload_unsupportedMimeType_throwsInvalidFileException() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "file.exe", "application/x-msdownload", new byte[100]);

        assertThrows(InvalidFileException.class,
                () -> service.upload(10L, 1L, "IDENTITY_PROOF", file));
    }

    @Test
    @DisplayName("upload: file exceeding 10MB limit throws InvalidFileException")
    void upload_fileTooLarge_throwsInvalidFileException() {
        byte[] largeContent = new byte[11 * 1024 * 1024]; // 11 MB
        MockMultipartFile file = new MockMultipartFile(
                "file", "large.pdf", "application/pdf", largeContent);

        assertThrows(InvalidFileException.class,
                () -> service.upload(10L, 1L, "IDENTITY_PROOF", file));
    }

    @Test
    @DisplayName("upload: file without extension throws InvalidFileException")
    void upload_noExtension_throwsInvalidFileException() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "nodotfile", "application/pdf", new byte[100]);

        assertThrows(InvalidFileException.class,
                () -> service.upload(10L, 1L, "IDENTITY_PROOF", file));
    }

    // ── verify ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("verify: approve sets status to VERIFIED and notifies")
    void verify_approve_setsVerifiedStatusAndNotifies() {
        Document doc = pendingDocument();
        when(repository.findById(1L)).thenReturn(Optional.of(doc));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Document result = service.verify(1L, "admin@finflow.com", "Looks good", true);

        assertThat(result.getStatus()).isEqualTo(DocumentStatus.VERIFIED);
        verify(notificationClient).sendInApp(eq(doc.getApplicantId()), anyString(), anyString(), eq("DOCUMENT_VERIFIED"), any());
    }

    @Test
    @DisplayName("verify: reject sets status to REJECTED and notifies")
    void verify_reject_setsRejectedStatusAndNotifies() {
        Document doc = pendingDocument();
        when(repository.findById(1L)).thenReturn(Optional.of(doc));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Document result = service.verify(1L, "admin@finflow.com", "Blurry image", false);

        assertThat(result.getStatus()).isEqualTo(DocumentStatus.REJECTED);
        verify(notificationClient).sendInApp(eq(doc.getApplicantId()), anyString(), anyString(), eq("DOCUMENT_REJECTED"), any());
    }

    @Test
    @DisplayName("verify: document not found throws ResourceNotFoundException")
    void verify_notFound_throwsException() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.verify(99L, "admin", null, true));
    }

    // ── getByApplication ──────────────────────────────────────────────────────

    @Test
    @DisplayName("getByApplication: returns documents for given application")
    void getByApplication_returnsCorrectDocuments() {
        Document doc = pendingDocument();
        when(repository.findByApplicationId(1L)).thenReturn(List.of(doc));

        List<Document> result = service.getByApplication(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getApplicationId()).isEqualTo(1L);
    }

    // ── getByApplicant ────────────────────────────────────────────────────────

    @Test
    @DisplayName("getByApplicant: returns documents for given applicant")
    void getByApplicant_returnsCorrectDocuments() {
        when(repository.findByApplicantId(10L)).thenReturn(List.of(pendingDocument()));

        List<Document> result = service.getByApplicant(10L);

        assertThat(result).hasSize(1);
    }

    // ── getPendingDocuments ───────────────────────────────────────────────────

    @Test
    @DisplayName("getPendingDocuments: returns only PENDING status documents")
    void getPendingDocuments_returnsOnlyPending() {
        when(repository.findByStatus(DocumentStatus.PENDING)).thenReturn(List.of(pendingDocument()));

        List<Document> result = service.getPendingDocuments();

        assertThat(result).allMatch(d -> d.getStatus() == DocumentStatus.PENDING);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Document pendingDocument() {
        Document doc = new Document();
        doc.setId(1L);
        doc.setApplicantId(10L);
        doc.setApplicationId(1L);
        doc.setDocumentType("IDENTITY_PROOF");
        doc.setStatus(DocumentStatus.PENDING);
        return doc;
    }
}
