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
}

