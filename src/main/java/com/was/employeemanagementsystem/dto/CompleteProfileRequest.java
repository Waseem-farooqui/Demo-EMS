package com.was.employeemanagementsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompleteProfileRequest {

    @NotBlank
    private String fullName;

    @NotBlank
    private String personType;

    @NotBlank
    private String jobTitle;

    private String reference;

    private LocalDate dateOfJoining;

    private String workingTiming;

    private Integer holidayAllowance;

    @NotBlank
    private String newPassword;

    @NotBlank
    private String confirmPassword;
}

