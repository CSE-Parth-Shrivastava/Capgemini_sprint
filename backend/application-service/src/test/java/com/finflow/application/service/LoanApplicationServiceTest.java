package com.finflow.application.service;

import com.finflow.application.client.NotificationClient;
import com.finflow.application.dto.LoanApplicationRequest;
import com.finflow.application.dto.SubmitResponse;
import com.finflow.application.entity.ApplicationStatus;
import com.finflow.application.entity.LoanApplication;
import com.finflow.application.entity.LoanType;
import com.finflow.application.entity.StatusHistory;
import com.finflow.application.exception.ForbiddenException;
import com.finflow.application.exception.InvalidOperationException;
import com.finflow.application.exception.ResourceNotFoundException;
import com.finflow.application.repository.LoanApplicationRepository;
import com.finflow.application.repository.StatusHistoryRepository;
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
@DisplayName("LoanApplicationService — Unit Tests")
class LoanApplicationServiceTest {

    @Mock private LoanApplicationRepository repository;
    @Mock private NotificationClient notificationClient;
    @Mock private StatusHistoryRepository statusHistoryRepository;

    @InjectMocks
    private LoanApplicationService service;

    private LoanApplication draftApp;
    private LoanApplicationRequest validRequest;

    @BeforeEach
    void setUp() {
        // FIX: LoanType.PERSONAL does not exist. Valid values: PRIVATE, BUSINESS, HOME, EDUCATION
        draftApp = new LoanApplication();
        draftApp.setId(1L);
        draftApp.setApplicantId(10L);
        draftApp.setFullName("Jane Doe");
        draftApp.setEmail("jane@example.com");
        draftApp.setPhone("9876543210");
        draftApp.setAddress("12 Baker Street");
        draftApp.setEmploymentType("SALARIED");
        draftApp.setMonthlyIncome(new BigDecimal("80000"));
        draftApp.setLoanType(LoanType.HOME);
        draftApp.setLoanAmount(new BigDecimal("500000"));
        draftApp.setTenureMonths(24);
        draftApp.setStatus(ApplicationStatus.DRAFT);

        validRequest = new LoanApplicationRequest();
        validRequest.setFullName("Jane Doe");
        validRequest.setEmail("jane@example.com");
        validRequest.setPhone("9876543210");
        validRequest.setAddress("12 Baker Street");
        validRequest.setDateOfBirth("1990-05-15");
        validRequest.setEmploymentType("SALARIED");
        validRequest.setEmployerName("Acme Corp");
        validRequest.setMonthlyIncome(new BigDecimal("80000"));
        validRequest.setLoanType(LoanType.HOME);
        validRequest.setLoanAmount(new BigDecimal("500000"));
        validRequest.setTenureMonths(24);
        validRequest.setPurpose("Home purchase");
    }

    // ── create() ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("saves application in DRAFT status and fires APPLICATION_CREATED notification")
        void create_validRequest_savesDraftAndNotifies() {
            when(repository.save(any(LoanApplication.class))).thenReturn(draftApp);

            LoanApplication result = service.create(10L, validRequest);

            assertThat(result.getStatus()).isEqualTo(ApplicationStatus.DRAFT);
            verify(repository).save(any(LoanApplication.class));
            verify(statusHistoryRepository).save(any(StatusHistory.class));
            verify(notificationClient).sendBoth(
                    eq(10L),
                    eq("jane@example.com"),
                    contains("Application Created"),
                    anyString(),
                    eq("APPLICATION_CREATED"),
                    eq(1L)
            );
        }

        @Test
        @DisplayName("status history records null-to-DRAFT transition on create")
        void create_recordsNullToDraftStatusHistory() {
            when(repository.save(any(LoanApplication.class))).thenReturn(draftApp);

            service.create(10L, validRequest);

            ArgumentCaptor<StatusHistory> captor = ArgumentCaptor.forClass(StatusHistory.class);
            verify(statusHistoryRepository).save(captor.capture());
            StatusHistory saved = captor.getValue();

            assertThat(saved.getFromStatus()).isNull();
            assertThat(saved.getToStatus()).isEqualTo(ApplicationStatus.DRAFT);
            assertThat(saved.getChangedBy()).isEqualTo("APPLICANT");
        }

