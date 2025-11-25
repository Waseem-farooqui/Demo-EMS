package com.was.employeemanagementsystem.controller;

import com.was.employeemanagementsystem.constants.AppConstants;
import com.was.employeemanagementsystem.dto.PositionDTO;
import com.was.employeemanagementsystem.service.PositionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for hotel positions
 */
@Slf4j
@RestController
@RequestMapping("/api/positions")
@CrossOrigin(origins = "${app.cors.origins}")
@RequiredArgsConstructor
public class PositionController {

    private final PositionService positionService;

    /**
     * Get all available positions
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<List<PositionDTO>> getAllPositions() {
        log.debug("Fetching all positions");
        List<PositionDTO> positions = positionService.getAllPositions();
        return ResponseEntity.ok(positions);
    }

    /**
     * Search positions by query
     */
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<List<PositionDTO>> searchPositions(@RequestParam(required = false) String q) {
        log.debug("Searching positions with query: {}", q);
        List<PositionDTO> positions = positionService.searchPositions(q);
        return ResponseEntity.ok(positions);
    }
}

