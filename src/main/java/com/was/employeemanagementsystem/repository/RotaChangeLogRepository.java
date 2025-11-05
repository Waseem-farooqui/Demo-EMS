package com.was.employeemanagementsystem.repository;

import com.was.employeemanagementsystem.entity.RotaChangeLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RotaChangeLogRepository extends JpaRepository<RotaChangeLog, Long> {

    List<RotaChangeLog> findByRotaIdOrderByChangedAtDesc(Long rotaId);

    List<RotaChangeLog> findByEmployeeIdOrderByChangedAtDesc(Long employeeId);

    List<RotaChangeLog> findByChangedByOrderByChangedAtDesc(Long changedBy);

    @Query("SELECT r FROM RotaChangeLog r ORDER BY r.changedAt DESC")
    List<RotaChangeLog> findAllOrderByChangedAtDesc();

    @Query("SELECT r FROM RotaChangeLog r WHERE r.changedAt >= :startDate ORDER BY r.changedAt DESC")
    List<RotaChangeLog> findRecentChanges(LocalDateTime startDate);

    @Query("SELECT r FROM RotaChangeLog r WHERE r.changedAt BETWEEN :startDate AND :endDate ORDER BY r.changedAt DESC")
    List<RotaChangeLog> findChangesBetween(LocalDateTime startDate, LocalDateTime endDate);

    List<RotaChangeLog> findTop10ByOrderByChangedAtDesc();
}

