package com.was.employeemanagementsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RotaScheduleUpdateDTO {
    private Long scheduleId;
    private LocalDate scheduleDate;
    private String dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private String duty;
    private Boolean isOffDay;
    private String changeReason; // Optional reason for the change
}

