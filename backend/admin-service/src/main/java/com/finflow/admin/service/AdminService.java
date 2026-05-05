package com.finflow.admin.service;

import com.finflow.admin.client.NotificationClient;
import com.finflow.admin.dto.ApplicationDetails;
import com.finflow.admin.dto.DecisionRequest;
import com.finflow.admin.entity.Decision;
import com.finflow.admin.entity.DecisionType;
import com.finflow.admin.exception.ResourceNotFoundException;
import com.finflow.admin.repository.DecisionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminService.class);

    private final DecisionRepository decisionRepository;
    private final RestTemplate restTemplate;
    private final NotificationClient notificationClient;

    @Value("${services.application.url:http://localhost:8082}")
    private String applicationServiceUrl;

    public AdminService(DecisionRepository decisionRepository,
                        RestTemplate restTemplate,
                        NotificationClient notificationClient) {
        this.decisionRepository = decisionRepository;
        this.restTemplate       = restTemplate;
        this.notificationClient = notificationClient;
    }

    /**
     * Records an admin decision (APPROVED / REJECTED), syncs status to application-service,
     * and fires both IN_APP and EMAIL notifications to the applicant.
     */
    public Decision makeDecision(Long applicationId, Long adminId, DecisionRequest request) {

        Decision decision = new Decision();
        decision.setApplicationId(applicationId);
        decision.setAdminId(adminId);
        decision.setDecision(DecisionType.valueOf(request.getDecision().toUpperCase()));
        decision.setRemarks(request.getRemarks());
        decision.setApprovedAmount(request.getApprovedAmount());
        decision.setInterestRate(request.getInterestRate());
        decision.setTenureMonths(request.getTenureMonths());
        decision = decisionRepository.save(decision);

        ApplicationDetails appDetails = fetchApplicationDetails(applicationId, adminId);
        syncApplicationStatus(applicationId, adminId, decision.getDecision().name(), request.getRemarks());
        fireDecisionNotifications(decision, appDetails, request);

        return decision;
    }

    /**
     * Returns the decision for a given application, or throws if none exists.
     * Accessible to both admins (direct) and applicants (via application ownership check upstream).
     */
    public Optional<Decision> getDecisionByApplicationId(Long applicationId) {
        return decisionRepository.findByApplicationId(applicationId);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private ApplicationDetails fetchApplicationDetails(Long applicationId, Long adminId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-User-Id",   String.valueOf(adminId));
            headers.set("X-User-Role", "ADMIN");

            ApplicationDetails details = restTemplate.exchange(
                    applicationServiceUrl + "/applications/" + applicationId,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    ApplicationDetails.class
            ).getBody();

            log.info("Fetched application details: id={} applicantId={} email={}",
                    applicationId,
                    details != null ? details.getApplicantId() : "null",
                    details != null ? details.getEmail()        : "null");
            return details;
        } catch (Exception e) {
            log.error("Failed to fetch application details for id={}: {}", applicationId, e.getMessage());
            ApplicationDetails fallback = new ApplicationDetails();
            fallback.setId(applicationId);
            return fallback;
        }
    }

    private void syncApplicationStatus(Long applicationId, Long adminId,
                                        String status, String remarks) {
        try {
            Map<String, String> body = new HashMap<>();
            body.put("status", status);
            if (remarks != null) body.put("remarks", remarks);

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-User-Id",    String.valueOf(adminId));
            headers.set("X-User-Role",  "ADMIN");
            headers.set("Content-Type", "application/json");

            restTemplate.exchange(
                    applicationServiceUrl + "/applications/" + applicationId + "/status",
                    HttpMethod.PUT,
                    new HttpEntity<>(body, headers),
                    Void.class
            );
            log.info("Synced status={} to application-service for appId={}", status, applicationId);
        } catch (Exception e) {
            log.error("Failed to sync status for application {}: {}", applicationId, e.getMessage());
        }
    }

    private void fireDecisionNotifications(Decision decision,
                                            ApplicationDetails appDetails,
                                            DecisionRequest request) {
        Long   applicantUserId = appDetails != null ? appDetails.getApplicantId() : null;
        String applicantEmail  = appDetails != null ? appDetails.getEmail()       : null;
        String applicantName   = (appDetails != null && appDetails.getFullName() != null)
                ? appDetails.getFullName() : "Applicant";

        boolean approved  = decision.getDecision() == DecisionType.APPROVED;
        String  eventType = approved ? "APPLICATION_APPROVED" : "APPLICATION_REJECTED";

        String subject = approved
                ? "🎉 Congratulations! Your Loan Application is Approved"
                : "Update on Your Loan Application #" + decision.getApplicationId();

        String message;
        if (approved) {
            message = "Dear " + applicantName + ",\n\n" +
                      "We are pleased to inform you that your loan application #" +
                      decision.getApplicationId() + " has been APPROVED.\n\n" +
                      "Approved Amount : ₹" + decision.getApprovedAmount() + "\n" +
                      "Interest Rate   : " + decision.getInterestRate() + "% per annum\n" +
                      "Tenure          : " + decision.getTenureMonths() + " months\n" +
                      (request.getRemarks() != null ? "Remarks: " + request.getRemarks() + "\n" : "") +
                      "\nOur team will contact you with the next steps shortly.\n\nThank you for choosing FinFlow.";
        } else {
            message = "Dear " + applicantName + ",\n\n" +
                      "After careful review, we regret to inform you that your loan application #" +
                      decision.getApplicationId() + " has been REJECTED.\n\n" +
                      (request.getRemarks() != null ? "Reason: " + request.getRemarks() + "\n\n" : "\n") +
                      "You may reapply after addressing the above concerns. " +
                      "If you have questions, please contact our support team.\n\nThank you for choosing FinFlow.";
        }

        if (applicantUserId != null) {
            notificationClient.sendBoth(
                    applicantUserId,
                    applicantEmail,
                    subject,
                    message,
                    eventType,
                    decision.getApplicationId()
            );
        } else {
            log.warn("Could not send notification — applicantUserId is null for appId={}",
                    decision.getApplicationId());
        }
    }

    // ── Reports & queries ─────────────────────────────────────────────────────

    public Map<String, Object> getReports() {
        Map<String, Object> report = new HashMap<>();
        long total    = decisionRepository.count();
        long approved = decisionRepository.countByDecision(DecisionType.APPROVED);
        long rejected = decisionRepository.countByDecision(DecisionType.REJECTED);
        report.put("totalDecisions", total);
        report.put("approved",       approved);
        report.put("rejected",       rejected);
        report.put("approvalRate",   total > 0 ? String.format("%.1f%%", approved * 100.0 / total) : "0%");
        return report;
    }

    public List<Decision> getAllDecisions() {
        return decisionRepository.findAll();
    }
}