package com.was.employeemanagementsystem.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OrganizationDTO {
    private Long id;
    private String organizationUuid;  // Unique identifier for organization
    private String name;
    private String description;
    private String contactEmail;
    private String contactPhone;
    private String address;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String logoUrl;
}

