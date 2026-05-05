package com.finflow.application.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "loan_applications")
public class LoanApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long applicantId;

    // Personal Info
    private String fullName;
    private String email;
    private String phone;
    private String address;
    private String dateOfBirth;

    // Employment Info
    private String employmentType;
    private String employerName;
    private BigDecimal monthlyIncome;

    // Loan Details
    private LoanType loanType;
    private BigDecimal loanAmount;
    private Integer tenureMonths;
    private String purpose;

    @Enumerated(EnumType.STRING)
    private ApplicationStatus status = ApplicationStatus.DRAFT;

    private String remarks;

    private LocalDateTime submittedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getApplicantId() { return applicantId; }
    public void setApplicantId(Long applicantId) { this.applicantId = applicantId; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(String dateOfBirth) { this.dateOfBirth = dateOfBirth; }
    public String getEmploymentType() { return employmentType; }
    public void setEmploymentType(String employmentType) { this.employmentType = employmentType; }
    public String getEmployerName() { return employerName; }
    public void setEmployerName(String employerName) { this.employerName = employerName; }
    public BigDecimal getMonthlyIncome() { return monthlyIncome; }
    public void setMonthlyIncome(BigDecimal monthlyIncome) { this.monthlyIncome = monthlyIncome; }
    public LoanType getLoanType() {	return loanType;}
	public void setLoanType(LoanType loanType) {this.loanType = loanType;}
	public BigDecimal getLoanAmount() { return loanAmount; }
    public void setLoanAmount(BigDecimal loanAmount) { this.loanAmount = loanAmount; }
    public Integer getTenureMonths() { return tenureMonths; }
    public void setTenureMonths(Integer tenureMonths) { this.tenureMonths = tenureMonths; }
    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
    public ApplicationStatus getStatus() { return status; }
    public void setStatus(ApplicationStatus status) { this.status = status; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
