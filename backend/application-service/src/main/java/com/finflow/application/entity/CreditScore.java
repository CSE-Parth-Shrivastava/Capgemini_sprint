package com.finflow.application.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "credit_scores")
public class CreditScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long applicationId;
    private Long applicantId;

    // Composite score (300–850 range, CIBIL-style)
    private Integer score;

    // Sub-scores (each 0–100)
    private Integer incomeStabilityScore;
    private Integer debtToIncomeScore;
    private Integer employmentScore;
    private Integer documentVerificationScore;
    private Integer loanAffordabilityScore;

    // Input factors stored for audit
    private BigDecimal monthlyIncome;
    private BigDecimal loanAmount;
    private Integer tenureMonths;
    private String employmentType;
    private Integer existingEmis;          // existing monthly obligations in INR
    private Integer creditHistoryMonths;   // months of credit history (0 if none)

    // AI-generated narrative
    @Column(columnDefinition = "TEXT")
    private String aiAnalysis;

    // EXCELLENT / GOOD / FAIR / POOR
    private String grade;

    // STRONGLY_RECOMMENDED / RECOMMENDED / CONDITIONAL / NOT_RECOMMENDED
    private String recommendation;

    private LocalDateTime assessedAt;

    @PrePersist
    protected void onCreate() {
        assessedAt = LocalDateTime.now();
    }

    // ── Getters & Setters ────────────────────────────────────────────────────
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getApplicationId() { return applicationId; }
    public void setApplicationId(Long applicationId) { this.applicationId = applicationId; }
    public Long getApplicantId() { return applicantId; }
    public void setApplicantId(Long applicantId) { this.applicantId = applicantId; }
    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }
    public Integer getIncomeStabilityScore() { return incomeStabilityScore; }
    public void setIncomeStabilityScore(Integer incomeStabilityScore) { this.incomeStabilityScore = incomeStabilityScore; }
    public Integer getDebtToIncomeScore() { return debtToIncomeScore; }
    public void setDebtToIncomeScore(Integer debtToIncomeScore) { this.debtToIncomeScore = debtToIncomeScore; }
    public Integer getEmploymentScore() { return employmentScore; }
    public void setEmploymentScore(Integer employmentScore) { this.employmentScore = employmentScore; }
    public Integer getDocumentVerificationScore() { return documentVerificationScore; }
    public void setDocumentVerificationScore(Integer documentVerificationScore) { this.documentVerificationScore = documentVerificationScore; }
    public Integer getLoanAffordabilityScore() { return loanAffordabilityScore; }
    public void setLoanAffordabilityScore(Integer loanAffordabilityScore) { this.loanAffordabilityScore = loanAffordabilityScore; }
    public BigDecimal getMonthlyIncome() { return monthlyIncome; }
    public void setMonthlyIncome(BigDecimal monthlyIncome) { this.monthlyIncome = monthlyIncome; }
    public BigDecimal getLoanAmount() { return loanAmount; }
    public void setLoanAmount(BigDecimal loanAmount) { this.loanAmount = loanAmount; }
    public Integer getTenureMonths() { return tenureMonths; }
    public void setTenureMonths(Integer tenureMonths) { this.tenureMonths = tenureMonths; }
    public String getEmploymentType() { return employmentType; }
    public void setEmploymentType(String employmentType) { this.employmentType = employmentType; }
    public Integer getExistingEmis() { return existingEmis; }
    public void setExistingEmis(Integer existingEmis) { this.existingEmis = existingEmis; }
    public Integer getCreditHistoryMonths() { return creditHistoryMonths; }
    public void setCreditHistoryMonths(Integer creditHistoryMonths) { this.creditHistoryMonths = creditHistoryMonths; }
    public String getAiAnalysis() { return aiAnalysis; }
    public void setAiAnalysis(String aiAnalysis) { this.aiAnalysis = aiAnalysis; }
    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }
    public String getRecommendation() { return recommendation; }
    public void setRecommendation(String recommendation) { this.recommendation = recommendation; }
    public LocalDateTime getAssessedAt() { return assessedAt; }
}
