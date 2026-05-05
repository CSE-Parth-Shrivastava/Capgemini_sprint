package com.finflow.application.service;

import com.finflow.application.dto.CreditScoreRequest;
import com.finflow.application.entity.CreditScore;
import com.finflow.application.entity.LoanApplication;
import com.finflow.application.exception.ResourceNotFoundException;
import com.finflow.application.repository.CreditScoreRepository;
import com.finflow.application.repository.LoanApplicationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreditScoreService — Unit Tests")
class CreditScoreServiceTest {

    @Mock private CreditScoreRepository creditScoreRepository;
    @Mock private LoanApplicationRepository applicationRepository;

    @InjectMocks
    private CreditScoreService service;

    private LoanApplication application;

    @BeforeEach
    void setUp() {
        application = new LoanApplication();
        application.setId(1L);
        application.setApplicantId(10L);
        application.setMonthlyIncome(new BigDecimal("80000"));
        application.setLoanAmount(new BigDecimal("500000"));
        application.setTenureMonths(24);
        application.setEmploymentType("SALARIED");
    }

    /** Builds a request that overrides all fields (does not fall back to application data). */
    private CreditScoreRequest standardRequest() {
        CreditScoreRequest req = new CreditScoreRequest();
        req.setMonthlyIncome(new BigDecimal("80000"));
        req.setLoanAmount(new BigDecimal("500000"));
        req.setTenureMonths(24);
        req.setEmploymentType("SALARIED");
        req.setExistingEmis(5000);
        req.setCreditHistoryMonths(36);
        req.setDocumentStatus("ALL_VERIFIED");
        req.setNumberOfDependents(1);
        req.setHasExistingLoan(false);
        return req;
    }

    // ── assess() ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("assess()")
    class Assess {

        @Test
        @DisplayName("returns CreditScore with overall score in 300–850 CIBIL range")
        void assess_validInput_returnsScoreInRange() {
            when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
            when(creditScoreRepository.save(any(CreditScore.class))).thenAnswer(inv -> inv.getArgument(0));

            CreditScore result = service.assess(1L, 10L, standardRequest());

            assertAll(
                () -> assertThat(result.getScore()).isBetween(300, 850),
                () -> assertThat(result.getGrade()).isNotBlank(),
                () -> assertThat(result.getRecommendation()).isNotBlank(),
                () -> assertThat(result.getAiAnalysis()).isNotBlank()
            );
            verify(creditScoreRepository).save(any(CreditScore.class));
        }

