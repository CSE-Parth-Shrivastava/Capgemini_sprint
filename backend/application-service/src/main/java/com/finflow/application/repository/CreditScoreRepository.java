package com.finflow.application.repository;

import com.finflow.application.entity.CreditScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CreditScoreRepository extends JpaRepository<CreditScore, Long> {
    Optional<CreditScore> findTopByApplicationIdOrderByAssessedAtDesc(Long applicationId);
    List<CreditScore> findByApplicationIdOrderByAssessedAtDesc(Long applicationId);
    List<CreditScore> findByApplicantIdOrderByAssessedAtDesc(Long applicantId);
}
