package com.was.employeemanagementsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JwtResponse {
    private String token;
    private String type = "Bearer";
    private Long id;
    private String username;
    private String email;
    private Set<String> roles;
    private String organizationUuid;  // UUID for organization identification
    private boolean firstLogin;
    private boolean profileCompleted;
    private boolean temporaryPassword;

    public JwtResponse(String token, Long id, String username, String email, Set<String> roles,
                      String organizationUuid, boolean firstLogin, boolean profileCompleted, boolean temporaryPassword) {
        this.token = token;
        this.id = id;
        this.username = username;
        this.email = email;
        this.roles = roles;
        this.organizationUuid = organizationUuid;
        this.firstLogin = firstLogin;
        this.profileCompleted = profileCompleted;
        this.temporaryPassword = temporaryPassword;
    }
}

