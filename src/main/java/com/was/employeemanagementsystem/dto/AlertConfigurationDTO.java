package com.was.employeemanagementsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlertConfigurationDTO {
    private Long id;
    private String documentType;
    private Integer alertDaysBefore;
    private String alertEmail;
    private boolean enabled;
    private String alertPriority; // EXPIRED, CRITICAL, WARNING, ATTENTION
    private String notificationType; // EMAIL, NOTIFICATION, BOTH
    private Long organizationId;
}

