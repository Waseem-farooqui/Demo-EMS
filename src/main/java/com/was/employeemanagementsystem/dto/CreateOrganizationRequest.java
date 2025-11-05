package com.was.employeemanagementsystem.dto;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

@Data
public class CreateOrganizationRequest {

    @NotBlank(message = "Organization name is required")
    private String organizationName;

    @NotBlank(message = "Super admin username is required")
    private String superAdminUsername;

    @NotBlank(message = "Super admin email is required")
    @Email(message = "Invalid email format")
    private String superAdminEmail;

    // Password is now optional - will be auto-generated if not provided
    private String password;

    @NotBlank(message = "Super admin full name is required")
    private String superAdminFullName;

    private String organizationDescription;
    private String contactEmail;
    private String contactPhone;
    private String address;
}

