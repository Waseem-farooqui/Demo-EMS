package com.was.employeemanagementsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentDTO {
    private Long id;
    private Long employeeId;
    private String employeeName;
    private String documentType;
    private String documentNumber;
    private String fileName;
    private String fileType;
    private String extractedText;
    private LocalDate issueDate;
    private LocalDate expiryDate;
    private String issuingCountry;
    private String fullName;
    private LocalDate dateOfBirth;
    private String nationality;

    // UK VISA specific fields
    private String companyName;
    private LocalDate dateOfCheck;
    private String referenceNumber;

    // CONTRACT specific fields
    private LocalDate contractDate; // Date of employment/contract start date
    private String placeOfWork; // Work location
    private String contractBetween; // Parties involved
    private String jobTitleContract; // Job title from contract

    private LocalDateTime uploadedDate;
    private Integer daysUntilExpiry;

    // Document view tracking
    private LocalDateTime lastViewedAt;
    private String lastViewedBy;
}

