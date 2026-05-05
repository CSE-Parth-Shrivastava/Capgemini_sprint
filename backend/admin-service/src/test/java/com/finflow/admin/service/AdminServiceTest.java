package com.finflow.admin.service;

import com.finflow.admin.client.NotificationClient;
import com.finflow.admin.dto.ApplicationDetails;
import com.finflow.admin.dto.DecisionRequest;
import com.finflow.admin.entity.Decision;
import com.finflow.admin.entity.DecisionType;
import com.finflow.admin.repository.DecisionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminService Unit Tests")
class AdminServiceTest {

    @Mock private DecisionRepository decisionRepository;
    @Mock private RestTemplate restTemplate;
    @Mock private NotificationClient notificationClient;

    @InjectMocks
    private AdminService adminService;

    private DecisionRequest approvedRequest;
    private DecisionRequest rejectedRequest;
    private ApplicationDetails appDetails;

    @BeforeEach
    void setUp() {
        approvedRequest = new DecisionRequest();
        approvedRequest.setDecision("APPROVED");
        approvedRequest.setRemarks("Good profile");
        approvedRequest.setApprovedAmount(new BigDecimal("500000"));
        approvedRequest.setInterestRate(new BigDecimal("10.5"));
        approvedRequest.setTenureMonths(24);

        rejectedRequest = new DecisionRequest();
        rejectedRequest.setDecision("REJECTED");
        rejectedRequest.setRemarks("Low income");

        appDetails = new ApplicationDetails();
        appDetails.setId(1L);
        appDetails.setApplicantId(10L);
        appDetails.setEmail("applicant@example.com");
        appDetails.setFullName("John Doe");

        when(restTemplate.exchange(
                anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(ApplicationDetails.class)))
                .thenReturn(ResponseEntity.ok(appDetails));
        when(restTemplate.exchange(
                anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(ResponseEntity.ok(null));
    }

    // ── makeDecision — APPROVED ───────────────────────────────────────────────

    @Test
    @DisplayName("makeDecision: saves APPROVED decision with all loan terms")
    void makeDecision_approved_savesDecisionWithLoanTerms() {
        Decision saved = decisionOf(DecisionType.APPROVED);
        when(decisionRepository.save(any(Decision.class))).thenReturn(saved);

        Decision result = adminService.makeDecision(1L, 10L, approvedRequest);

        assertThat(result.getDecision()).isEqualTo(DecisionType.APPROVED);
        verify(decisionRepository).save(any(Decision.class));
    }

    @Test
    @DisplayName("makeDecision: fires both notifications after APPROVED decision")
    void makeDecision_approved_firesNotification() {
        when(decisionRepository.save(any())).thenReturn(decisionOf(DecisionType.APPROVED));

        adminService.makeDecision(1L, 10L, approvedRequest);

        verify(notificationClient).sendBoth(
                eq(10L), eq("applicant@example.com"),
                anyString(), anyString(), eq("APPLICATION_APPROVED"), eq(1L));
    }

    // ── makeDecision — REJECTED ───────────────────────────────────────────────

    @Test
    @DisplayName("makeDecision: saves REJECTED decision")
    void makeDecision_rejected_savesDecision() {
        Decision saved = decisionOf(DecisionType.REJECTED);
        when(decisionRepository.save(any(Decision.class))).thenReturn(saved);

        Decision result = adminService.makeDecision(1L, 10L, rejectedRequest);

        assertThat(result.getDecision()).isEqualTo(DecisionType.REJECTED);
    }

    @Test
    @DisplayName("makeDecision: fires notifications after REJECTED decision")
    void makeDecision_rejected_firesNotification() {
        when(decisionRepository.save(any())).thenReturn(decisionOf(DecisionType.REJECTED));

        adminService.makeDecision(1L, 10L, rejectedRequest);

        verify(notificationClient).sendBoth(
                anyLong(), anyString(), anyString(), anyString(), eq("APPLICATION_REJECTED"), any());
    }

    // ── makeDecision — resilience ─────────────────────────────────────────────

    @Test
    @DisplayName("makeDecision: case-insensitive decision string is handled")
    void makeDecision_lowercaseDecision_parsedCorrectly() {
        approvedRequest.setDecision("approved");
        when(decisionRepository.save(any())).thenReturn(decisionOf(DecisionType.APPROVED));

        assertDoesNotThrow(() -> adminService.makeDecision(1L, 10L, approvedRequest));
    }

    @Test
    @DisplayName("makeDecision: invalid decision string throws IllegalArgumentException")
    void makeDecision_invalidDecisionString_throwsException() {
        approvedRequest.setDecision("MAYBE");

        assertThrows(IllegalArgumentException.class,
                () -> adminService.makeDecision(1L, 10L, approvedRequest));
    }

    @Test
    @DisplayName("makeDecision: returns decision even if application-service fetch fails")
    void makeDecision_applicationServiceFetchFails_stillReturnsDecision() {
        when(decisionRepository.save(any())).thenReturn(decisionOf(DecisionType.APPROVED));
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(ApplicationDetails.class)))
                .thenThrow(new RestClientException("Connection refused"));

        Decision result = assertDoesNotThrow(() -> adminService.makeDecision(1L, 10L, approvedRequest));

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("makeDecision: returns decision even if status sync fails")
    void makeDecision_statusSyncFails_stillReturnsDecision() {
        when(decisionRepository.save(any())).thenReturn(decisionOf(DecisionType.APPROVED));
        when(restTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(), eq(Void.class)))
                .thenThrow(new RestClientException("Service down"));

        Decision result = assertDoesNotThrow(() -> adminService.makeDecision(1L, 10L, approvedRequest));

        assertThat(result).isNotNull();
    }

