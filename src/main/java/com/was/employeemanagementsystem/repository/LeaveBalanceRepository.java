package com.was.employeemanagementsystem.repository;

import com.was.employeemanagementsystem.entity.LeaveBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, Long> {

    List<LeaveBalance> findByEmployeeId(Long employeeId);

    List<LeaveBalance> findByEmployeeIdAndFinancialYear(Long employeeId, String financialYear);

    Optional<LeaveBalance> findByEmployeeIdAndFinancialYearAndLeaveType(
        Long employeeId, String financialYear, String leaveType);

    boolean existsByEmployeeIdAndFinancialYear(Long employeeId, String financialYear);
}

