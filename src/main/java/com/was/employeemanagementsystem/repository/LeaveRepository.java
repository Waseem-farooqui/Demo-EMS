package com.was.employeemanagementsystem.repository;

import com.was.employeemanagementsystem.entity.Employee;
import com.was.employeemanagementsystem.entity.Leave;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LeaveRepository extends JpaRepository<Leave, Long> {
    List<Leave> findByEmployeeId(Long employeeId);
    List<Leave> findByStatus(String status);
    List<Leave> findByEmployeeIdAndStatus(Long employeeId, String status);
    List<Leave> findByStartDateBetween(LocalDate startDate, LocalDate endDate);

    // New methods for attendance system
    List<Leave> findByEmployeeAndStatus(Employee employee, String status);

    @Query("SELECT COUNT(DISTINCT l.employee) FROM Leave l " +
           "WHERE l.status = 'APPROVED' " +
           "AND :date BETWEEN l.startDate AND l.endDate")
    Long countEmployeesOnLeaveToday(@Param("date") LocalDate date);
}

