package com.finflow.notification.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private String email;
    private String subject;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    private NotificationType type;

    @Column(name = "`read`")
    private boolean read = false;
    private boolean emailSent = false;

    private Long applicationId;
    private String eventType; // APPLICATION_SUBMITTED, APPROVED, REJECTED, DOCS_REQUESTED, etc.

    private LocalDateTime createdAt;
    private LocalDateTime sentAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public NotificationType getType() { return type; }
    public void setType(NotificationType type) { this.type = type; }
    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
    public boolean isEmailSent() { return emailSent; }
    public void setEmailSent(boolean emailSent) { this.emailSent = emailSent; }
    public Long getApplicationId() { return applicationId; }
    public void setApplicationId(Long applicationId) { this.applicationId = applicationId; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
}
