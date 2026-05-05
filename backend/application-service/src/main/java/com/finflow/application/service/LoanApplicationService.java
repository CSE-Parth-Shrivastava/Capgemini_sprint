package com.finflow.application.service;

import com.finflow.application.client.NotificationClient;
import com.finflow.application.dto.LoanApplicationRequest;
import com.finflow.application.dto.SubmitResponse;
import com.finflow.application.entity.ApplicationStatus;
import com.finflow.application.entity.LoanApplication;
import com.finflow.application.entity.StatusHistory;
import com.finflow.application.exception.ForbiddenException;
import com.finflow.application.exception.InvalidOperationException;
import com.finflow.application.exception.ResourceNotFoundException;
import com.finflow.application.repository.LoanApplicationRepository;
import com.finflow.application.repository.StatusHistoryRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class LoanApplicationService {

    private final LoanApplicationRepository repository;
    private final NotificationClient notificationClient;
    private final StatusHistoryRepository statusHistoryRepository;

    public LoanApplicationService(LoanApplicationRepository repository,
                                   NotificationClient notificationClient,
                                   StatusHistoryRepository statusHistoryRepository) {
        this.repository              = repository;
        this.notificationClient      = notificationClient;
        this.statusHistoryRepository = statusHistoryRepository;
    }

    // ── helper: persist status transition ───────────────────────────────────
    private void recordStatusChange(Long appId, ApplicationStatus from, ApplicationStatus to,
                                     String remarks, String changedBy) {
        StatusHistory h = new StatusHistory();
        h.setApplicationId(appId);
        h.setFromStatus(from);
        h.setToStatus(to);
        h.setRemarks(remarks);
        h.setChangedBy(changedBy);
        statusHistoryRepository.save(h);
    }

    /**
     * Creates a new DRAFT application and fires APPLICATION_CREATED notifications.
     */
    public LoanApplication create(Long applicantId, LoanApplicationRequest request) {
        LoanApplication app = new LoanApplication();
        app.setApplicantId(applicantId);
        mapRequest(app, request);
        app.setStatus(ApplicationStatus.DRAFT);
        app = repository.save(app);
        recordStatusChange(app.getId(), null, ApplicationStatus.DRAFT,
                "Application created in DRAFT status.", "APPLICANT");

        // Notify applicant that their application was created
        notificationClient.sendBoth(
                applicantId,
                app.getEmail(),
                "Loan Application Created — #" + app.getId(),
                "Hi " + app.getFullName() + ", your loan application #" + app.getId() +
                " has been created successfully in DRAFT status. " +
                "You can continue editing and submit when ready.",
                "APPLICATION_CREATED",
                app.getId()
        );

        return app;
    }

    /**
     * Updates a DRAFT application (no status change, no notification).
     */
    public LoanApplication update(Long id, Long applicantId, LoanApplicationRequest request) {
        LoanApplication app = getByIdAndApplicant(id, applicantId);
        if (app.getStatus() != ApplicationStatus.DRAFT) {
            throw new InvalidOperationException(
                    "Application cannot be updated. Only DRAFT applications can be modified.");
        }
        mapRequest(app, request);
        return repository.save(app);
    }

    /**
     * Submits a DRAFT application.
     * Fires APPLICATION_SUBMITTED notifications and returns a SubmitResponse
     * that guides the user to the document upload step.
     */
    public SubmitResponse submit(Long id, Long applicantId) {
        LoanApplication app = getByIdAndApplicant(id, applicantId);
        if (app.getStatus() != ApplicationStatus.DRAFT) {
            throw new InvalidOperationException(
                    "Application cannot be submitted. Only DRAFT applications can be submitted.");
        }

        app.setStatus(ApplicationStatus.SUBMITTED);
        app.setSubmittedAt(LocalDateTime.now());
        app = repository.save(app);
        recordStatusChange(app.getId(), ApplicationStatus.DRAFT, ApplicationStatus.SUBMITTED,
                "Application submitted by applicant.", "APPLICANT");

        // Notify applicant: application submitted
        notificationClient.sendBoth(
                applicantId,
                app.getEmail(),
                "Application #" + id + " Submitted Successfully",
                "Hi " + app.getFullName() + ", your loan application #" + id +
                " has been submitted and is now under review. " +
                "Please upload the required supporting documents to complete your application.",
                "APPLICATION_SUBMITTED",
                id
        );

        // Return guided next-step response directing user to document upload
        return new SubmitResponse(
                app,
                "Step 2 of 3: Please upload your supporting documents to complete your application.",
                "/documents/upload?applicationId=" + id,
                List.of(
                        "IDENTITY_PROOF",   // e.g. Aadhar / Passport
                        "INCOME_PROOF",     // e.g. Salary slips / ITR
                        "ADDRESS_PROOF",    // e.g. Utility bill
                        "BANK_STATEMENT"    // Last 6 months
                )
        );
    }

    public LoanApplication getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("LoanApplication", id));
    }

    public List<LoanApplication> getByApplicant(Long applicantId) {
        return repository.findByApplicantIdOrderByCreatedAtDesc(applicantId);
    }

    public List<LoanApplication> getAll() {
        return repository.findAll();
    }

    /**
     * Called by admin-service (via internal HTTP) to sync application status after a decision.
     * Does NOT fire notifications here — admin-service handles notification after decision.
     */
    public LoanApplication updateStatus(Long id, ApplicationStatus status, String remarks) {
        LoanApplication app = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("LoanApplication", id));
        ApplicationStatus prevStatus = app.getStatus();
        app.setStatus(status);
        if (remarks != null) app.setRemarks(remarks);
        app = repository.save(app);
        recordStatusChange(id, prevStatus, status,
                remarks != null ? remarks : "Status updated by admin.", "ADMIN");
        return app;
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private LoanApplication getByIdAndApplicant(Long id, Long applicantId) {
        LoanApplication app = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("LoanApplication", id));
        if (!app.getApplicantId().equals(applicantId)) {
            throw new ForbiddenException("You do not have access to this application.");
        }
        return app;
    }

    private void mapRequest(LoanApplication app, LoanApplicationRequest req) {
        app.setFullName(req.getFullName());
        app.setEmail(req.getEmail());
        app.setPhone(req.getPhone());
        app.setAddress(req.getAddress());
        app.setDateOfBirth(req.getDateOfBirth());
        app.setEmploymentType(req.getEmploymentType());
        app.setEmployerName(req.getEmployerName());
        app.setMonthlyIncome(req.getMonthlyIncome());
        app.setLoanType(req.getLoanType());
        app.setLoanAmount(req.getLoanAmount());
        app.setTenureMonths(req.getTenureMonths());
        app.setPurpose(req.getPurpose());
    }
}
