package com.was.employeemanagementsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentDTO {
    private Long id;
    private String name;
    private String description;
    private String code;
    private Long managerId;
    private String managerName;
    private Boolean isActive;
    private Long employeeCount;
    private Boolean hasAdmin; // Indicates if department already has an ADMIN assigned
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

