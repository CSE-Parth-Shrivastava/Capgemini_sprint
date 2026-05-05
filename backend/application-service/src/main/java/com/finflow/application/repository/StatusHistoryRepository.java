package com.finflow.application.repository;

import com.finflow.application.entity.StatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StatusHistoryRepository extends JpaRepository<StatusHistory, Long> {
    List<StatusHistory> findByApplicationIdOrderByChangedAtAsc(Long applicationId);
}
