package com.was.employeemanagementsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDTO {
    private Map<String, Long> employeesByDepartment;
    private Map<String, Long> employeesByWorkLocation;
    private Long employeesOnLeave;
    private Long employeesWorking;
    private Long documentsExpiringIn30Days;
    private Long documentsExpiringIn60Days;
    private Long documentsExpired;
    private Long totalEmployees;
}

