package com.was.employeemanagementsystem.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "rota_change_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RotaChangeLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long rotaId;

    @Column(nullable = false)
    private Long scheduleId; // The specific schedule entry that was changed

    @Column(nullable = false)
    private Long employeeId;

    @Column(nullable = false)
    private String employeeName;

    @Column(nullable = false)
    private String changeType; // CREATED, UPDATED, DELETED, REPLACED

    @Column(columnDefinition = "TEXT")
    private String oldValue; // JSON or string representation of old values

    @Column(columnDefinition = "TEXT")
    private String newValue; // JSON or string representation of new values

    @Column(columnDefinition = "TEXT")
    private String changeDescription; // Human-readable description

    @Column(nullable = false)
    private LocalDateTime changedAt;

    @Column(nullable = false)
    private Long changedBy; // User ID who made the change

    @Column(nullable = false)
    private String changedByName;

    @Column
    private String changedByRole; // ADMIN, SUPER_ADMIN, etc.

    @Column
    private String ipAddress;

    @Column
    private String userAgent;
}

