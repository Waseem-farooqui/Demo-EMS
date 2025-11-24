package com.was.employeemanagementsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmploymentRecordDTO {
    private Long id;
    private String jobTitle;
    private String employmentPeriod;
    private String employerName;
    private String employerAddress;
}

