package com.was.employeemanagementsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for showing preview of extracted records after ROTA upload
 * This allows users to review and confirm before final save
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RotaUploadPreviewDTO {
    private Long rotaId;
    private String hotelName;
    private String department;
    private String fileName;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDateTime uploadedDate;
    private String uploadedByName;
    private Integer totalSchedules;
    private Integer totalEmployees;
    private List<EmployeeSchedulePreview> employeeSchedules;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmployeeSchedulePreview {
        private Long employeeId;
        private String employeeName;
        private Integer totalDays;
        private Integer workDays;
        private Integer offDays;
        private List<DaySchedulePreview> schedules;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DaySchedulePreview {
        private LocalDate date;
        private String dayOfWeek;
        private String duty;
        private String startTime;
        private String endTime;
        private Boolean isOffDay;
    }
}

