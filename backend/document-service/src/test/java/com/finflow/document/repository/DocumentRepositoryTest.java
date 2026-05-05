package com.finflow.document.repository;

import com.finflow.document.entity.Document;
import com.finflow.document.entity.DocumentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:doctest;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false"
})
@DisplayName("DocumentRepository")
class DocumentRepositoryTest {

    @Autowired
    private DocumentRepository repository;

    private Document persist(Long applicantId, Long applicationId,
                              String docType, DocumentStatus status) {
        Document doc = new Document();
        doc.setApplicantId(applicantId);
        doc.setApplicationId(applicationId);
        doc.setDocumentType(docType);
        doc.setFileName(docType.toLowerCase() + ".pdf");
        doc.setFilePath("/tmp/" + docType.toLowerCase() + ".pdf");
        doc.setFileSize(1024L);
        doc.setMimeType("application/pdf");
        doc.setStatus(status);
        return repository.save(doc);
    }

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Nested
    @DisplayName("findByApplicationId()")
    class FindByApplicationId {

        @Test
        @DisplayName("returns all documents for given application")
        void findByApplicationId_returnsMatchingDocuments() {
            persist(10L, 20L, "IDENTITY_PROOF", DocumentStatus.PENDING);
            persist(10L, 20L, "INCOME_PROOF",   DocumentStatus.VERIFIED);
            persist(11L, 21L, "ADDRESS_PROOF",  DocumentStatus.PENDING); // different app

            List<Document> result = repository.findByApplicationId(20L);

            assertThat(result).hasSize(2);
            assertThat(result).extracting(Document::getApplicationId).containsOnly(20L);
        }

        @Test
        @DisplayName("returns empty list for unknown application ID")
        void findByApplicationId_unknownId_returnsEmpty() {
            persist(10L, 20L, "IDENTITY_PROOF", DocumentStatus.PENDING);

            assertThat(repository.findByApplicationId(999L)).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByApplicantId()")
    class FindByApplicantId {

        @Test
        @DisplayName("returns all documents for given applicant across applications")
        void findByApplicantId_returnsAllDocumentsForApplicant() {
            persist(10L, 20L, "IDENTITY_PROOF", DocumentStatus.PENDING);
            persist(10L, 21L, "INCOME_PROOF",   DocumentStatus.VERIFIED);
            persist(11L, 22L, "ADDRESS_PROOF",  DocumentStatus.PENDING); // different applicant

            List<Document> result = repository.findByApplicantId(10L);

            assertThat(result).hasSize(2);
            assertThat(result).extracting(Document::getApplicantId).containsOnly(10L);
        }

        @Test
        @DisplayName("returns empty list for unknown applicant")
        void findByApplicantId_unknownId_returnsEmpty() {
            assertThat(repository.findByApplicantId(999L)).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByStatus()")
    class FindByStatus {

        @Test
        @DisplayName("returns only PENDING documents")
        void findByStatus_pending_returnsPendingOnly() {
            persist(10L, 20L, "IDENTITY_PROOF", DocumentStatus.PENDING);
            persist(10L, 20L, "INCOME_PROOF",   DocumentStatus.VERIFIED);
            persist(11L, 21L, "ADDRESS_PROOF",  DocumentStatus.REJECTED);
            persist(12L, 22L, "BANK_STATEMENT", DocumentStatus.PENDING);

            List<Document> result = repository.findByStatus(DocumentStatus.PENDING);

            assertThat(result).hasSize(2);
            assertThat(result).extracting(Document::getStatus)
                    .containsOnly(DocumentStatus.PENDING);
        }

        @Test
        @DisplayName("returns only VERIFIED documents")
        void findByStatus_verified_returnsVerifiedOnly() {
            persist(10L, 20L, "IDENTITY_PROOF", DocumentStatus.PENDING);
            persist(10L, 20L, "INCOME_PROOF",   DocumentStatus.VERIFIED);

            List<Document> result = repository.findByStatus(DocumentStatus.VERIFIED);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getDocumentType()).isEqualTo("INCOME_PROOF");
        }

        @Test
        @DisplayName("returns empty list when no documents match status")
        void findByStatus_noMatch_returnsEmpty() {
            persist(10L, 20L, "IDENTITY_PROOF", DocumentStatus.PENDING);

            assertThat(repository.findByStatus(DocumentStatus.REJECTED)).isEmpty();
        }
    }

    @Nested
    @DisplayName("@PrePersist uploadedAt")
    class PrePersist {

        @Test
        @DisplayName("uploadedAt is set automatically on save")
        void save_setsUploadedAt() {
            Document doc = persist(10L, 20L, "IDENTITY_PROOF", DocumentStatus.PENDING);

            assertThat(doc.getUploadedAt()).isNotNull();
        }
    }
}
