package com.was.employeemanagementsystem.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Table(name = "leave_balances", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"employee_id", "financial_year", "leave_type"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaveBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "financial_year", nullable = false)
    private String financialYear; // e.g., "2024-2025"

    @Column(name = "leave_type", nullable = false)
    private String leaveType; // ANNUAL, SICK, CASUAL, OTHER

    @Column(name = "total_allocated", nullable = false)
    private Integer totalAllocated; // Total leaves allocated for this type

    @Column(name = "used_leaves", nullable = false)
    private Integer usedLeaves; // Leaves used

    @Column(name = "remaining_leaves", nullable = false)
    private Integer remainingLeaves; // Remaining leaves

    @Column(name = "organization_id")
    private Long organizationId;
}

