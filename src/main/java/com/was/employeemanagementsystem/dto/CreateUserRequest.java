package com.was.employeemanagementsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserRequest {
    @NotBlank(message = "Title is required")
    private String title;
    
    @NotBlank(message = "Full name is required")
    private String fullName;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
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
    private String phoneNumber;
    private String dateOfBirth;
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

    // Emergency Contact Information
    private String emergencyContactName;
    private String emergencyContactPhone;
    private String emergencyContactRelationship;

    private List<EmploymentRecordDTO> employmentRecords;
    private List<NextOfKinDTO> nextOfKinList;
}

