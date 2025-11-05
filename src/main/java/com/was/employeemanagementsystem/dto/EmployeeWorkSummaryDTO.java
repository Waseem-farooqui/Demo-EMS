package com.was.employeemanagementsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeWorkSummaryDTO {
    private Long employeeId;
    private String employeeName;
    private String email;
    private String jobTitle;
    private Double totalHoursThisWeek;
    private Double totalHoursThisMonth;
    private List<AttendanceDTO> weeklyAttendance;
    private Integer daysWorkedThisWeek;
    private Integer daysWorkedThisMonth;
    private String currentStatus; // CHECKED_IN, CHECKED_OUT, ON_LEAVE
}

