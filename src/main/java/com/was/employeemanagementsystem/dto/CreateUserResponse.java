package com.was.employeemanagementsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserResponse {
    private Long employeeId;
    private Long userId;
    private String fullName;
    private String email;
    private String username;
    private String temporaryPassword;
    private String role;
    private String departmentName;
    private String message;
    private boolean emailSent;
}

