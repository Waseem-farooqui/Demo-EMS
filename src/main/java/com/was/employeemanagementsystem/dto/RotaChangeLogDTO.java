package com.was.employeemanagementsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RotaChangeLogDTO {
    private Long id;
    private Long rotaId;
    private Long scheduleId;
    private Long employeeId;
    private String employeeName;
    private String changeType;
    private String oldValue;
    private String newValue;
    private String changeDescription;
    private LocalDateTime changedAt;
    private Long changedBy;
    private String changedByName;
    private String changedByRole;
    private String ipAddress;
    private String userAgent;
}

