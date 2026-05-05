package com.finflow.application.dto;

import java.math.BigDecimal;

public class CreditScoreRequest {

    private Long applicationId;

    // Optional overrides — if not provided, pulled from the loan application
    private BigDecimal monthlyIncome;
    private BigDecimal loanAmount;
    private Integer tenureMonths;
    private String employmentType;

    // Extra inputs that are not on the application form
    private Integer existingEmis;         // total existing monthly EMI obligations (₹)
    private Integer creditHistoryMonths;  // months of credit history (0 if first-timer)
    private Integer numberOfDependents;
    private Boolean hasExistingLoan;
    private String documentStatus;        // e.g. "ALL_VERIFIED", "SOME_PENDING", "NONE_UPLOADED"

    // Getters and Setters
    public Long getApplicationId() { return applicationId; }
    public void setApplicationId(Long applicationId) { this.applicationId = applicationId; }
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
    public Integer getNumberOfDependents() { return numberOfDependents; }
    public void setNumberOfDependents(Integer numberOfDependents) { this.numberOfDependents = numberOfDependents; }
    public Boolean getHasExistingLoan() { return hasExistingLoan; }
    public void setHasExistingLoan(Boolean hasExistingLoan) { this.hasExistingLoan = hasExistingLoan; }
    public String getDocumentStatus() { return documentStatus; }
    public void setDocumentStatus(String documentStatus) { this.documentStatus = documentStatus; }
}
