package com.was.employeemanagementsystem.service;

import com.was.employeemanagementsystem.entity.Document;
import com.was.employeemanagementsystem.entity.Employee;
import com.was.employeemanagementsystem.entity.Notification;
import com.was.employeemanagementsystem.entity.User;
import com.was.employeemanagementsystem.repository.DocumentRepository;
import com.was.employeemanagementsystem.repository.EmployeeRepository;
import com.was.employeemanagementsystem.repository.NotificationRepository;
import com.was.employeemanagementsystem.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class DocumentExpiryNotificationService {

    private final DocumentRepository documentRepository;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;

    public DocumentExpiryNotificationService(DocumentRepository documentRepository,
                                            NotificationRepository notificationRepository,
                                            UserRepository userRepository,
                                            EmployeeRepository employeeRepository) {
        this.documentRepository = documentRepository;
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.employeeRepository = employeeRepository;
    }

    /**
     * Scheduled task to check for expiring/expired documents
     * Runs every hour
     */
    @Scheduled(fixedRate = 3600000) // Every 1 hour
    public void checkDocumentExpiry() {
        log.info("🔍 Running scheduled document expiry check...");

        List<Document> allDocuments = documentRepository.findAll();
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        int expiredNotifications = 0;
        int nearExpiryNotifications = 0;

        for (Document document : allDocuments) {
            if (document.getExpiryDate() == null) {
                continue; // Skip documents without expiry date
            }

            long daysUntilExpiry = ChronoUnit.DAYS.between(today, document.getExpiryDate());

            // Check if document is expired
            if (daysUntilExpiry < 0) {
                // Document expired - send notification every 8 hours
                if (shouldSendExpiredNotification(document, now)) {
                    createExpiryNotification(document, "EXPIRED");
                    updateLastAlertSent(document, now);
                    expiredNotifications++;
                }
            }
            // Check if document is near expiry (within 30 days)
            else if (daysUntilExpiry <= 30 && daysUntilExpiry >= 0) {
                // Near expiry - send notification every 7 days
                if (shouldSendNearExpiryNotification(document, now)) {
                    createExpiryNotification(document, "NEAR_EXPIRY");
                    updateLastAlertSent(document, now);
                    nearExpiryNotifications++;
                }
            }
        }

        log.info("✅ Document expiry check complete. Expired: {}, Near expiry: {}",
                expiredNotifications, nearExpiryNotifications);
    }

    /**
     * Check if we should send notification for expired document (every 8 hours)
     */
    private boolean shouldSendExpiredNotification(Document document, LocalDateTime now) {
        if (document.getLastAlertSent() == null) {
            return true; // Never sent before
        }

        long hoursSinceLastAlert = ChronoUnit.HOURS.between(document.getLastAlertSent(), now);
        return hoursSinceLastAlert >= 8;
    }

    /**
     * Check if we should send notification for near-expiry document (every 7 days)
     */
    private boolean shouldSendNearExpiryNotification(Document document, LocalDateTime now) {
        if (document.getLastAlertSent() == null) {
            return true; // Never sent before
        }

        long daysSinceLastAlert = ChronoUnit.DAYS.between(document.getLastAlertSent(), now);
        return daysSinceLastAlert >= 7;
    }

    /**
     * Create expiry notification for appropriate users
     */
    private void createExpiryNotification(Document document, String notificationType) {
        Employee employee = document.getEmployee();
        if (employee == null) {
            log.warn("Document {} has no associated employee", document.getId());
            return;
        }

        List<Long> notifyUserIds = new ArrayList<>();

        // Find employee's user to get organization and role
        User employeeUser = employee.getUserId() != null
            ? userRepository.findById(employee.getUserId()).orElse(null)
            : null;

        if (employeeUser != null && employeeUser.getOrganizationId() != null) {
            Long organizationId = employeeUser.getOrganizationId();
            
            // Determine document owner's role
            boolean isUserRole = employeeUser.getRoles().contains("USER");
            boolean isAdminRole = employeeUser.getRoles().contains("ADMIN") && 
                                 !employeeUser.getRoles().contains("SUPER_ADMIN");

            if (isUserRole) {
                // USER role document: Notify Super Admin + Department Admin (if exists) + Document Owner
                log.info("📋 Document owner is USER role - notifying Super Admin + Department Admin + Owner");
                
                // 1. Notify all SUPER_ADMINs in the organization
                List<User> superAdmins = userRepository.findAll().stream()
                    .filter(u -> u.getRoles().contains("SUPER_ADMIN"))
                    .filter(u -> organizationId.equals(u.getOrganizationId()))
                    .collect(Collectors.toList());

                notifyUserIds.addAll(superAdmins.stream()
                    .map(User::getId)
                    .collect(Collectors.toList()));
                log.info("🔔 Found {} SUPER_ADMIN users in organization {}", superAdmins.size(), organizationId);

                // 2. Notify Department Admin (if employee has a department)
                if (employee.getDepartment() != null) {
                    List<Employee> deptEmployees = employeeRepository.findByDepartmentId(employee.getDepartment().getId());
                    for (Employee deptEmp : deptEmployees) {
                        if (deptEmp.getUserId() != null) {
                            User deptUser = userRepository.findById(deptEmp.getUserId()).orElse(null);
                            if (deptUser != null && deptUser.getRoles().contains("ADMIN") && 
                                !deptUser.getRoles().contains("SUPER_ADMIN")) {
                                notifyUserIds.add(deptUser.getId());
                                log.info("🔔 Added department ADMIN: {} (Department: {})",
                                    deptUser.getUsername(), employee.getDepartment().getName());
                            }
                        }
                    }
                }

                // 3. Notify the document owner
                notifyUserIds.add(employeeUser.getId());
                log.info("🔔 Added document owner (USER): {}", employeeUser.getUsername());
                
            } else if (isAdminRole) {
                // ADMIN role document: Notify Document Owner + Super Admin only
                log.info("📋 Document owner is ADMIN role - notifying Super Admin + Owner only");
                
                // 1. Notify all SUPER_ADMINs in the organization
                List<User> superAdmins = userRepository.findAll().stream()
                    .filter(u -> u.getRoles().contains("SUPER_ADMIN"))
                    .filter(u -> organizationId.equals(u.getOrganizationId()))
                    .collect(Collectors.toList());

                notifyUserIds.addAll(superAdmins.stream()
                    .map(User::getId)
                    .collect(Collectors.toList()));
                log.info("🔔 Found {} SUPER_ADMIN users in organization {}", superAdmins.size(), organizationId);

                // 2. Notify the document owner
                notifyUserIds.add(employeeUser.getId());
                log.info("🔔 Added document owner (ADMIN): {}", employeeUser.getUsername());
                
            } else {
                // No user account or unknown role - notify Super Admin only
                log.info("📋 Document owner has no user account or unknown role - notifying Super Admin only");
                
                List<User> superAdmins = userRepository.findAll().stream()
                    .filter(u -> u.getRoles().contains("SUPER_ADMIN"))
                    .filter(u -> organizationId.equals(u.getOrganizationId()))
                    .collect(Collectors.toList());

                notifyUserIds.addAll(superAdmins.stream()
                    .map(User::getId)
                    .collect(Collectors.toList()));
                log.info("🔔 Found {} SUPER_ADMIN users in organization {}", superAdmins.size(), organizationId);
            }
        } else {
            // Employee has no user account - notify Super Admin only (if we can find organization)
            log.warn("⚠️ Document owner has no user account - attempting to notify Super Admin only");
            // Note: Without user account, we can't determine organization, so skip notification
        }

        // Remove duplicates
        notifyUserIds = notifyUserIds.stream().distinct().collect(Collectors.toList());

        // Create notifications
        String title;
        String message;
        String type;

        if ("EXPIRED".equals(notificationType)) {
            title = "Document Expired!";
            message = String.format("%s's %s has expired on %s. Please renew immediately.",
                employee.getFullName(),
                document.getDocumentType(),
                document.getExpiryDate());
            type = "DOCUMENT_EXPIRED";
        } else {
            long daysUntilExpiry = ChronoUnit.DAYS.between(LocalDate.now(), document.getExpiryDate());
            title = "Document Expiring Soon";
            message = String.format("%s's %s will expire in %d days on %s. Please renew soon.",
                employee.getFullName(),
                document.getDocumentType(),
                daysUntilExpiry,
                document.getExpiryDate());
            type = "DOCUMENT_NEAR_EXPIRY";
        }

        for (Long userId : notifyUserIds) {
            Notification notification = new Notification();
            notification.setUserId(userId);
            notification.setType(type);
            notification.setTitle(title);
            notification.setMessage(message);
            notification.setReferenceId(document.getId());
            notification.setReferenceType("DOCUMENT");
            notification.setIsRead(false);
            notification.setOrganizationId(employeeUser != null ? employeeUser.getOrganizationId() : null);

            notificationRepository.save(notification);
            log.info("📄 Document expiry notification sent to user ID: {}", userId);
        }

        log.info("🔔 Total {} notifications created for document expiry", notifyUserIds.size());
    }

    /**
     * Update last alert sent timestamp
     */
    private void updateLastAlertSent(Document document, LocalDateTime now) {
        document.setLastAlertSent(now);
        document.setAlertSentCount(document.getAlertSentCount() == null ? 1 : document.getAlertSentCount() + 1);
        documentRepository.save(document);
    }

    /**
     * Manual trigger for testing - check all documents now
     */
    public void triggerManualCheck() {
        log.info("⚡ Manual document expiry check triggered");
        checkDocumentExpiry();
    }
}

