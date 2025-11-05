package com.was.employeemanagementsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RotaScheduleDTO {
    private Long employeeId;
    private String employeeName;
    private Map<LocalDate, DaySchedule> schedules; // Date -> Schedule details

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DaySchedule {
        private String dayOfWeek;
        private String duty;
        private String startTime;
        private String endTime;
        private Boolean isOffDay;
    }
}

