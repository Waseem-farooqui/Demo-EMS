package com.was.employeemanagementsystem.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SmtpConfigurationDTO {
    private Long id;
    private Long organizationId;
    private String provider; // GMAIL, OUTLOOK, CUSTOM
    private String host;
    private Integer port;
    private String username;
    private String password; // Only for create/update, not returned in GET
    private String fromEmail;
    private String fromName;
    private Boolean enabled;
    private Boolean useDefault;
    private Boolean isConfigured; // Helper field to check if SMTP is configured
}

