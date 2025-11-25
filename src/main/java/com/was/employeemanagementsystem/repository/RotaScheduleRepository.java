package com.was.employeemanagementsystem.repository;

import com.was.employeemanagementsystem.entity.RotaSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface RotaScheduleRepository extends JpaRepository<RotaSchedule, Long> {

    List<RotaSchedule> findByRotaId(Long rotaId);

    List<RotaSchedule> findByEmployeeIdAndScheduleDateBetween(Long employeeId, LocalDate start, LocalDate end);

    List<RotaSchedule> findByEmployeeIdInAndScheduleDateBetween(List<Long> employeeIds, LocalDate start, LocalDate end);

    List<RotaSchedule> findByRotaIdAndEmployeeIdIn(Long rotaId, List<Long> employeeIds);

    // Find all rota schedules for a specific employee
    List<RotaSchedule> findByEmployeeId(Long employeeId);

    // Delete all rota schedules for a specific employee
    @Modifying
    @Transactional
    @Query("DELETE FROM RotaSchedule r WHERE r.employeeId = :employeeId")
    void deleteByEmployeeId(@Param("employeeId") Long employeeId);
}