        @Test
        @DisplayName("maps all request fields onto the saved entity")
        void create_mapsRequestFieldsCorrectly() {
            when(repository.save(any(LoanApplication.class))).thenAnswer(inv -> {
                LoanApplication app = inv.getArgument(0);
                app.setId(1L);
                return app;
            });

            LoanApplication result = service.create(10L, validRequest);

            assertThat(result.getFullName()).isEqualTo("Jane Doe");
            assertThat(result.getEmail()).isEqualTo("jane@example.com");
            assertThat(result.getEmploymentType()).isEqualTo("SALARIED");
            assertThat(result.getLoanType()).isEqualTo(LoanType.HOME);
            assertThat(result.getLoanAmount()).isEqualByComparingTo("500000");
            assertThat(result.getTenureMonths()).isEqualTo(24);
        }
    }

    // ── update() ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("update()")
    class Update {

        @Test
        @DisplayName("updates and saves a DRAFT application without firing notifications")
        void update_draftApp_savesUpdatedFields() {
            when(repository.findById(1L)).thenReturn(Optional.of(draftApp));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            validRequest.setFullName("Updated Name");
            LoanApplication result = service.update(1L, 10L, validRequest);

            assertThat(result.getFullName()).isEqualTo("Updated Name");
            verify(repository).save(draftApp);
            verifyNoInteractions(notificationClient);   // update must NOT notify
            verifyNoInteractions(statusHistoryRepository); // update does NOT record history
        }

        @Test
        @DisplayName("throws InvalidOperationException when application is not in DRAFT status")
        void update_submittedApp_throwsInvalidOperation() {
            draftApp.setStatus(ApplicationStatus.SUBMITTED);
            when(repository.findById(1L)).thenReturn(Optional.of(draftApp));

            InvalidOperationException ex = assertThrows(InvalidOperationException.class,
                    () -> service.update(1L, 10L, validRequest));

            assertThat(ex.getMessage()).containsIgnoringCase("DRAFT");
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("throws ForbiddenException when applicantId does not match application owner")
        void update_differentApplicant_throwsForbidden() {
            when(repository.findById(1L)).thenReturn(Optional.of(draftApp));

            assertThrows(ForbiddenException.class,
                    () -> service.update(1L, 99L, validRequest));
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when application id does not exist")
        void update_unknownId_throwsResourceNotFound() {
            when(repository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> service.update(999L, 10L, validRequest));
        }
    }

    // ── submit() ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("submit()")
    class Submit {

        @Test
        @DisplayName("transitions DRAFT to SUBMITTED, sets submittedAt, and returns SubmitResponse")
        void submit_draftApp_transitionsToSubmittedAndReturnsResponse() {
            when(repository.findById(1L)).thenReturn(Optional.of(draftApp));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            SubmitResponse response = service.submit(1L, 10L);

            assertAll(
                () -> assertThat(response).isNotNull(),
                () -> assertThat(response.getApplication().getStatus())
                          .isEqualTo(ApplicationStatus.SUBMITTED),
                () -> assertThat(response.getApplication().getSubmittedAt()).isNotNull(),
                () -> assertThat(response.getDocumentUploadUrl()).contains("applicationId=1"),
                () -> assertThat(response.getRequiredDocuments())
                          .containsExactlyInAnyOrder(
                                  "IDENTITY_PROOF", "INCOME_PROOF",
                                  "ADDRESS_PROOF", "BANK_STATEMENT"),
                () -> assertThat(response.getWorkflowStep()).isEqualTo(2),
                () -> assertThat(response.getTotalWorkflowSteps()).isEqualTo(3)
            );
        }

        @Test
        @DisplayName("fires APPLICATION_SUBMITTED notification with correct event type and applicant info")
        void submit_firesApplicationSubmittedNotification() {
            when(repository.findById(1L)).thenReturn(Optional.of(draftApp));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.submit(1L, 10L);

            verify(notificationClient).sendBoth(
                    eq(10L),
                    eq("jane@example.com"),
                    anyString(),
                    anyString(),
                    eq("APPLICATION_SUBMITTED"),
                    eq(1L)
            );
        }

        @Test
        @DisplayName("records DRAFT → SUBMITTED status history transition")
        void submit_recordsDraftToSubmittedHistory() {
            when(repository.findById(1L)).thenReturn(Optional.of(draftApp));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.submit(1L, 10L);

            ArgumentCaptor<StatusHistory> captor = ArgumentCaptor.forClass(StatusHistory.class);
            verify(statusHistoryRepository, atLeastOnce()).save(captor.capture());

            boolean hasSubmitRecord = captor.getAllValues().stream().anyMatch(h ->
                    h.getFromStatus() == ApplicationStatus.DRAFT &&
                    h.getToStatus()   == ApplicationStatus.SUBMITTED &&
                    "APPLICANT".equals(h.getChangedBy()));
            assertThat(hasSubmitRecord).isTrue();
        }

        @Test
        @DisplayName("throws InvalidOperationException when application is already SUBMITTED")
        void submit_alreadySubmitted_throwsInvalidOperation() {
            draftApp.setStatus(ApplicationStatus.SUBMITTED);
            when(repository.findById(1L)).thenReturn(Optional.of(draftApp));

            InvalidOperationException ex = assertThrows(InvalidOperationException.class,
                    () -> service.submit(1L, 10L));
            assertThat(ex.getMessage()).containsIgnoringCase("DRAFT");
        }

        @Test
        @DisplayName("throws InvalidOperationException when application is in APPROVED status")
        void submit_approvedApp_throwsInvalidOperation() {
            draftApp.setStatus(ApplicationStatus.APPROVED);
            when(repository.findById(1L)).thenReturn(Optional.of(draftApp));

            assertThrows(InvalidOperationException.class, () -> service.submit(1L, 10L));
        }

        @Test
        @DisplayName("throws ForbiddenException when wrong applicant tries to submit")
        void submit_wrongApplicant_throwsForbidden() {
            when(repository.findById(1L)).thenReturn(Optional.of(draftApp));

            assertThrows(ForbiddenException.class, () -> service.submit(1L, 55L));
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when application id does not exist")
        void submit_unknownId_throwsResourceNotFound() {
            when(repository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> service.submit(99L, 10L));
        }
    }

    // ── getById() ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getById()")
    class GetById {

        @Test
        @DisplayName("returns application when id exists")
        void getById_existingId_returnsApplication() {
            when(repository.findById(1L)).thenReturn(Optional.of(draftApp));

            LoanApplication result = service.getById(1L);

            assertThat(result.getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when id does not exist")
        void getById_notFound_throwsException() {
            when(repository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> service.getById(99L));
        }
    }

    // ── getByApplicant() ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("getByApplicant()")
    class GetByApplicant {

        @Test
        @DisplayName("delegates to repository and returns applicant's applications")
        void getByApplicant_returnsFilteredList() {
            when(repository.findByApplicantIdOrderByCreatedAtDesc(10L))
                    .thenReturn(List.of(draftApp));

            List<LoanApplication> result = service.getByApplicant(10L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getApplicantId()).isEqualTo(10L);
            verify(repository).findByApplicantIdOrderByCreatedAtDesc(10L);
        }

        @Test
        @DisplayName("returns empty list when applicant has no applications")
        void getByApplicant_noApplications_returnsEmpty() {
            when(repository.findByApplicantIdOrderByCreatedAtDesc(99L)).thenReturn(List.of());

            assertThat(service.getByApplicant(99L)).isEmpty();
        }
    }

    // ── getAll() ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getAll()")
    class GetAll {

        @Test
        @DisplayName("returns all applications from repository")
        void getAll_returnsAllApplications() {
            LoanApplication second = new LoanApplication();
            second.setId(2L);
            when(repository.findAll()).thenReturn(List.of(draftApp, second));

            assertThat(service.getAll()).hasSize(2);
            verify(repository).findAll();
        }

        @Test
        @DisplayName("returns empty list when no applications exist")
        void getAll_empty_returnsEmptyList() {
            when(repository.findAll()).thenReturn(List.of());

            assertThat(service.getAll()).isEmpty();
        }
    }

    // ── updateStatus() ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateStatus()")
    class UpdateStatus {

        @Test
        @DisplayName("sets new status, applies remarks, and saves application")
        void updateStatus_validId_changesStatusAndSaves() {
            when(repository.findById(1L)).thenReturn(Optional.of(draftApp));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            LoanApplication result = service.updateStatus(1L, ApplicationStatus.APPROVED, "Approved by admin");

            assertThat(result.getStatus()).isEqualTo(ApplicationStatus.APPROVED);
            assertThat(result.getRemarks()).isEqualTo("Approved by admin");
        }

        @Test
        @DisplayName("records status transition in history with ADMIN as changedBy")
        void updateStatus_recordsCorrectHistory() {
            draftApp.setStatus(ApplicationStatus.SUBMITTED);
            when(repository.findById(1L)).thenReturn(Optional.of(draftApp));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.updateStatus(1L, ApplicationStatus.UNDER_REVIEW, "Assigned");

            ArgumentCaptor<StatusHistory> captor = ArgumentCaptor.forClass(StatusHistory.class);
            verify(statusHistoryRepository).save(captor.capture());
            StatusHistory h = captor.getValue();

            assertThat(h.getFromStatus()).isEqualTo(ApplicationStatus.SUBMITTED);
            assertThat(h.getToStatus()).isEqualTo(ApplicationStatus.UNDER_REVIEW);
            assertThat(h.getChangedBy()).isEqualTo("ADMIN");
            assertThat(h.getRemarks()).isEqualTo("Assigned");
        }

        @Test
        @DisplayName("uses default remark when null is passed")
        void updateStatus_nullRemarks_usesDefaultRemark() {
            when(repository.findById(1L)).thenReturn(Optional.of(draftApp));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.updateStatus(1L, ApplicationStatus.REJECTED, null);

            ArgumentCaptor<StatusHistory> captor = ArgumentCaptor.forClass(StatusHistory.class);
            verify(statusHistoryRepository).save(captor.capture());
            // default remark is "Status updated by admin."
            assertThat(captor.getValue().getRemarks()).isEqualTo("Status updated by admin.");
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when application not found")
        void updateStatus_notFound_throwsException() {
            when(repository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> service.updateStatus(99L, ApplicationStatus.APPROVED, null));
        }
    }
}