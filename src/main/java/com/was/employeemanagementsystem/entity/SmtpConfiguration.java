package com.was.employeemanagementsystem.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "smtp_configuration", uniqueConstraints = {
    @UniqueConstraint(columnNames = "organization_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SmtpConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(nullable = false, length = 50)
    private String provider = "GMAIL"; // GMAIL, OUTLOOK, CUSTOM

    @Column(nullable = false)
    private String host;

    @Column(nullable = false)
    private Integer port = 587;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false, length = 500)
    private String password; // Encrypted password/token

    @Column(name = "from_email", nullable = false)
    private String fromEmail;

    @Column(name = "from_name")
    private String fromName = "Employee Management System";

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(name = "use_default", nullable = false)
    private Boolean useDefault = false; // If true, use env variables instead

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private Long createdBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

