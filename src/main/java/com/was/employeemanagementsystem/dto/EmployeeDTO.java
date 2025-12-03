package com.was.employeemanagementsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeDTO {
    private Long id;
    private String title;
    private String fullName;
    private String personType;
    private String workEmail;

    // Personal Information
    private String phoneNumber;
    private LocalDate dateOfBirth;
    private String nationality;
    private String presentAddress;
    private String previousAddress;
    private Boolean hasMedicalCondition;
    private String medicalConditionDetails;
    // Legacy next of kin fields (kept for backward compatibility)
    private String nextOfKinName;
    private String nextOfKinContact;
    private String nextOfKinAddress;
    private String bloodGroup;

    // Emergency Contact Information
    private String emergencyContactName;
    private String emergencyContactPhone;
    private String emergencyContactRelationship;

    // Job Information
    private String jobTitle;
    private String reference;
    private LocalDate dateOfJoining;
    private String employmentStatus;
    private String contractType;
    private String workingTiming;
    private Integer holidayAllowance;
    private String allottedOrganization;

    // Financial and Employment Details
    private String nationalInsuranceNumber;
    private String shareCode;
    private String bankAccountNumber;
    private String bankSortCode;
    private String bankAccountHolderName;
    private String bankName;
    private String wageRate;
    private String contractHours;

    // Relations
    private Long userId;
    private String username;  // Add username for display
    private String role;  // User role (USER, ADMIN, SUPER_ADMIN)
    private Long departmentId;
    private String departmentName;
    private List<EmploymentRecordDTO> employmentRecords;
    private List<NextOfKinDTO> nextOfKinList;
}

