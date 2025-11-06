package com.was.employeemanagementsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

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

    // Job Information
    private String jobTitle;
    private String reference;
    private LocalDate dateOfJoining;
    private String employmentStatus;
    private String contractType;
    private String workingTiming;
    private Integer holidayAllowance;

    // Relations
    private Long userId;
    private String username;  // Add username for display
    private Long departmentId;
    private String departmentName;
}

