package com.was.employeemanagementsystem.service;

import com.was.employeemanagementsystem.dto.PositionDTO;
import com.was.employeemanagementsystem.enums.HotelPositions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing hotel positions
 */
@Slf4j
@Service
public class PositionService {

    /**
     * Get all available hotel positions
     */
    public List<PositionDTO> getAllPositions() {
        return Arrays.stream(HotelPositions.values())
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Search positions by name (case-insensitive)
     */
    public List<PositionDTO> searchPositions(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAllPositions();
        }

        String lowerQuery = query.toLowerCase();
        return Arrays.stream(HotelPositions.values())
                .filter(position -> position.getPositionName().toLowerCase().contains(lowerQuery) ||
                                   position.getDescription().toLowerCase().contains(lowerQuery))
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Convert enum to DTO
     */
    private PositionDTO convertToDTO(HotelPositions position) {
        PositionDTO dto = new PositionDTO();
        dto.setName(position.getPositionName());
        dto.setDescription(position.getDescription());
        dto.setCategory(getCategory(position));
        return dto;
    }

    /**
     * Determine category based on position name
     */
    private String getCategory(HotelPositions position) {
        String name = position.name();
        if (name.startsWith("FOOD_") || name.startsWith("ASSISTANT_FB") || 
            name.startsWith("OUTLET_") || name.startsWith("BANQUET_") || 
            name.startsWith("BAR_") || name.startsWith("ROOM_SERVICE") ||
            name.startsWith("RESTAURANT_")) {
            return "Food & Beverage";
        } else if (name.startsWith("EXECUTIVE_HOUSEKEEPER") || 
                   name.startsWith("ASSISTANT_EXECUTIVE_HOUSEKEEPER") ||
                   name.startsWith("HOUSEKEEPING_") || name.startsWith("PUBLIC_AREA") ||
                   name.startsWith("LINEN_") || name.startsWith("NIGHT_SUPERVISOR") ||
                   name.startsWith("ROOM_ATTENDANT") || name.startsWith("LAUNDRY_") ||
                   name.startsWith("TURNDOWN_") || name.startsWith("UNIFORM_") ||
                   name.startsWith("TAILOR")) {
            return "Housekeeping";
        } else if (name.startsWith("FRONT_OFFICE") || name.startsWith("FRONT_DESK") ||
                   name.startsWith("GUEST_RELATIONS") || name.startsWith("RECEPTIONIST") ||
                   name.startsWith("TELEPHONE_") || name.startsWith("CASHIER") ||
                   name.startsWith("NIGHT_AUDITOR")) {
            return "Front Office";
        } else if (name.startsWith("CHIEF_CONCIERGE") || name.startsWith("ASSISTANT_CHIEF_CONCIERGE") ||
                   name.startsWith("CONCIERGE_") || name.startsWith("BELL_") ||
                   name.startsWith("DOORMAN") || name.startsWith("VALET_")) {
            return "Concierge";
        }
        return "Other";
    }
}

