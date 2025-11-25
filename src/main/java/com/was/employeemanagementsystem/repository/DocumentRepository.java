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

    // Eagerly fetch Employee and Department to avoid LazyInitializationException
    @Query("SELECT DISTINCT d FROM Document d " +
           "JOIN FETCH d.employee e " +
           "LEFT JOIN FETCH e.department " +
           "WHERE d.documentType = :documentType")
    List<Document> findByDocumentTypeWithEmployee(@Param("documentType") String documentType);

    // Find document by file hash for deduplication
    Document findByFileHash(String fileHash);

    // Count how many documents reference a specific file path
    @Query("SELECT COUNT(d) FROM Document d WHERE d.filePath = :filePath")
    long countByFilePath(@Param("filePath") String filePath);

    @Query("SELECT d FROM Document d WHERE d.expiryDate <= :expiryDate AND d.expiryDate >= :currentDate")
    List<Document> findDocumentsExpiringBefore(@Param("expiryDate") LocalDate expiryDate,
                                                 @Param("currentDate") LocalDate currentDate);
}

