package com.was.employeemanagementsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaveDTO {
    private Long id;
    private Long employeeId;
    private String employeeName;
    private String leaveType;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer numberOfDays;
    private String reason;
    private String status;
    private LocalDate appliedDate;
    private String approvedBy;
    private LocalDate approvalDate;
    private String remarks;
    private boolean hasMedicalCertificate;
    private String certificateFileName;
    private String financialYear;
}

