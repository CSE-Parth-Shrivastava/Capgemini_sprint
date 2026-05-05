package com.finflow.document.repository;

import com.finflow.document.entity.Document;
import com.finflow.document.entity.DocumentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByApplicationId(Long applicationId);
    List<Document> findByApplicantId(Long applicantId);
    List<Document> findByStatus(DocumentStatus status);
}
