package com.was.employeemanagementsystem.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Table(name = "alert_configurations")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlertConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_type", nullable = false, unique = true)
    private String documentType; // PASSPORT, VISA

    @Column(name = "alert_days_before", nullable = false)
    private Integer alertDaysBefore; // Days before expiry to send alert

    @Column(name = "alert_email", nullable = false)
    private String alertEmail;

    @Column(name = "enabled")
    private boolean enabled = true;

    public AlertConfiguration(String documentType, Integer alertDaysBefore, String alertEmail) {
        this.documentType = documentType;
        this.alertDaysBefore = alertDaysBefore;
        this.alertEmail = alertEmail;
        this.enabled = true;
    }
}

