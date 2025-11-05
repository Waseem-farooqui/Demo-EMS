package com.was.employeemanagementsystem.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "rota_schedules")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RotaSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "rota_id", nullable = false)
    private Rota rota;

    @Column(nullable = false)
    private Long employeeId;

    @Column(nullable = false)
    private String employeeName;

    @Column(nullable = false)
    private LocalDate scheduleDate;

    @Column(nullable = false)
    private String dayOfWeek;

    private LocalTime startTime;

    private LocalTime endTime;

    @Column(nullable = false)
    private String duty; // Original duty string from ROTA (e.g., "08:00-18:00", "17:00-03:00", "Set-Ups")

    private Boolean isOffDay = false;
}