        @Test
        @DisplayName("persists all sub-scores between 0 and 100")
        void assess_allSubScoresInValidRange() {
            when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
            when(creditScoreRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            CreditScore result = service.assess(1L, 10L, standardRequest());

            assertAll(
                () -> assertThat(result.getIncomeStabilityScore()).isBetween(0, 100),
                () -> assertThat(result.getDebtToIncomeScore()).isBetween(0, 100),
                () -> assertThat(result.getEmploymentScore()).isBetween(0, 100),
                () -> assertThat(result.getDocumentVerificationScore()).isBetween(0, 100),
                () -> assertThat(result.getLoanAffordabilityScore()).isBetween(0, 100)
            );
        }

        @Test
        @DisplayName("persists correct applicant and application ids on CreditScore")
        void assess_persistsCorrectIds() {
            when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
            when(creditScoreRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            CreditScore result = service.assess(1L, 10L, standardRequest());

            assertThat(result.getApplicationId()).isEqualTo(1L);
            assertThat(result.getApplicantId()).isEqualTo(10L);
        }

        @Test
        @DisplayName("grade is EXCELLENT or GOOD for a strong salaried profile (score >= 650)")
        void assess_strongSalariedProfile_producesGoodOrExcellentGrade() {
            CreditScoreRequest req = new CreditScoreRequest();
            req.setMonthlyIncome(new BigDecimal("200000"));
            req.setLoanAmount(new BigDecimal("300000"));
            req.setTenureMonths(60);
            req.setEmploymentType("SALARIED");
            req.setExistingEmis(0);
            req.setCreditHistoryMonths(72);
            req.setDocumentStatus("ALL_VERIFIED");
            req.setNumberOfDependents(0);
            req.setHasExistingLoan(false);

            when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
            when(creditScoreRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            CreditScore result = service.assess(1L, 10L, req);

            assertThat(result.getScore()).isGreaterThanOrEqualTo(650);
            assertThat(result.getGrade()).isIn("EXCELLENT", "GOOD");
            assertThat(result.getRecommendation()).isIn("STRONGLY_RECOMMENDED", "RECOMMENDED");
        }

        @Test
        @DisplayName("grade is POOR and NOT_RECOMMENDED for a weak unemployed profile (score < 550)")
        void assess_weakUnemployedProfile_producesLowScoreAndPoorGrade() {
            CreditScoreRequest req = new CreditScoreRequest();
            req.setMonthlyIncome(new BigDecimal("8000"));
            req.setLoanAmount(new BigDecimal("2000000"));
            req.setTenureMonths(12);
            req.setEmploymentType("UNEMPLOYED");
            req.setExistingEmis(20000);
            req.setCreditHistoryMonths(0);
            req.setDocumentStatus("NONE_UPLOADED");
            req.setNumberOfDependents(5);
            req.setHasExistingLoan(true);

            application.setMonthlyIncome(new BigDecimal("8000"));
            application.setLoanAmount(new BigDecimal("2000000"));
            application.setEmploymentType("UNEMPLOYED");

            when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
            when(creditScoreRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            CreditScore result = service.assess(1L, 10L, req);

            assertThat(result.getScore()).isLessThan(550);
            assertThat(result.getGrade()).isEqualTo("POOR");
            assertThat(result.getRecommendation()).isEqualTo("NOT_RECOMMENDED");
        }

        @Test
        @DisplayName("falls back to application data when request income/loan/tenure/employment are null")
        void assess_nullRequestFields_fallsBackToApplicationData() {
            CreditScoreRequest emptyReq = new CreditScoreRequest(); // all overrides null

            when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
            when(creditScoreRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            CreditScore result = service.assess(1L, 10L, emptyReq);

            // Should use application's values
            assertThat(result.getMonthlyIncome()).isEqualByComparingTo(application.getMonthlyIncome());
            assertThat(result.getLoanAmount()).isEqualByComparingTo(application.getLoanAmount());
            assertThat(result.getTenureMonths()).isEqualTo(application.getTenureMonths());
            assertThat(result.getEmploymentType()).isEqualTo(application.getEmploymentType());
        }

        @Test
        @DisplayName("documentVerificationScore is 100 when ALL_VERIFIED")
        void assess_allVerifiedDocs_fullDocScore() {
            CreditScoreRequest req = standardRequest();
            req.setDocumentStatus("ALL_VERIFIED");

            when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
            when(creditScoreRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            CreditScore result = service.assess(1L, 10L, req);

            assertThat(result.getDocumentVerificationScore()).isEqualTo(100);
        }

        @Test
        @DisplayName("documentVerificationScore is 10 when NONE_UPLOADED")
        void assess_noDocuments_lowestDocScore() {
            CreditScoreRequest req = standardRequest();
            req.setDocumentStatus("NONE_UPLOADED");

            when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
            when(creditScoreRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            CreditScore result = service.assess(1L, 10L, req);

            assertThat(result.getDocumentVerificationScore()).isEqualTo(10);
        }

        @Test
        @DisplayName("aiAnalysis contains score, grade and recommendation sections")
        void assess_aiAnalysisContainsExpectedSections() {
            when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
            when(creditScoreRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            CreditScore result = service.assess(1L, 10L, standardRequest());

            assertThat(result.getAiAnalysis())
                    .contains("CREDIT ASSESSMENT SUMMARY")
                    .contains("FACTOR ANALYSIS")
                    .contains("CONCLUSION");
        }

        @Test
        @DisplayName("AI narrative mentions existing loan when hasExistingLoan is true")
        void assess_existingLoanTrue_narrativeMentionsIt() {
            CreditScoreRequest req = standardRequest();
            req.setHasExistingLoan(true);

            when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
            when(creditScoreRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            CreditScore result = service.assess(1L, 10L, req);

            assertThat(result.getAiAnalysis()).containsIgnoringCase("existing");
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when applicationId does not exist")
        void assess_applicationNotFound_throwsException() {
            when(applicationRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> service.assess(99L, 10L, standardRequest()));

            verifyNoInteractions(creditScoreRepository);
        }

        @Test
        @DisplayName("existingEmis defaults to 0 when null in request")
        void assess_nullExistingEmis_defaultsToZero() {
            CreditScoreRequest req = standardRequest();
            req.setExistingEmis(null);

            when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
            when(creditScoreRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            CreditScore result = service.assess(1L, 10L, req);

            assertThat(result.getExistingEmis()).isEqualTo(0);
        }

        @Test
        @DisplayName("creditHistoryMonths defaults to 0 when null in request")
        void assess_nullCreditHistoryMonths_defaultsToZero() {
            CreditScoreRequest req = standardRequest();
            req.setCreditHistoryMonths(null);

            when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
            when(creditScoreRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            CreditScore result = service.assess(1L, 10L, req);

            assertThat(result.getCreditHistoryMonths()).isEqualTo(0);
        }

        @Test
        @DisplayName("higher credit history months produce a higher employment score")
        void assess_longerCreditHistory_improvesEmploymentScore() {
            // Short history
            CreditScoreRequest shortHistory = standardRequest();
            shortHistory.setCreditHistoryMonths(0);

            // Long history
            CreditScoreRequest longHistory = standardRequest();
            longHistory.setCreditHistoryMonths(72); // 60+ months → +15 bonus

            when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
            when(creditScoreRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            CreditScore resultShort = service.assess(1L, 10L, shortHistory);

            reset(creditScoreRepository, applicationRepository);
            when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
            when(creditScoreRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            CreditScore resultLong = service.assess(1L, 10L, longHistory);

            assertThat(resultLong.getEmploymentScore())
                    .isGreaterThan(resultShort.getEmploymentScore());
        }
    }

    // ── getLatest() ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getLatest()")
    class GetLatest {

        @Test
        @DisplayName("returns latest credit score when one exists")
        void getLatest_existingScore_returnsMostRecent() {
            CreditScore cs = new CreditScore();
            cs.setScore(720);
            when(creditScoreRepository.findTopByApplicationIdOrderByAssessedAtDesc(1L))
                    .thenReturn(Optional.of(cs));

            Optional<CreditScore> result = service.getLatest(1L);

            assertThat(result).isPresent();
            assertThat(result.get().getScore()).isEqualTo(720);
        }

        @Test
        @DisplayName("returns empty Optional when no score exists for application")
        void getLatest_noScore_returnsEmpty() {
            when(creditScoreRepository.findTopByApplicationIdOrderByAssessedAtDesc(1L))
                    .thenReturn(Optional.empty());

            assertThat(service.getLatest(1L)).isEmpty();
        }
    }

    // ── getHistory() ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getHistory()")
    class GetHistory {

        @Test
        @DisplayName("returns all credit score records ordered by date descending")
        void getHistory_returnsAllScores() {
            CreditScore cs1 = new CreditScore();
            cs1.setScore(680);
            CreditScore cs2 = new CreditScore();
            cs2.setScore(710);
            when(creditScoreRepository.findByApplicationIdOrderByAssessedAtDesc(1L))
                    .thenReturn(List.of(cs2, cs1));

            List<CreditScore> result = service.getHistory(1L);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getScore()).isEqualTo(710); // newest first
        }

        @Test
        @DisplayName("returns empty list when no history exists")
        void getHistory_noHistory_returnsEmpty() {
            when(creditScoreRepository.findByApplicationIdOrderByAssessedAtDesc(99L))
                    .thenReturn(List.of());

            assertThat(service.getHistory(99L)).isEmpty();
        }
    }
}