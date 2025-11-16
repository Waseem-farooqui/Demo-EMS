package com.was.employeemanagementsystem.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "leaves")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Leave {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "leave_type", nullable = false)
    private String leaveType; // Annual, Sick, Unpaid, etc.

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "number_of_days", nullable = false)
    private Integer numberOfDays;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "status", nullable = false)
    private String status; // PENDING, APPROVED, REJECTED

    @Column(name = "applied_date", nullable = false)
    private LocalDate appliedDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    @Column(name = "approval_date")
    private LocalDate approvalDate;

    @Column(name = "remarks")
    private String remarks;

    // Medical Certificate for sick leave (more than 2 days)
    @Lob
    @Column(name = "medical_certificate", columnDefinition = "LONGBLOB")
    private byte[] medicalCertificate;

    @Column(name = "certificate_file_name")
    private String certificateFileName;

    @Column(name = "certificate_content_type")
    private String certificateContentType;

    // Financial year for leave balance tracking (e.g., "2024-2025")
    @Column(name = "financial_year")
    private String financialYear;

    @Column(name = "organization_id")
    private Long organizationId;
}

