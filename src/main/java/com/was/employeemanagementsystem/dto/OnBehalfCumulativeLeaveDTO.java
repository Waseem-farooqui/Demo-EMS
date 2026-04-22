package com.was.employeemanagementsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Approved leave-day totals used for admin/super-admin on-behalf applications
 * (cumulative cap per financial year).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OnBehalfCumulativeLeaveDTO {
    private String financialYear;
    private int approvedDaysThisFinancialYear;
    private int cumulativeCap;
    private int remainingDaysUnderCap;
}
