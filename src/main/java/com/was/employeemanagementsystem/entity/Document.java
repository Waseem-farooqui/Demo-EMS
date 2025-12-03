package com.was.employeemanagementsystem.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "documents")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "document_type", nullable = false)
    private String documentType; // PASSPORT, VISA

    @Column(name = "document_number")
    private String documentNumber;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(name = "file_type")
    private String fileType;

    @Column(name = "file_hash", length = 32)
    private String fileHash; // MD5 hash for deduplication

    @Column(name = "issue_date")
    private LocalDate issueDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "issuing_country")
    private String issuingCountry;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "nationality")
    private String nationality;

    // UK VISA specific fields
    @Column(name = "visa_type")
    private String visaType; // e.g., Skilled Worker, Student, Family, etc.

    @Column(name = "company_name")
    private String companyName;

    @Column(name = "date_of_check")
    private LocalDate dateOfCheck;

    @Column(name = "reference_number")
    private String referenceNumber;

    // CONTRACT specific fields
    @Column(name = "contract_date")
    private LocalDate contractDate; // Date of employment/contract start date

    @Column(name = "place_of_work")
    private String placeOfWork; // Work location

    @Column(name = "contract_between")
    private String contractBetween; // Parties involved (e.g., "Company Ltd and Employee Name")

    @Column(name = "job_title_contract")
    private String jobTitleContract; // Job title from contract

    @Column(name = "uploaded_date", nullable = false)
    private LocalDateTime uploadedDate;

    @Column(name = "last_alert_sent")
    private LocalDateTime lastAlertSent;

    @Column(name = "alert_sent_count")
    private Integer alertSentCount = 0;

    // Document view tracking
    @Column(name = "last_viewed_at")
    private LocalDateTime lastViewedAt;

    @Column(name = "last_viewed_by")
    private String lastViewedBy; // Username of the person who last viewed the document
}

