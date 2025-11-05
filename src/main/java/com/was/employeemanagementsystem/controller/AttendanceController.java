package com.was.employeemanagementsystem.controller;

import com.was.employeemanagementsystem.constants.AppConstants;
import com.was.employeemanagementsystem.dto.AttendanceDTO;
import com.was.employeemanagementsystem.dto.EmployeeWorkSummaryDTO;
import com.was.employeemanagementsystem.entity.Attendance.WorkLocation;
import com.was.employeemanagementsystem.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(AppConstants.API_ATTENDANCE_PATH)
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"http://localhost:4200", "http://127.0.0.1:4200"})
public class AttendanceController {

    private static final String REQUEST_KEY_EMPLOYEE_ID = "employeeId";
    private static final String REQUEST_KEY_WORK_LOCATION = "workLocation";
    private static final String REQUEST_KEY_NOTES = "notes";
    private static final String RESPONSE_KEY_ERROR = "error";
    private static final String RESPONSE_KEY_STATUS = "status";
    private static final String STATUS_CHECKED_OUT = "CHECKED_OUT";

    private final AttendanceService attendanceService;

    /**
     * Check in an employee
     */
    @PostMapping("/check-in")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> checkIn(@RequestBody Map<String, Object> request) {
        try {
            // Validate required parameters
            if (request == null || request.isEmpty()) {
                return createErrorResponse(HttpStatus.BAD_REQUEST, "Request body is required");
            }

            if (!request.containsKey(REQUEST_KEY_EMPLOYEE_ID) || request.get(REQUEST_KEY_EMPLOYEE_ID) == null) {
                return createErrorResponse(HttpStatus.BAD_REQUEST, "Employee ID is required");
            }

            if (!request.containsKey(REQUEST_KEY_WORK_LOCATION) || request.get(REQUEST_KEY_WORK_LOCATION) == null) {
                return createErrorResponse(HttpStatus.BAD_REQUEST, "Work location is required");
            }

            final Long employeeId = Long.valueOf(request.get(REQUEST_KEY_EMPLOYEE_ID).toString());
            final WorkLocation workLocation = WorkLocation.valueOf(request.get(REQUEST_KEY_WORK_LOCATION).toString());
            final String notes = request.get(REQUEST_KEY_NOTES) != null
                    ? request.get(REQUEST_KEY_NOTES).toString()
                    : null;

            final AttendanceDTO attendance = attendanceService.checkIn(employeeId, workLocation, notes);
            return ResponseEntity.ok(attendance);
        } catch (IllegalArgumentException e) {
            log.error("Invalid input during check-in: {}", e.getMessage());
            return createErrorResponse(HttpStatus.BAD_REQUEST, "Invalid input: " + getErrorMessage(e));
        } catch (Exception e) {
            log.error("Error during check-in", e);
            return createErrorResponse(HttpStatus.BAD_REQUEST, getErrorMessage(e));
        }
    }

    /**
     * Check out an employee
     */
    @PostMapping("/check-out")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> checkOut(@RequestBody Map<String, Object> request) {
        try {
            // Validate required parameters
            if (request == null || request.isEmpty()) {
                return createErrorResponse(HttpStatus.BAD_REQUEST, "Request body is required");
            }

            if (!request.containsKey(REQUEST_KEY_EMPLOYEE_ID) || request.get(REQUEST_KEY_EMPLOYEE_ID) == null) {
                return createErrorResponse(HttpStatus.BAD_REQUEST, "Employee ID is required");
            }

            final Long employeeId = Long.valueOf(request.get(REQUEST_KEY_EMPLOYEE_ID).toString());
            final String notes = request.get(REQUEST_KEY_NOTES) != null
                    ? request.get(REQUEST_KEY_NOTES).toString()
                    : null;

            final AttendanceDTO attendance = attendanceService.checkOut(employeeId, notes);
            return ResponseEntity.ok(attendance);
        } catch (IllegalArgumentException e) {
            log.error("Invalid input during check-out: {}", e.getMessage());
            return createErrorResponse(HttpStatus.BAD_REQUEST, "Invalid input: " + getErrorMessage(e));
        } catch (Exception e) {
            log.error("Error during check-out", e);
            return createErrorResponse(HttpStatus.BAD_REQUEST, getErrorMessage(e));
        }
    }

