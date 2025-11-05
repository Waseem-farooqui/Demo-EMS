package com.was.employeemanagementsystem.service;

import com.was.employeemanagementsystem.entity.Document;
import com.was.employeemanagementsystem.entity.Employee;
import com.was.employeemanagementsystem.entity.Notification;
import com.was.employeemanagementsystem.entity.User;
import com.was.employeemanagementsystem.repository.DocumentRepository;
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
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class DocumentExpiryNotificationService {

    private final DocumentRepository documentRepository;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public DocumentExpiryNotificationService(DocumentRepository documentRepository,
                                            NotificationRepository notificationRepository,
                                            UserRepository userRepository) {
        this.documentRepository = documentRepository;
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
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

        // 1. Notify the employee themselves if they have a user account
        if (employee.getUserId() != null) {
            notifyUserIds.add(employee.getUserId());
        }

        // 2. Find employee's user to get organization
        User employeeUser = employee.getUserId() != null
            ? userRepository.findById(employee.getUserId()).orElse(null)
            : null;

        if (employeeUser != null && employeeUser.getOrganizationId() != null) {
            Long organizationId = employeeUser.getOrganizationId();

            // 3. Notify department ADMIN if employee has a department
            if (employee.getDepartment() != null) {
                List<User> deptAdmins = userRepository.findAll().stream()
                    .filter(u -> u.getRoles().contains("ADMIN"))
                    .filter(u -> !u.getRoles().contains("SUPER_ADMIN"))
                    .filter(u -> organizationId.equals(u.getOrganizationId()))
                    .collect(Collectors.toList());

                notifyUserIds.addAll(deptAdmins.stream()
                    .map(User::getId)
                    .collect(Collectors.toList()));
            }

            // 4. Notify all SUPER_ADMINs in the organization
            List<User> superAdmins = userRepository.findAll().stream()
                .filter(u -> u.getRoles().contains("SUPER_ADMIN"))
                .filter(u -> organizationId.equals(u.getOrganizationId()))
                .collect(Collectors.toList());

            notifyUserIds.addAll(superAdmins.stream()
                .map(User::getId)
                .collect(Collectors.toList()));
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

