package com.was.employeemanagementsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationCreationResponse {
    private OrganizationDTO organization;
    private CredentialsDTO credentials;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CredentialsDTO {
        private String username;
        private String password;
    }
}

