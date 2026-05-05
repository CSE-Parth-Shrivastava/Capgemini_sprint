package com.finflow.admin.repository;

import com.finflow.admin.entity.Decision;
import com.finflow.admin.entity.DecisionType;
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
        "spring.datasource.url=jdbc:h2:mem:admintest;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false"
})
@DisplayName("DecisionRepository — Integration Tests")
class DecisionRepositoryTest {

    @Autowired
    private DecisionRepository repository;

    @BeforeEach
    void cleanUp() {
        repository.deleteAll();
    }

    private Decision persist(Long appId, Long adminId, DecisionType type) {
        Decision d = new Decision();
        d.setApplicationId(appId);
        d.setAdminId(adminId);
        d.setDecision(type);
        d.setRemarks("Test remark");
        if (type == DecisionType.APPROVED) {
            d.setApprovedAmount(new BigDecimal("500000"));
            d.setInterestRate(new BigDecimal("10.5"));
            d.setTenureMonths(24);
        }
        return repository.save(d);
    }

    // ── findByApplicationId() ─────────────────────────────────────────────────

    @Nested
    @DisplayName("findByApplicationId()")
    class FindByApplicationId {

        @Test
        @DisplayName("returns present Optional when a decision exists for the application")
        void findByApplicationId_existing_returnsDecision() {
            persist(1L, 10L, DecisionType.APPROVED);

            Optional<Decision> result = repository.findByApplicationId(1L);

            assertThat(result).isPresent();
            assertThat(result.get().getApplicationId()).isEqualTo(1L);
            assertThat(result.get().getDecision()).isEqualTo(DecisionType.APPROVED);
        }

        @Test
        @DisplayName("returns empty Optional when no decision exists for the application")
        void findByApplicationId_notFound_returnsEmpty() {
            assertThat(repository.findByApplicationId(999L)).isEmpty();
        }

        @Test
        @DisplayName("returns only the decision for the requested application, not others")
        void findByApplicationId_returnsCorrectApplication() {
            persist(1L, 10L, DecisionType.APPROVED);
            persist(2L, 10L, DecisionType.REJECTED);

            Optional<Decision> result = repository.findByApplicationId(2L);

            assertThat(result).isPresent();
            assertThat(result.get().getDecision()).isEqualTo(DecisionType.REJECTED);
        }
    }

    // ── findByDecision() ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("findByDecision()")
    class FindByDecision {

        @Test
        @DisplayName("returns only APPROVED decisions")
        void findByDecision_approved_returnsOnlyApproved() {
            persist(1L, 10L, DecisionType.APPROVED);
            persist(2L, 10L, DecisionType.APPROVED);
            persist(3L, 10L, DecisionType.REJECTED);

            List<Decision> approved = repository.findByDecision(DecisionType.APPROVED);

            assertThat(approved).hasSize(2);
            assertThat(approved).extracting(Decision::getDecision)
                    .containsOnly(DecisionType.APPROVED);
        }

        @Test
        @DisplayName("returns only REJECTED decisions")
        void findByDecision_rejected_returnsOnlyRejected() {
            persist(1L, 10L, DecisionType.APPROVED);
            persist(2L, 10L, DecisionType.REJECTED);

            List<Decision> rejected = repository.findByDecision(DecisionType.REJECTED);

            assertThat(rejected).hasSize(1);
            assertThat(rejected.get(0).getDecision()).isEqualTo(DecisionType.REJECTED);
        }

        @Test
        @DisplayName("returns empty list when no decisions match the type")
        void findByDecision_noMatch_returnsEmpty() {
            persist(1L, 10L, DecisionType.APPROVED);

            assertThat(repository.findByDecision(DecisionType.REJECTED)).isEmpty();
        }
    }

    // ── countByDecision() ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("countByDecision()")
    class CountByDecision {

        @Test
        @DisplayName("returns correct APPROVED count")
        void countApproved_returnsCorrectCount() {
            persist(1L, 10L, DecisionType.APPROVED);
            persist(2L, 10L, DecisionType.APPROVED);
            persist(3L, 10L, DecisionType.REJECTED);

            assertThat(repository.countByDecision(DecisionType.APPROVED)).isEqualTo(2L);
        }

        @Test
        @DisplayName("returns correct REJECTED count")
        void countRejected_returnsCorrectCount() {
            persist(1L, 10L, DecisionType.APPROVED);
            persist(2L, 10L, DecisionType.REJECTED);

            assertThat(repository.countByDecision(DecisionType.REJECTED)).isEqualTo(1L);
        }

        @Test
        @DisplayName("returns 0 when no decisions of the given type exist")
        void count_noMatch_returnsZero() {
            persist(1L, 10L, DecisionType.APPROVED);

            assertThat(repository.countByDecision(DecisionType.REJECTED)).isZero();
        }

        @Test
        @DisplayName("returns 0 when repository is empty")
        void count_empty_returnsZero() {
            assertThat(repository.countByDecision(DecisionType.APPROVED)).isZero();
        }
    }

    // ── findAll() / count() ───────────────────────────────────────────────────

    @Nested
    @DisplayName("findAll() and count()")
    class FindAllAndCount {

        @Test
        @DisplayName("findAll() returns every persisted decision")
        void findAll_returnsAll() {
            persist(1L, 10L, DecisionType.APPROVED);
            persist(2L, 10L, DecisionType.REJECTED);
            persist(3L, 11L, DecisionType.APPROVED);

            assertThat(repository.findAll()).hasSize(3);
        }

        @Test
        @DisplayName("findAll() returns empty list when repository is empty")
        void findAll_empty_returnsEmptyList() {
            assertThat(repository.findAll()).isEmpty();
        }

        @Test
        @DisplayName("count() returns total number of decisions")
        void count_returnsTotalDecisions() {
            persist(1L, 10L, DecisionType.APPROVED);
            persist(2L, 10L, DecisionType.REJECTED);

            assertThat(repository.count()).isEqualTo(2L);
        }

        @Test
        @DisplayName("count() returns 0 when repository is empty")
        void count_empty_returnsZero() {
            assertThat(repository.count()).isZero();
        }
    }

    // ── @PrePersist ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("@PrePersist lifecycle")
    class PrePersist {

        @Test
        @DisplayName("decidedAt is automatically set on save")
        void save_setsDecidedAt() {
            Decision d = persist(1L, 10L, DecisionType.APPROVED);

            assertThat(d.getDecidedAt()).isNotNull();
        }

        @Test
        @DisplayName("all fields are correctly persisted and retrievable")
        void save_persistsAllFields() {
            Decision d = persist(42L, 7L, DecisionType.APPROVED);

            Decision fetched = repository.findById(d.getId()).orElseThrow();

            assertThat(fetched.getApplicationId()).isEqualTo(42L);
            assertThat(fetched.getAdminId()).isEqualTo(7L);
            assertThat(fetched.getDecision()).isEqualTo(DecisionType.APPROVED);
            assertThat(fetched.getRemarks()).isEqualTo("Test remark");
            assertThat(fetched.getApprovedAmount()).isEqualByComparingTo("500000");
            assertThat(fetched.getInterestRate()).isEqualByComparingTo("10.5");
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
            persist(1L, 10L, DecisionType.APPROVED);
            persist(2L, 10L, DecisionType.REJECTED);

            repository.deleteAll();

            assertThat(repository.findAll()).isEmpty();
        }
    }
}