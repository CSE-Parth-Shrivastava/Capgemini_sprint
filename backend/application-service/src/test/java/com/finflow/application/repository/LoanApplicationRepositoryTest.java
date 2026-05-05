package com.finflow.application.repository;

import com.finflow.application.entity.ApplicationStatus;
import com.finflow.application.entity.LoanApplication;
import com.finflow.application.entity.LoanType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:apptest;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.cloud.config.enabled=false",
        "spring.config.import=",
        "eureka.client.enabled=false"
})
@DisplayName("LoanApplicationRepository — Integration Tests")
class LoanApplicationRepositoryTest {

    @Autowired
    private LoanApplicationRepository repository;

    @BeforeEach
    void cleanUp() {
        repository.deleteAll();
    }

    /**
     * Helper: creates and persists a LoanApplication.
     * FIX: LoanType.PERSONAL does not exist. Using LoanType.PRIVATE (valid enum value).
     */
    private LoanApplication persist(Long applicantId, ApplicationStatus status) {
        LoanApplication app = new LoanApplication();
        app.setApplicantId(applicantId);
        app.setFullName("Test User " + applicantId);
        app.setEmail("user" + applicantId + "@example.com");
        app.setPhone("9876543210");
        app.setAddress("123 Main Street");
        app.setEmploymentType("SALARIED");
        app.setMonthlyIncome(new BigDecimal("80000"));
        app.setLoanType(LoanType.PRIVATE);     // FIX: PERSONAL → PRIVATE (valid enum constant)
        app.setLoanAmount(new BigDecimal("500000"));
        app.setTenureMonths(24);
        app.setStatus(status);
        return repository.save(app);
    }

    // ── findByApplicantIdOrderByCreatedAtDesc() ───────────────────────────────

    @Nested
    @DisplayName("findByApplicantIdOrderByCreatedAtDesc()")
    class FindByApplicantId {

        @Test
        @DisplayName("returns all applications for the given applicant")
        void returnsApplicationsForApplicant() {
            persist(10L, ApplicationStatus.DRAFT);
            persist(10L, ApplicationStatus.SUBMITTED);
            persist(11L, ApplicationStatus.DRAFT); // different applicant — must be excluded

            List<LoanApplication> result =
                    repository.findByApplicantIdOrderByCreatedAtDesc(10L);

            assertThat(result).hasSize(2);
            assertThat(result).extracting(LoanApplication::getApplicantId).containsOnly(10L);
        }

        @Test
        @DisplayName("returns empty list when applicant has no applications")
        void unknownApplicant_returnsEmpty() {
            persist(10L, ApplicationStatus.DRAFT);

            assertThat(repository.findByApplicantIdOrderByCreatedAtDesc(999L)).isEmpty();
        }

        @Test
        @DisplayName("returns applications ordered newest-first (DESC)")
        void returnsInDescendingCreatedAtOrder() throws InterruptedException {
            LoanApplication first  = persist(10L, ApplicationStatus.DRAFT);
            Thread.sleep(10); // ensure distinct createdAt timestamps
            LoanApplication second = persist(10L, ApplicationStatus.SUBMITTED);

            List<LoanApplication> result =
                    repository.findByApplicantIdOrderByCreatedAtDesc(10L);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getId()).isEqualTo(second.getId()); // newest first
            assertThat(result.get(1).getId()).isEqualTo(first.getId());
        }
    }

    // ── findByApplicantId() ───────────────────────────────────────────────────

    @Nested
    @DisplayName("findByApplicantId()")
    class FindByApplicantIdUnordered {

        @Test
        @DisplayName("returns all applications for the given applicant without ordering")
        void returnsAllApplicationsForApplicant() {
            persist(20L, ApplicationStatus.DRAFT);
            persist(20L, ApplicationStatus.APPROVED);
            persist(21L, ApplicationStatus.DRAFT);

            List<LoanApplication> result = repository.findByApplicantId(20L);

            assertThat(result).hasSize(2);
            assertThat(result).extracting(LoanApplication::getApplicantId).containsOnly(20L);
        }
    }

    // ── findByStatus() ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("findByStatus()")
    class FindByStatus {

        @Test
        @DisplayName("returns only applications matching the given status")
        void returnsApplicationsMatchingStatus() {
            persist(10L, ApplicationStatus.DRAFT);
            persist(11L, ApplicationStatus.DRAFT);
            persist(12L, ApplicationStatus.SUBMITTED);

            List<LoanApplication> drafts = repository.findByStatus(ApplicationStatus.DRAFT);

            assertThat(drafts).hasSize(2);
            assertThat(drafts).extracting(LoanApplication::getStatus)
                    .containsOnly(ApplicationStatus.DRAFT);
        }

        @Test
        @DisplayName("returns empty list when no applications match the status")
        void noMatchingStatus_returnsEmpty() {
            persist(10L, ApplicationStatus.DRAFT);

            assertThat(repository.findByStatus(ApplicationStatus.APPROVED)).isEmpty();
        }
    }

    // ── findById() ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("findById()")
    class FindById {

        @Test
        @DisplayName("returns present Optional when application exists")
        void findById_existingId_returnsApplication() {
            LoanApplication saved = persist(10L, ApplicationStatus.DRAFT);

            Optional<LoanApplication> result = repository.findById(saved.getId());

            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(saved.getId());
        }

        @Test
        @DisplayName("returns empty Optional for unknown id")
        void findById_unknownId_returnsEmpty() {
            assertThat(repository.findById(9999L)).isEmpty();
        }
    }

    // ── save() — @PrePersist ──────────────────────────────────────────────────

    @Nested
    @DisplayName("@PrePersist lifecycle")
    class PrePersist {

        @Test
        @DisplayName("createdAt is automatically set on first save")
        void save_setsCreatedAt() {
            LoanApplication app = persist(10L, ApplicationStatus.DRAFT);

            assertThat(app.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("updatedAt is automatically set on first save")
        void save_setsUpdatedAt() {
            LoanApplication app = persist(10L, ApplicationStatus.DRAFT);

            assertThat(app.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("all saved fields are correctly persisted and retrievable")
        void save_persistsAllFields() {
            LoanApplication app = persist(42L, ApplicationStatus.SUBMITTED);

            LoanApplication fetched = repository.findById(app.getId()).orElseThrow();

            assertThat(fetched.getApplicantId()).isEqualTo(42L);
            assertThat(fetched.getStatus()).isEqualTo(ApplicationStatus.SUBMITTED);
            assertThat(fetched.getLoanType()).isEqualTo(LoanType.PRIVATE);
            assertThat(fetched.getLoanAmount()).isEqualByComparingTo("500000");
            assertThat(fetched.getTenureMonths()).isEqualTo(24);
        }
    }

    // ── deleteAll() ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteAll()")
    class DeleteAll {

        @Test
        @DisplayName("repository is empty after deleteAll()")
        void deleteAll_leavesRepositoryEmpty() {
            persist(10L, ApplicationStatus.DRAFT);
            persist(11L, ApplicationStatus.SUBMITTED);

            repository.deleteAll();

            assertThat(repository.findAll()).isEmpty();
        }
    }
}