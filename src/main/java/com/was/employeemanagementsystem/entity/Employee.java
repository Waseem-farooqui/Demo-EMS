package com.was.employeemanagementsystem.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "employees", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"work_email", "organization_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title")
    private String title; // Mr, Mrs, Miss, Ms, Dr, Prof, etc.

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "person_type", nullable = false)
    private String personType;

    @Column(name = "work_email", nullable = false)
    private String workEmail;

    // Personal Information
    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "nationality")
    private String nationality;

    @Column(name = "present_address", columnDefinition = "TEXT")
    private String presentAddress;

    @Column(name = "previous_address", columnDefinition = "TEXT")
    private String previousAddress;

    @Column(name = "has_medical_condition")
    private Boolean hasMedicalCondition = false;

    @Column(name = "medical_condition_details", columnDefinition = "TEXT")
    private String medicalConditionDetails;

    // Legacy next of kin fields (kept for backward compatibility, deprecated)
    @Column(name = "next_of_kin_name")
    @Deprecated
    private String nextOfKinName;

    @Column(name = "next_of_kin_contact")
    @Deprecated
    private String nextOfKinContact;

    @Column(name = "next_of_kin_address", columnDefinition = "TEXT")
    @Deprecated
    private String nextOfKinAddress;

    // Job Information
    @Column(name = "job_title")
    private String jobTitle;

    @Column(name = "reference")
    private String reference;

    @Column(name = "date_of_joining", nullable = false)
    private LocalDate dateOfJoining;

    @Column(name = "employment_status")
    private String employmentStatus; // e.g., "FULL_TIME", "PART_TIME", "CONTRACT"

    @Column(name = "contract_type")
    private String contractType; // e.g., "PERMANENT", "TEMPORARY", "FIXED_TERM"

    @Column(name = "working_timing")
    private String workingTiming;

    @Column(name = "holiday_allowance")
    private Integer holidayAllowance;

    @Column(name = "allotted_organization")
    private String allottedOrganization;

    // Financial and Employment Details
    @Column(name = "national_insurance_number")
    private String nationalInsuranceNumber;

    @Column(name = "share_code")
    private String shareCode;

    @Column(name = "bank_account_number")
    private String bankAccountNumber;

    @Column(name = "bank_sort_code")
    private String bankSortCode;

    @Column(name = "bank_account_holder_name")
    private String bankAccountHolderName;

    @Column(name = "bank_name")
    private String bankName;

    @Column(name = "wage_rate")
    private String wageRate; // e.g., "£15.50/hour" or "£30,000/year"

    @Column(name = "contract_hours")
    private String contractHours; // e.g., "40 hours/week" or "Full-time"

    @Column(name = "comments", columnDefinition = "TEXT")
    private String comments; // General comments/notes about the employee

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "organization_id")
    private Long organizationId;

    @Column(name = "organization_uuid", length = 36)
    private String organizationUuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", insertable = false, updatable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Leave> leaves = new ArrayList<>();

    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<EmploymentRecord> employmentRecords = new ArrayList<>();

    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<NextOfKin> nextOfKinList = new ArrayList<>();
}

