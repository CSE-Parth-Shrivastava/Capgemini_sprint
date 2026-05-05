package com.finflow.document.controller;

import com.finflow.document.entity.Document;
import com.finflow.document.entity.DocumentStatus;
import com.finflow.document.exception.InvalidFileException;
import com.finflow.document.exception.ResourceNotFoundException;
import com.finflow.document.service.DocumentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finflow.document.config.SecurityConfig;

@WebMvcTest(DocumentController.class)
@Import(SecurityConfig.class)
@DisplayName("DocumentController")
class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DocumentService documentService;

    private Document makeDocument(Long id, Long applicantId, Long applicationId,
                                   String docType, DocumentStatus status) {
        Document doc = new Document();
        doc.setId(id);
        doc.setApplicantId(applicantId);
        doc.setApplicationId(applicationId);
        doc.setDocumentType(docType);
        doc.setStatus(status);
        doc.setFileName("test.pdf");
        doc.setMimeType("application/pdf");
        doc.setFileSize(512L);
        return doc;
    }

    // ── POST /documents/upload ────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /documents/upload")
    class UploadEndpoint {

        @Test
        @DisplayName("returns 200 with saved document on valid upload")
        void upload_validFile_returns200() throws Exception {
            Document saved = makeDocument(1L, 10L, 20L, "IDENTITY_PROOF", DocumentStatus.PENDING);
            when(documentService.upload(10L, 20L, "IDENTITY_PROOF", any()))
                    .thenReturn(saved);

            MockMultipartFile file = new MockMultipartFile(
                    "file", "id_proof.pdf", "application/pdf", "PDF content".getBytes());

            mockMvc.perform(multipart("/documents/upload")
                            .file(file)
                            .param("applicationId", "20")
                            .param("documentType", "IDENTITY_PROOF")
                            .header("X-User-Id", "10")
                            .header("X-User-Role", "APPLICANT"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andExpect(jsonPath("$.documentType").value("IDENTITY_PROOF"));
        }

        @Test
        @DisplayName("returns 403 when caller does not have APPLICANT role")
        void upload_nonApplicantRole_returns403() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "id_proof.pdf", "application/pdf", "PDF content".getBytes());

            mockMvc.perform(multipart("/documents/upload")
                            .file(file)
                            .param("applicationId", "20")
                            .param("documentType", "IDENTITY_PROOF")
                            .header("X-User-Id", "10")
                            .header("X-User-Role", "ADMIN"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("returns 403 when authentication headers are missing")
        void upload_noAuthHeaders_returns403() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "id_proof.pdf", "application/pdf", "PDF content".getBytes());

            mockMvc.perform(multipart("/documents/upload")
                            .file(file)
                            .param("applicationId", "20")
                            .param("documentType", "IDENTITY_PROOF"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("returns 400 when service throws InvalidFileException")
        void upload_invalidFile_returns400() throws Exception {
            when(documentService.upload(anyLong(), anyLong(), anyString(), any()))
                    .thenThrow(new InvalidFileException("File size exceeds the 10 MB limit"));

            MockMultipartFile file = new MockMultipartFile(
                    "file", "big.pdf", "application/pdf", "content".getBytes());

            mockMvc.perform(multipart("/documents/upload")
                            .file(file)
                            .param("applicationId", "20")
                            .param("documentType", "IDENTITY_PROOF")
                            .header("X-User-Id", "10")
                            .header("X-User-Role", "APPLICANT"))
                    .andExpect(status().isBadRequest());
        }
    }

    // ── GET /documents/application/{applicationId} ────────────────────────────

    @Nested
    @DisplayName("GET /documents/application/{applicationId}")
    class GetByApplicationEndpoint {

        @Test
        @DisplayName("returns 200 with document list")
        void getByApplication_returns200WithDocuments() throws Exception {
            List<Document> docs = List.of(
                    makeDocument(1L, 10L, 20L, "IDENTITY_PROOF", DocumentStatus.PENDING),
                    makeDocument(2L, 10L, 20L, "INCOME_PROOF", DocumentStatus.VERIFIED)
            );
            when(documentService.getByApplication(20L)).thenReturn(docs);

            mockMvc.perform(get("/documents/application/20")
                            .header("X-User-Id", "10")
                            .header("X-User-Role", "APPLICANT"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].documentType").value("IDENTITY_PROOF"))
                    .andExpect(jsonPath("$[1].status").value("VERIFIED"));
        }

        @Test
        @DisplayName("returns 200 with empty list when no documents found")
        void getByApplication_noDocuments_returnsEmptyList() throws Exception {
            when(documentService.getByApplication(99L)).thenReturn(List.of());

            mockMvc.perform(get("/documents/application/99")
                            .header("X-User-Id", "10")
                            .header("X-User-Role", "APPLICANT"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    // ── GET /documents/my ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /documents/my")
    class MyDocumentsEndpoint {

        @Test
        @DisplayName("returns 200 with applicant's own documents")
        void myDocuments_returns200() throws Exception {
            List<Document> docs = List.of(
                    makeDocument(1L, 10L, 20L, "IDENTITY_PROOF", DocumentStatus.PENDING)
            );
            when(documentService.getByApplicant(10L)).thenReturn(docs);

            mockMvc.perform(get("/documents/my")
                            .header("X-User-Id", "10")
                            .header("X-User-Role", "APPLICANT"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].applicantId").value(10));
        }

        @Test
        @DisplayName("returns 403 when ADMIN tries to access /my")
        void myDocuments_adminRole_returns403() throws Exception {
            mockMvc.perform(get("/documents/my")
                            .header("X-User-Id", "1")
                            .header("X-User-Role", "ADMIN"))
                    .andExpect(status().isForbidden());
        }
    }

    // ── GET /documents/pending ────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /documents/pending")
    class PendingDocumentsEndpoint {

        @Test
        @DisplayName("returns 200 with pending documents for ADMIN")
        void getPending_adminRole_returns200() throws Exception {
            List<Document> pending = List.of(
                    makeDocument(1L, 10L, 20L, "IDENTITY_PROOF", DocumentStatus.PENDING),
                    makeDocument(2L, 11L, 21L, "INCOME_PROOF", DocumentStatus.PENDING)
            );
            when(documentService.getPendingDocuments()).thenReturn(pending);

            mockMvc.perform(get("/documents/pending")
                            .header("X-User-Id", "1")
                            .header("X-User-Role", "ADMIN"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[*].status", everyItem(is("PENDING"))));
        }

        @Test
        @DisplayName("returns 403 when APPLICANT tries to access /pending")
        void getPending_applicantRole_returns403() throws Exception {
            mockMvc.perform(get("/documents/pending")
                            .header("X-User-Id", "10")
                            .header("X-User-Role", "APPLICANT"))
                    .andExpect(status().isForbidden());
        }
    }

    // ── PUT /documents/{id}/verify ────────────────────────────────────────────

    @Nested
    @DisplayName("PUT /documents/{id}/verify")
    class VerifyEndpoint {

        @Test
        @DisplayName("returns 200 with VERIFIED document when approved=true")
        void verify_approved_returns200Verified() throws Exception {
            Document verified = makeDocument(1L, 10L, 20L, "IDENTITY_PROOF", DocumentStatus.VERIFIED);
            when(documentService.verify(1L, "1", "All good", true)).thenReturn(verified);

            Map<String, Object> body = Map.of("approved", true, "remarks", "All good");

            mockMvc.perform(put("/documents/1/verify")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body))
                            .header("X-User-Id", "1")
                            .header("X-User-Role", "ADMIN"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("VERIFIED"));
        }

        @Test
        @DisplayName("returns 200 with REJECTED document when approved=false")
        void verify_rejected_returns200Rejected() throws Exception {
            Document rejected = makeDocument(1L, 10L, 20L, "IDENTITY_PROOF", DocumentStatus.REJECTED);
            when(documentService.verify(1L, "1", "Blurry", false)).thenReturn(rejected);

            Map<String, Object> body = Map.of("approved", false, "remarks", "Blurry");

            mockMvc.perform(put("/documents/1/verify")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body))
                            .header("X-User-Id", "1")
                            .header("X-User-Role", "ADMIN"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("REJECTED"));
        }

        @Test
        @DisplayName("returns 404 when document does not exist")
        void verify_documentNotFound_returns404() throws Exception {
            when(documentService.verify(eq(99L), anyString(), anyString(), anyBoolean()))
                    .thenThrow(new ResourceNotFoundException("Document", 99L));

            Map<String, Object> body = Map.of("approved", true, "remarks", "ok");

            mockMvc.perform(put("/documents/99/verify")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body))
                            .header("X-User-Id", "1")
                            .header("X-User-Role", "ADMIN"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("returns 403 when APPLICANT tries to verify a document")
        void verify_applicantRole_returns403() throws Exception {
            Map<String, Object> body = Map.of("approved", true, "remarks", "ok");

            mockMvc.perform(put("/documents/1/verify")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body))
                            .header("X-User-Id", "10")
                            .header("X-User-Role", "APPLICANT"))
                    .andExpect(status().isForbidden());
        }
    }
}
