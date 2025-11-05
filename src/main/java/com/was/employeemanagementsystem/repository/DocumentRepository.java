package com.was.employeemanagementsystem.repository;

import com.was.employeemanagementsystem.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByEmployeeId(Long employeeId);
    List<Document> findByDocumentType(String documentType);

    @Query("SELECT d FROM Document d WHERE d.expiryDate <= :expiryDate AND d.expiryDate >= :currentDate")
    List<Document> findDocumentsExpiringBefore(@Param("expiryDate") LocalDate expiryDate,
                                                 @Param("currentDate") LocalDate currentDate);
}

