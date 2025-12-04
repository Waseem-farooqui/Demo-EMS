package com.was.employeemanagementsystem.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Table(name = "alert_configurations",
       uniqueConstraints = {
           @UniqueConstraint(columnNames = {"organization_id", "document_type", "alert_priority"})
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlertConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_type", nullable = false)
    private String documentType; // PASSPORT, VISA, etc.

    @Column(name = "alert_days_before", nullable = false)
    private Integer alertDaysBefore; // Days before expiry to send alert

    @Column(name = "alert_email")
    private String alertEmail;

    @Column(name = "enabled")
    private boolean enabled = true;

    @Column(name = "alert_priority", nullable = false)
    private String alertPriority; // EXPIRED, CRITICAL, WARNING, ATTENTION

    @Column(name = "notification_type", nullable = false)
    private String notificationType; // EMAIL, NOTIFICATION, BOTH

    @Column(name = "alert_frequency")
    private String alertFrequency; // ONCE, DAILY, HOURLY

    @Column(name = "repeat_until_resolved")
    private boolean repeatUntilResolved = false; // Keep sending alerts until document is updated

    @Column(name = "organization_id", nullable = false)
    private Long organizationId; // Multi-tenancy support

    public AlertConfiguration(String documentType, Integer alertDaysBefore, String alertEmail,
                            String alertPriority, String notificationType) {
        this.documentType = documentType;
        this.alertDaysBefore = alertDaysBefore;
        this.alertEmail = alertEmail;
        this.alertPriority = alertPriority;
        this.notificationType = notificationType;
        this.enabled = true;
        this.alertFrequency = "ONCE"; // Default to once
        this.repeatUntilResolved = false;
    }
}

