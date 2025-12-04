package com.was.employeemanagementsystem.service;

import com.was.employeemanagementsystem.dto.AttendanceDTO;
import com.was.employeemanagementsystem.dto.EmployeeWorkSummaryDTO;
import com.was.employeemanagementsystem.entity.Attendance;
import com.was.employeemanagementsystem.entity.Attendance.WorkLocation;
import com.was.employeemanagementsystem.entity.Employee;
import com.was.employeemanagementsystem.entity.Leave;
import com.was.employeemanagementsystem.repository.AttendanceRepository;
import com.was.employeemanagementsystem.repository.EmployeeRepository;
import com.was.employeemanagementsystem.repository.LeaveRepository;
import com.was.employeemanagementsystem.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;
    private final LeaveRepository leaveRepository;
    private final SecurityUtils securityUtils;

    /**
     * Check in an employee
     */
    public AttendanceDTO checkIn(Long employeeId, WorkLocation workLocation, String notes) {
        log.info("Check-in request for employee ID: {} at location: {}", employeeId, workLocation);

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found with id: " + employeeId));

        // Check access permission
        if (!canAccessAttendance(employee)) {
            throw new RuntimeException("Access denied. You can only manage your own attendance.");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();

        // First, deactivate any old active check-ins from previous days
        Optional<Attendance> oldActiveCheckIn = attendanceRepository.findByEmployeeAndIsActiveTrue(employee);
        if (oldActiveCheckIn.isPresent()) {
            Attendance oldAttendance = oldActiveCheckIn.get();
            // If the old check-in is from a different day, deactivate it
            if (!oldAttendance.getWorkDate().equals(today)) {
                log.warn("Found active check-in from previous day ({}) for employee {}. Deactivating it.",
                        oldAttendance.getWorkDate(), employee.getFullName());
                oldAttendance.setIsActive(false);
                // If no checkout time, set it to end of that day
                if (oldAttendance.getCheckOutTime() == null) {
                    oldAttendance.setCheckOutTime(oldAttendance.getWorkDate().atTime(23, 59, 59));
                    // Calculate hours worked
                    Duration duration = Duration.between(oldAttendance.getCheckInTime(), oldAttendance.getCheckOutTime());
                    double hours = duration.toMinutes() / 60.0;
                    oldAttendance.setHoursWorked(Math.round(hours * 100.0) / 100.0);
                }
                attendanceRepository.save(oldAttendance);
            } else {
                // Already checked in today
                throw new RuntimeException("Already checked in today. Please check out first.");
            }
        }

        // Check if there's already an attendance record for today
        Optional<Attendance> existingTodayAttendance = attendanceRepository.findByEmployeeAndWorkDate(employee, today);
        
        Attendance attendance;
        if (existingTodayAttendance.isPresent()) {
            // Update existing record for today
            attendance = existingTodayAttendance.get();
            log.info("Found existing attendance record for today. Updating check-in time.");
            
            // If already checked out today, don't allow another check-in
            if (attendance.getCheckOutTime() != null) {
                throw new RuntimeException("Already checked in and out today. Cannot check in again on the same day.");
            }
            
            // Update check-in time and other details
            attendance.setCheckInTime(now);
            attendance.setWorkLocation(workLocation);
            attendance.setIsActive(true);
            if (notes != null && !notes.trim().isEmpty()) {
                String existingNotes = attendance.getNotes() != null ? attendance.getNotes() : "";
                attendance.setNotes(existingNotes + (existingNotes.isEmpty() ? "" : " | ") + "Re-check-in: " + notes);
            }
        } else {
            // Create new attendance record
            attendance = new Attendance();
            attendance.setEmployee(employee);
            attendance.setCheckInTime(now);
            attendance.setWorkDate(today);
            attendance.setWorkLocation(workLocation);
            attendance.setNotes(notes);
            attendance.setIsActive(true);
        }

        attendance = attendanceRepository.save(attendance);
        log.info("✓ Check-in successful for employee: {} at {}", employee.getFullName(), now);

        return convertToDTO(attendance);
    }

    /**
     * Check out an employee
     */
    public AttendanceDTO checkOut(Long employeeId, String notes) {
        log.info("Check-out request for employee ID: {}", employeeId);

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found with id: " + employeeId));

        // Check access permission
        if (!canAccessAttendance(employee)) {
            throw new RuntimeException("Access denied. You can only manage your own attendance.");
        }

        LocalDate today = LocalDate.now();
        
        // First try to find active check-in for today
        Optional<Attendance> activeCheckIn = attendanceRepository.findByEmployeeAndIsActiveTrue(employee);
        
        Attendance attendance;
        if (activeCheckIn.isPresent() && activeCheckIn.get().getWorkDate().equals(today)) {
            // Use the active check-in for today
            attendance = activeCheckIn.get();
        } else {
            // If no active check-in, try to find today's attendance record
            Optional<Attendance> todayAttendance = attendanceRepository.findByEmployeeAndWorkDate(employee, today);
            if (todayAttendance.isPresent()) {
                attendance = todayAttendance.get();
                // If already checked out, throw error
                if (attendance.getCheckOutTime() != null) {
                    throw new RuntimeException("Already checked out today. Cannot check out again.");
                }
            } else {
                throw new RuntimeException("No active check-in found for today. Please check in first.");
            }
        }

        LocalDateTime now = LocalDateTime.now();
        attendance.setCheckOutTime(now);
        attendance.setIsActive(false);

        // Calculate hours worked
        Duration duration = Duration.between(attendance.getCheckInTime(), now);
        double hours = duration.toMinutes() / 60.0;
        attendance.setHoursWorked(Math.round(hours * 100.0) / 100.0); // Round to 2 decimal places

        // Append notes if provided
        if (notes != null && !notes.trim().isEmpty()) {
            String existingNotes = attendance.getNotes() != null ? attendance.getNotes() : "";
            attendance.setNotes(existingNotes + (existingNotes.isEmpty() ? "" : " | ") + "Checkout: " + notes);
        }

        attendance = attendanceRepository.save(attendance);
        log.info("✓ Check-out successful for employee: {} at {}. Hours worked: {}",
                employee.getFullName(), now, attendance.getHoursWorked());

        return convertToDTO(attendance);
    }

    /**
     * Get current check-in status for an employee
     */
    public AttendanceDTO getCurrentStatus(Long employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found with id: " + employeeId));

        if (!canAccessAttendance(employee)) {
            throw new RuntimeException("Access denied.");
        }

        LocalDate today = LocalDate.now();
        // Only return active check-in if it's for today
        Optional<Attendance> activeCheckIn = attendanceRepository.findByEmployeeAndIsActiveTrue(employee);
        if (activeCheckIn.isPresent() && activeCheckIn.get().getWorkDate().equals(today)) {
            return convertToDTO(activeCheckIn.get());
        }
        
        // Also check if there's a record for today (even if not active)
        Optional<Attendance> todayAttendance = attendanceRepository.findByEmployeeAndWorkDate(employee, today);
        return todayAttendance.map(this::convertToDTO).orElse(null);
    }

    /**
     * Get attendance records for an employee within date range (max 3 months)
     */
    public List<AttendanceDTO> getAttendanceByDateRange(Long employeeId, LocalDate startDate, LocalDate endDate) {
        log.info("Fetching attendance for employee {} from {} to {}", employeeId, startDate, endDate);

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found with id: " + employeeId));

        if (!canAccessAttendance(employee)) {
            throw new RuntimeException("Access denied.");
        }

        // Validate date range (max 3 months)
        if (startDate.plusMonths(3).isBefore(endDate)) {
            throw new RuntimeException("Date range cannot exceed 3 months");
        }

        List<Attendance> attendanceList = attendanceRepository
                .findByEmployeeAndWorkDateBetweenOrderByWorkDateDesc(employee, startDate, endDate);

        return attendanceList.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get employee work summary with weekly hours
     */
    public EmployeeWorkSummaryDTO getEmployeeWorkSummary(Long employeeId) {
        log.info("Fetching work summary for employee ID: {}", employeeId);

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found with id: " + employeeId));

        // Check access permissions (admins can view any, users can view their own)
        if (!canAccessAttendance(employee)) {
            log.warn("Access denied: User {} attempted to access employee {} work summary",
                     securityUtils.getCurrentUsername(), employeeId);
            throw new RuntimeException("Access denied. You can only view your own attendance records.");
        }

        EmployeeWorkSummaryDTO summary = new EmployeeWorkSummaryDTO();
        summary.setEmployeeId(employee.getId());
        summary.setEmployeeName(employee.getFullName());
        summary.setEmail(employee.getWorkEmail());
        summary.setJobTitle(employee.getJobTitle());

        // Get current week dates (Monday to Sunday)
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)).plusDays(1);

        // Get this month dates
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate monthEnd = today.withDayOfMonth(today.lengthOfMonth()).plusDays(1);

        // Get weekly attendance
        List<Attendance> weeklyAttendance = attendanceRepository.getWeeklyAttendance(employee, weekStart, weekEnd);
        summary.setWeeklyAttendance(weeklyAttendance.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList()));

        // Calculate weekly hours
        Double weeklyHours = attendanceRepository.getTotalHoursWorked(employee, weekStart, today.plusDays(1));
        summary.setTotalHoursThisWeek(weeklyHours != null ? Math.round(weeklyHours * 100.0) / 100.0 : 0.0);

        // Calculate monthly hours
        Double monthlyHours = attendanceRepository.getTotalHoursWorked(employee, monthStart, today.plusDays(1));
        summary.setTotalHoursThisMonth(monthlyHours != null ? Math.round(monthlyHours * 100.0) / 100.0 : 0.0);

        // Count days worked
        summary.setDaysWorkedThisWeek((int) weeklyAttendance.stream()
                .filter(a -> a.getCheckOutTime() != null)
                .count());

        List<Attendance> monthlyAttendance = attendanceRepository
                .findByEmployeeAndWorkDateBetweenOrderByWorkDateDesc(employee, monthStart, today.plusDays(1));
        summary.setDaysWorkedThisMonth((int) monthlyAttendance.stream()
                .filter(a -> a.getCheckOutTime() != null)
                .count());

        // Determine current status - only consider today's active check-in
        Optional<Attendance> activeCheckIn = attendanceRepository.findByEmployeeAndIsActiveTrue(employee);
        if (activeCheckIn.isPresent() && activeCheckIn.get().getWorkDate().equals(today)) {
            summary.setCurrentStatus("CHECKED_IN");
        } else {
            // Check if on leave today
            List<Leave> todayLeaves = leaveRepository.findByEmployeeAndStatus(employee, "APPROVED");
            boolean onLeaveToday = todayLeaves.stream()
                    .anyMatch(leave -> !today.isBefore(leave.getStartDate()) && !today.isAfter(leave.getEndDate()));
            summary.setCurrentStatus(onLeaveToday ? "ON_LEAVE" : "CHECKED_OUT");
        }

        return summary;
    }

    /**
    /**
     * Get all active check-ins for today (Admin only)
     */
    public List<AttendanceDTO> getTodayActiveCheckIns() {
        if (!securityUtils.isAdminOrSuperAdmin()) {
            throw new RuntimeException("Access denied. Admin only.");
        }

        LocalDate today = LocalDate.now();
        List<Attendance> activeCheckIns = attendanceRepository.findActiveCheckInsForDate(today);

        return activeCheckIns.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Check if user can access attendance records
     */
    private boolean canAccessAttendance(Employee employee) {
        // Check if user is admin or super admin first
        boolean isAdminOrSuperAdmin = securityUtils.isAdminOrSuperAdmin();
        log.debug("Access check - isAdmin: {}, isSuperAdmin: {}, isAdminOrSuperAdmin: {}",
                  securityUtils.isAdmin(), securityUtils.isSuperAdmin(), isAdminOrSuperAdmin);

        if (isAdminOrSuperAdmin) {
            log.debug("Access granted: User has admin privileges");
            return true;
        }

        // Check if user can access their own records
        var currentUser = securityUtils.getCurrentUser();
        log.debug("Access check - currentUser: {}, currentUser.id: {}, employee.userId: {}, employee.id: {}",
                  currentUser != null ? currentUser.getUsername() : "null",
                  currentUser != null ? currentUser.getId() : "null",
                  employee.getUserId(),
                  employee.getId());

        boolean hasAccess = currentUser != null && employee.getUserId() != null
                && employee.getUserId().equals(currentUser.getId());

        log.debug("Access check result: {}", hasAccess);
        return hasAccess;
    }

    /**
     * Convert Attendance entity to DTO
     */
    private AttendanceDTO convertToDTO(Attendance attendance) {
        AttendanceDTO dto = new AttendanceDTO();
        dto.setId(attendance.getId());
        dto.setEmployeeId(attendance.getEmployee().getId());
        dto.setEmployeeName(attendance.getEmployee().getFullName());
        dto.setCheckInTime(attendance.getCheckInTime());
        dto.setCheckOutTime(attendance.getCheckOutTime());
        dto.setWorkDate(attendance.getWorkDate());
        dto.setWorkLocation(attendance.getWorkLocation());
        dto.setWorkLocationDisplay(attendance.getWorkLocation().getDisplayName());
        dto.setHoursWorked(attendance.getHoursWorked());
        dto.setNotes(attendance.getNotes());
        dto.setIsActive(attendance.getIsActive());
        dto.setCreatedAt(attendance.getCreatedAt());
        dto.setUpdatedAt(attendance.getUpdatedAt());
        return dto;
    }
}