    // ── getDecisionByApplicationId ────────────────────────────────────────────

    @Test
    @DisplayName("getDecisionByApplicationId: returns Optional with decision when found")
    void getDecisionByApplicationId_found_returnsOptional() {
        Decision d = decisionOf(DecisionType.APPROVED);
        when(decisionRepository.findByApplicationId(1L)).thenReturn(Optional.of(d));

        Optional<Decision> result = adminService.getDecisionByApplicationId(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getDecision()).isEqualTo(DecisionType.APPROVED);
    }

    @Test
    @DisplayName("getDecisionByApplicationId: returns empty Optional when no decision exists")
    void getDecisionByApplicationId_notFound_returnsEmpty() {
        when(decisionRepository.findByApplicationId(99L)).thenReturn(Optional.empty());

        assertThat(adminService.getDecisionByApplicationId(99L)).isEmpty();
    }

    @Test
    @DisplayName("getDecisionByApplicationId: returns decision with remarks")
    void getDecisionByApplicationId_returnsRemarksIntact() {
        Decision d = decisionOf(DecisionType.REJECTED);
        d.setRemarks("Low credit score — please reapply after 6 months.");
        when(decisionRepository.findByApplicationId(1L)).thenReturn(Optional.of(d));

        Optional<Decision> result = adminService.getDecisionByApplicationId(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getRemarks()).contains("Low credit score");
    }

    // ── getReports ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getReports: returns correct counts and 70% approval rate")
    void getReports_returnsCorrectStats() {
        when(decisionRepository.count()).thenReturn(10L);
        when(decisionRepository.countByDecision(DecisionType.APPROVED)).thenReturn(7L);
        when(decisionRepository.countByDecision(DecisionType.REJECTED)).thenReturn(3L);

        Map<String, Object> report = adminService.getReports();

        assertAll(
            () -> assertThat(report.get("totalDecisions")).isEqualTo(10L),
            () -> assertThat(report.get("approved")).isEqualTo(7L),
            () -> assertThat(report.get("rejected")).isEqualTo(3L),
            () -> assertThat(report.get("approvalRate").toString()).contains("70")
        );
    }

    @Test
    @DisplayName("getReports: returns 0% approval rate when no decisions exist")
    void getReports_noDecisions_returnsZeroPercent() {
        when(decisionRepository.count()).thenReturn(0L);
        when(decisionRepository.countByDecision(any())).thenReturn(0L);

        Map<String, Object> report = adminService.getReports();

        assertThat(report.get("approvalRate")).isEqualTo("0%");
    }

    // ── getAllDecisions ────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAllDecisions: returns list from repository")
    void getAllDecisions_returnsList() {
        when(decisionRepository.findAll()).thenReturn(List.of(decisionOf(DecisionType.APPROVED)));

        List<Decision> result = adminService.getAllDecisions();

        assertThat(result).hasSize(1);
        verify(decisionRepository).findAll();
    }

    @Test
    @DisplayName("getAllDecisions: returns empty list when no decisions")
    void getAllDecisions_empty_returnsEmptyList() {
        when(decisionRepository.findAll()).thenReturn(List.of());

        assertThat(adminService.getAllDecisions()).isEmpty();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Decision decisionOf(DecisionType type) {
        Decision d = new Decision();
        d.setApplicationId(1L);
        d.setAdminId(10L);
        d.setDecision(type);
        d.setRemarks(type == DecisionType.REJECTED ? "Low income" : "Good profile");
        return d;
    }
}