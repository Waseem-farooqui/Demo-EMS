package com.was.employeemanagementsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserRequest {
    private String fullName;
    private String email;
    private String jobTitle;
    private String personType;
    private String role; // ADMIN or USER
    private Long departmentId; // Required for SUPER_ADMIN, auto-assigned for ADMIN
    private String reference;
    private String dateOfJoining;
    private String employmentStatus; // FULL_TIME, PART_TIME, CONTRACT, TEMPORARY
    private String contractType; // PERMANENT, TEMPORARY, FIXED_TERM
    private String workingTiming;
    private Integer holidayAllowance;

    // Extended profile fields
    private String personalEmail;
    private String phoneNumber;
    private String dateOfBirth;
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
    private String allottedOrganization;

    // Emergency Contact Information
    private String emergencyContactName;
    private String emergencyContactPhone;
    private String emergencyContactRelationship;

    private List<EmploymentRecordDTO> employmentRecords;
}

