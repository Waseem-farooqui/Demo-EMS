package com.was.employeemanagementsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for hotel positions
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PositionDTO {
    private String name;
    private String description;
    private String category; // e.g., "Food & Beverage", "Housekeeping", "Front Office", "Concierge"
}

