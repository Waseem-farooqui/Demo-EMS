package com.was.employeemanagementsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaveBalanceDTO {
    private Long id;
    private Long employeeId;
    private String employeeName;
    private String financialYear;
    private String leaveType;
    private Integer totalAllocated;
    private Integer usedLeaves;
    private Integer remainingLeaves;
}

