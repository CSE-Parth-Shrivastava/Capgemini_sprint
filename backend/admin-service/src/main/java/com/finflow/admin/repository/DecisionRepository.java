package com.finflow.admin.repository;

import com.finflow.admin.entity.Decision;
import com.finflow.admin.entity.DecisionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DecisionRepository extends JpaRepository<Decision, Long> {
    Optional<Decision> findByApplicationId(Long applicationId);
    List<Decision> findByDecision(DecisionType decision);
    long countByDecision(DecisionType decision);
}
