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
    private String fullName;
    private String personType;
    private String workEmail;

    // Personal Information
    private String personalEmail;
    private String phoneNumber;
    private LocalDate dateOfBirth;
    private String nationality;
    private String address;
    private String presentAddress;
    private String previousAddress;
    private Boolean hasMedicalCondition;
    private String medicalConditionDetails;
    private String nextOfKinName;
    private String nextOfKinContact;
    private String nextOfKinAddress;
    private String bloodGroup;

    // Job Information
    private String jobTitle;
    private String reference;
    private LocalDate dateOfJoining;
    private String employmentStatus;
    private String contractType;
    private String workingTiming;
    private Integer holidayAllowance;
    private String allottedOrganization;

    // Relations
    private Long userId;
    private String username;  // Add username for display
    private String role;  // User role (USER, ADMIN, SUPER_ADMIN)
    private Long departmentId;
    private String departmentName;
    private List<EmploymentRecordDTO> employmentRecords;
}

