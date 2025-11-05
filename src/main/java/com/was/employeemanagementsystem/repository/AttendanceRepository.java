package com.was.employeemanagementsystem.repository;

import com.was.employeemanagementsystem.entity.Attendance;
import com.was.employeemanagementsystem.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    // Find active check-in for an employee (not checked out yet)
    Optional<Attendance> findByEmployeeAndIsActiveTrue(Employee employee);

    // Find all active check-ins (all employees currently checked in)
    List<Attendance> findByIsActiveTrue();

    // Find all attendance for an employee
    List<Attendance> findByEmployeeOrderByWorkDateDesc(Employee employee);

    // Find attendance for an employee within date range
    List<Attendance> findByEmployeeAndWorkDateBetweenOrderByWorkDateDesc(
            Employee employee, LocalDate startDate, LocalDate endDate);

    // Find attendance for specific date
    Optional<Attendance> findByEmployeeAndWorkDate(Employee employee, LocalDate workDate);

    // Find all attendance for a specific date
    List<Attendance> findByWorkDate(LocalDate workDate);

    // Find all attendance within date range
    List<Attendance> findByWorkDateBetween(LocalDate startDate, LocalDate endDate);

    // Count active check-ins by work location
    @Query("SELECT a.workLocation, COUNT(a) FROM Attendance a " +
           "WHERE a.isActive = true AND a.workDate = :date " +
           "GROUP BY a.workLocation")
    List<Object[]> countActiveByWorkLocation(@Param("date") LocalDate date);

    // Get total hours worked by employee in date range
    @Query("SELECT COALESCE(SUM(a.hoursWorked), 0) FROM Attendance a " +
           "WHERE a.employee = :employee AND a.workDate BETWEEN :startDate AND :endDate")
    Double getTotalHoursWorked(@Param("employee") Employee employee,
                               @Param("startDate") LocalDate startDate,
                               @Param("endDate") LocalDate endDate);

    // Get weekly hours for employee
    @Query("SELECT a FROM Attendance a " +
           "WHERE a.employee = :employee " +
           "AND a.workDate >= :weekStart " +
           "AND a.workDate < :weekEnd " +
           "ORDER BY a.workDate DESC")
    List<Attendance> getWeeklyAttendance(@Param("employee") Employee employee,
                                        @Param("weekStart") LocalDate weekStart,
                                        @Param("weekEnd") LocalDate weekEnd);

    // Count employees checked in today by location
    @Query("SELECT COUNT(DISTINCT a.employee) FROM Attendance a " +
           "WHERE a.workDate = :date AND a.isActive = true")
    Long countCheckedInToday(@Param("date") LocalDate date);

    // Get all active check-ins for today
    @Query("SELECT a FROM Attendance a " +
           "WHERE a.workDate = :date AND a.isActive = true")
    List<Attendance> findActiveCheckInsForDate(@Param("date") LocalDate date);
}

