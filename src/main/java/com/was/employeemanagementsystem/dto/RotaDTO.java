package com.was.employeemanagementsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RotaDTO {
    private Long id;
    private String hotelName;
    private String department;
    private String fileName;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDateTime uploadedDate;
    private String uploadedByName;
    private Integer totalEmployees;
}