    /**
     * Get current check-in status for an employee
     */
    @GetMapping("/status/{employeeId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> getCurrentStatus(@PathVariable Long employeeId) {
        try {
            final AttendanceDTO status = attendanceService.getCurrentStatus(employeeId);
            if (status == null) {
                return ResponseEntity.ok(Map.of(RESPONSE_KEY_STATUS, STATUS_CHECKED_OUT));
            }
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            log.error("Error fetching status for employee {}", employeeId, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(RESPONSE_KEY_ERROR, e.getMessage()));
        }
    }

    /**
     * Get attendance records by date range
     */
    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> getAttendanceByDateRange(
            @PathVariable Long employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            final List<AttendanceDTO> attendanceList = attendanceService.getAttendanceByDateRange(
                    employeeId, startDate, endDate);
            return ResponseEntity.ok(attendanceList);
        } catch (Exception e) {
            log.error("Error fetching attendance records for employee {}", employeeId, e);
            return createErrorResponse(HttpStatus.BAD_REQUEST, getErrorMessage(e));
        }
    }

    /**
     * Get employee work summary with weekly hours
     */
    @GetMapping("/summary/{employeeId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> getEmployeeWorkSummary(@PathVariable Long employeeId) {
        try {
            final EmployeeWorkSummaryDTO summary = attendanceService.getEmployeeWorkSummary(employeeId);
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            log.error("Error fetching work summary for employee {}", employeeId, e);
            return createErrorResponse(HttpStatus.BAD_REQUEST, getErrorMessage(e));
        }
    }

    /**
     * Get all active check-ins for today (Admin and Super Admin only)
     */
    @GetMapping("/active-today")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> getTodayActiveCheckIns() {
        try {
            final List<AttendanceDTO> activeCheckIns = attendanceService.getTodayActiveCheckIns();
            return ResponseEntity.ok(activeCheckIns);
        } catch (Exception e) {
            log.error("Error fetching active check-ins", e);
            return createErrorResponse(HttpStatus.BAD_REQUEST, getErrorMessage(e));
        }
    }

    /**
     * Get available work locations
     */
    @GetMapping("/work-locations")
    public ResponseEntity<?> getWorkLocations() {
        try {
            final List<Map<String, String>> locations = List.of(
                    Map.of("value", AppConstants.WORK_LOCATION_OFFICE, "label", "Office"),
                    Map.of("value", AppConstants.WORK_LOCATION_HOME, "label", "Work From Home"),
                    Map.of("value", AppConstants.WORK_LOCATION_CLIENT_SITE, "label", "Client Site"),
                    Map.of("value", AppConstants.WORK_LOCATION_FIELD_WORK, "label", "Field Work"),
                    Map.of("value", AppConstants.WORK_LOCATION_HYBRID, "label", "Hybrid")
            );
            return ResponseEntity.ok(locations);
        } catch (Exception e) {
            log.error("Error fetching work locations", e);
            return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, getErrorMessage(e));
        }
    }

    /**
     * Helper method to create error response with null-safe error message
     */
    private ResponseEntity<?> createErrorResponse(HttpStatus status, String message) {
        final Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put(RESPONSE_KEY_ERROR, message);
        return ResponseEntity.status(status).body(errorResponse);
    }

    /**
     * Helper method to safely get error message from exception
     */
    private String getErrorMessage(Exception e) {
        return e.getMessage() != null ? e.getMessage() : "An unexpected error occurred";
    }
}

