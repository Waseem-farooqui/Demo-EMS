package com.was.employeemanagementsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
}

