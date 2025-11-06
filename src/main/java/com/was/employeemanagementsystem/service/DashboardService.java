package com.was.employeemanagementsystem.service;

import com.was.employeemanagementsystem.dto.DashboardStatsDTO;
import com.was.employeemanagementsystem.entity.Attendance;
import com.was.employeemanagementsystem.entity.Document;
import com.was.employeemanagementsystem.entity.Employee;
import com.was.employeemanagementsystem.entity.Leave;
import com.was.employeemanagementsystem.entity.User;
import com.was.employeemanagementsystem.repository.AttendanceRepository;
import com.was.employeemanagementsystem.repository.DocumentRepository;
import com.was.employeemanagementsystem.repository.EmployeeRepository;
import com.was.employeemanagementsystem.repository.LeaveRepository;
import com.was.employeemanagementsystem.repository.UserRepository;
import com.was.employeemanagementsystem.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class DashboardService {

    private final EmployeeRepository employeeRepository;
    private final AttendanceRepository attendanceRepository;
    private final LeaveRepository leaveRepository;
    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;

    /**
     * Get comprehensive dashboard statistics for SUPER_ADMIN only
     * ROOT user has a separate organization-focused dashboard
     *
     * NOTE: SUPER_ADMIN users are NOT counted as employees - they are management
     */
    public DashboardStatsDTO getDashboardStats() {
        // ROOT user has NO access to employee dashboard
        if (securityUtils.isRoot()) {
            log.warn("⚠️ ROOT user attempted to access employee dashboard - Access denied");
            throw new AccessDeniedException("ROOT user cannot access employee dashboard. ROOT has a separate organization dashboard.");
        }

        // Only SUPER_ADMIN can access employee dashboard stats
        if (!securityUtils.isSuperAdmin()) {
            throw new AccessDeniedException("Only SUPER_ADMIN can access dashboard statistics");
        }

        log.info("📊 Generating dashboard statistics for SUPER_ADMIN");

        DashboardStatsDTO stats = new DashboardStatsDTO();

        // 1. Employees by Department (excluding SUPER_ADMINs)
        stats.setEmployeesByDepartment(getEmployeesByDepartment());

        // 2. Employees by Current Work Location (excluding SUPER_ADMINs)
        stats.setEmployeesByWorkLocation(getEmployeesByWorkLocation());

        // 3. Leave Statistics (excluding SUPER_ADMINs)
        Map<String, Long> leaveStats = getLeaveStatistics();
        stats.setEmployeesOnLeave(leaveStats.get("onLeave"));
        stats.setEmployeesWorking(leaveStats.get("working"));

        // 4. Document Expiry Statistics
        Map<String, Long> expiryStats = getDocumentExpiryStats();
        stats.setDocumentsExpiringIn30Days(expiryStats.get("expiring30"));
        stats.setDocumentsExpiringIn60Days(expiryStats.get("expiring60"));
        stats.setDocumentsExpired(expiryStats.get("expired"));

        // 5. Total Employees (excluding SUPER_ADMINs)
        stats.setTotalEmployees(countActualEmployees());

        log.info("✅ Dashboard stats generated successfully");
        return stats;
    }

    /**
     * Count actual employees excluding SUPER_ADMIN users
     * SUPER_ADMINs are management/admin accounts, not employees
     * ONLY counts employees from current user's organization
     */
    private long countActualEmployees() {
        User currentUser = securityUtils.getCurrentUser();
        if (currentUser == null || currentUser.getOrganizationId() == null) {
            log.warn("⚠️ No organization ID found for current user");
            return 0;
        }

        // Get employees ONLY from current user's organization
        List<Employee> orgEmployees = employeeRepository.findByOrganizationId(currentUser.getOrganizationId());

        long actualEmployees = orgEmployees.stream()
                .filter(this::isActualEmployee)
                .count();

        log.debug("📊 Total employees in organization {} (excluding SUPER_ADMINs): {}",
                currentUser.getOrganizationId(), actualEmployees);
        return actualEmployees;
    }

    /**
     * Get list of actual employees excluding SUPER_ADMINs
     * ONLY returns employees from current user's organization
     */
    private List<Employee> getActualEmployees() {
        User currentUser = securityUtils.getCurrentUser();
        if (currentUser == null || currentUser.getOrganizationId() == null) {
            log.warn("⚠️ No organization ID found for current user");
            return List.of();
        }

        // Get employees ONLY from current user's organization
        List<Employee> orgEmployees = employeeRepository.findByOrganizationId(currentUser.getOrganizationId());

        return orgEmployees.stream()
                .filter(this::isActualEmployee)
                .collect(Collectors.toList());
    }

    /**
     * Check if employee is an actual employee (not SUPER_ADMIN)
     * SUPER_ADMINs are organizational management, not regular employees
     */
    private boolean isActualEmployee(Employee employee) {
        if (employee.getUserId() == null) {
            return true; // No user account = consider as employee
        }

        User user = userRepository.findById(employee.getUserId()).orElse(null);
        if (user == null) {
            return true; // User not found = consider as employee
        }

        // Exclude SUPER_ADMIN from employee counts
        boolean isSuperAdmin = user.getRoles().contains("SUPER_ADMIN");
        return !isSuperAdmin;
    }

    /**
     * Get count of employees by department (excluding SUPER_ADMINs)
     */
    private Map<String, Long> getEmployeesByDepartment() {
        List<Employee> actualEmployees = getActualEmployees();

        Map<String, Long> departmentStats = actualEmployees.stream()
                .filter(emp -> emp.getDepartment() != null)
                .collect(Collectors.groupingBy(
                        emp -> emp.getDepartment().getName(),
                        Collectors.counting()
                ));

        // Add count for employees without department
        long noDepartment = actualEmployees.stream()
                .filter(emp -> emp.getDepartment() == null)
                .count();

        if (noDepartment > 0) {
            departmentStats.put("No Department", noDepartment);
        }

        log.info("📊 Employees by department (excluding SUPER_ADMINs): {}", departmentStats);
        return departmentStats;
    }

    /**
     * Get count of currently checked-in employees by work location (excluding SUPER_ADMINs)
     */
    private Map<String, Long> getEmployeesByWorkLocation() {
        List<Employee> actualEmployees = getActualEmployees();

        // Get all active check-ins (currently working)
        List<Attendance> activeAttendances = attendanceRepository.findByIsActiveTrue();

        // Filter active attendances to only include actual employees (not SUPER_ADMINs)
        List<Attendance> actualEmployeeAttendances = activeAttendances.stream()
                .filter(attendance -> attendance.getEmployee() != null && isActualEmployee(attendance.getEmployee()))
                .collect(Collectors.toList());

        Map<String, Long> locationStats = actualEmployeeAttendances.stream()
                .collect(Collectors.groupingBy(
                        attendance -> attendance.getWorkLocation().getDisplayName(),
                        Collectors.counting()
                ));

        // Add count for not checked in
        long totalEmployees = actualEmployees.size();
        long checkedIn = actualEmployeeAttendances.size();
        long notCheckedIn = totalEmployees - checkedIn;

        if (notCheckedIn > 0) {
            locationStats.put("Not Checked In", notCheckedIn);
        }

        log.info("📍 Employees by work location (excluding SUPER_ADMINs): {}", locationStats);
        return locationStats;
    }

    /**
     * Get leave statistics (excluding SUPER_ADMINs)
     */
    private Map<String, Long> getLeaveStatistics() {
        LocalDate today = LocalDate.now();
        List<Employee> actualEmployees = getActualEmployees();

        // Get approved leaves that include today
        List<Leave> todayLeaves = leaveRepository.findByStatus("APPROVED");

        // Filter leaves to only include actual employees
        long onLeave = todayLeaves.stream()
                .filter(leave -> leave.getEmployee() != null && isActualEmployee(leave.getEmployee()))
                .filter(leave -> !today.isBefore(leave.getStartDate()) && !today.isAfter(leave.getEndDate()))
                .count();

        long totalEmployees = actualEmployees.size();
        long working = totalEmployees - onLeave;

        Map<String, Long> stats = new HashMap<>();
        stats.put("onLeave", onLeave);
        stats.put("working", working);

        log.info("📅 Leave stats (excluding SUPER_ADMINs) - On Leave: {}, Working: {}", onLeave, working);
        return stats;
    }

    /**
     * Get document expiry statistics
     */
    private Map<String, Long> getDocumentExpiryStats() {
        LocalDate today = LocalDate.now();
        LocalDate in30Days = today.plusDays(30);
        LocalDate in60Days = today.plusDays(60);

        List<Document> allDocuments = documentRepository.findAll();

        // Documents expired (expiry date < today)
        long expired = allDocuments.stream()
                .filter(doc -> doc.getExpiryDate() != null)
                .filter(doc -> doc.getExpiryDate().isBefore(today))
                .count();

        // Documents expiring in next 30 days
        long expiring30 = allDocuments.stream()
                .filter(doc -> doc.getExpiryDate() != null)
                .filter(doc -> !doc.getExpiryDate().isBefore(today))
                .filter(doc -> doc.getExpiryDate().isBefore(in30Days) || doc.getExpiryDate().isEqual(in30Days))
                .count();

        // Documents expiring in 31-60 days
        long expiring60 = allDocuments.stream()
                .filter(doc -> doc.getExpiryDate() != null)
                .filter(doc -> doc.getExpiryDate().isAfter(in30Days))
                .filter(doc -> doc.getExpiryDate().isBefore(in60Days) || doc.getExpiryDate().isEqual(in60Days))
                .count();

        Map<String, Long> stats = new HashMap<>();
        stats.put("expired", expired);
        stats.put("expiring30", expiring30);
        stats.put("expiring60", expiring60);

        log.info("📄 Document expiry - Expired: {}, Expiring in 30 days: {}, Expiring in 60 days: {}",
                 expired, expiring30, expiring60);
        return stats;
    }
}

