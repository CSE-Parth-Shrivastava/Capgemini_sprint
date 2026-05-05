package com.finflow.application.repository;

import com.finflow.application.entity.ApplicationStatus;
import com.finflow.application.entity.LoanApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoanApplicationRepository extends JpaRepository<LoanApplication, Long> {
    List<LoanApplication> findByApplicantId(Long applicantId);
    List<LoanApplication> findByStatus(ApplicationStatus status);
    List<LoanApplication> findByApplicantIdOrderByCreatedAtDesc(Long applicantId);
}
