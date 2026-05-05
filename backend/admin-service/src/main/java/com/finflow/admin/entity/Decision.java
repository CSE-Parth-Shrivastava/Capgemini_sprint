package com.finflow.admin.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "decisions")
public class Decision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long applicationId;
    private Long adminId;

    @Enumerated(EnumType.STRING)
    private DecisionType decision;

    private String remarks;
    private BigDecimal approvedAmount;
    private BigDecimal interestRate;
    private Integer tenureMonths;
    private LocalDateTime decidedAt;

    @PrePersist
    protected void onCreate() {
        decidedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getApplicationId() { return applicationId; }
    public void setApplicationId(Long applicationId) { this.applicationId = applicationId; }
    public Long getAdminId() { return adminId; }
    public void setAdminId(Long adminId) { this.adminId = adminId; }
    public DecisionType getDecision() { return decision; }
    public void setDecision(DecisionType decision) { this.decision = decision; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public BigDecimal getApprovedAmount() { return approvedAmount; }
    public void setApprovedAmount(BigDecimal approvedAmount) { this.approvedAmount = approvedAmount; }
    public BigDecimal getInterestRate() { return interestRate; }
    public void setInterestRate(BigDecimal interestRate) { this.interestRate = interestRate; }
    public Integer getTenureMonths() { return tenureMonths; }
    public void setTenureMonths(Integer tenureMonths) { this.tenureMonths = tenureMonths; }
    public LocalDateTime getDecidedAt() { return decidedAt; }
}
