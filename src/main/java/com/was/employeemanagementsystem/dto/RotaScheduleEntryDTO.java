package com.was.employeemanagementsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO for individual rota schedule entry (for editing)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RotaScheduleEntryDTO {
    private Long id;
    private Long rotaId;
    private Long employeeId;
    private String employeeName;
    private LocalDate scheduleDate;
    private String dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private String duty;
    private Boolean isOffDay;
}

