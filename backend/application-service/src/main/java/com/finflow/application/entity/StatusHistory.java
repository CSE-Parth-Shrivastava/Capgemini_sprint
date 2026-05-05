package com.finflow.application.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "application_status_history")
public class StatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long applicationId;

    @Enumerated(EnumType.STRING)
    private ApplicationStatus fromStatus;

    @Enumerated(EnumType.STRING)
    private ApplicationStatus toStatus;

    private String remarks;
    private String changedBy; // "APPLICANT" or "ADMIN"

    private LocalDateTime changedAt;

    @PrePersist
    protected void onCreate() {
        changedAt = LocalDateTime.now();
    }

    // ── Getters & Setters ────────────────────────────────────────────────────
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getApplicationId() { return applicationId; }
    public void setApplicationId(Long applicationId) { this.applicationId = applicationId; }
    public ApplicationStatus getFromStatus() { return fromStatus; }
    public void setFromStatus(ApplicationStatus fromStatus) { this.fromStatus = fromStatus; }
    public ApplicationStatus getToStatus() { return toStatus; }
    public void setToStatus(ApplicationStatus toStatus) { this.toStatus = toStatus; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public String getChangedBy() { return changedBy; }
    public void setChangedBy(String changedBy) { this.changedBy = changedBy; }
    public LocalDateTime getChangedAt() { return changedAt; }
}
