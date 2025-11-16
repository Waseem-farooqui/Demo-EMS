package com.was.employeemanagementsystem.service;

import com.was.employeemanagementsystem.entity.AlertConfiguration;
import com.was.employeemanagementsystem.entity.Document;
import com.was.employeemanagementsystem.entity.Employee;
import com.was.employeemanagementsystem.entity.Notification;
import com.was.employeemanagementsystem.entity.User;
import com.was.employeemanagementsystem.repository.AlertConfigurationRepository;
import com.was.employeemanagementsystem.repository.DocumentRepository;
import com.was.employeemanagementsystem.repository.EmployeeRepository;
import com.was.employeemanagementsystem.repository.NotificationRepository;
import com.was.employeemanagementsystem.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DocumentExpiryScheduler {

    private final DocumentRepository documentRepository;
    private final AlertConfigurationRepository alertConfigurationRepository;
    private final EmailService emailService;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;

    public DocumentExpiryScheduler(DocumentRepository documentRepository,
                                  AlertConfigurationRepository alertConfigurationRepository,
                                  EmailService emailService,
                                  NotificationRepository notificationRepository,
                                  UserRepository userRepository,
                                  EmployeeRepository employeeRepository) {
        this.documentRepository = documentRepository;
        this.alertConfigurationRepository = alertConfigurationRepository;
        this.emailService = emailService;
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.employeeRepository = employeeRepository;
    }

    // Run every day at 9:00 AM
    @Scheduled(cron = "0 0 9 * * ?")
    public void checkDocumentExpiry() {
        log.info("⏰ Running scheduled document expiry check at: {}", LocalDateTime.now());
        performExpiryCheck(false); // Not manual - respect frequency rules
    }

    // Run every hour for HOURLY frequency alerts
    @Scheduled(cron = "0 0 * * * ?")
    public void checkDocumentExpiryHourly() {
        log.info("⏰ Running hourly document expiry check at: {}", LocalDateTime.now());
        performExpiryCheck(false); // Not manual - respect frequency rules
    }

    private void performExpiryCheck(boolean isManualTest) {
        log.info("📋 === Starting Document Expiry Check {} ===", isManualTest ? "(MANUAL TEST - BYPASSING FREQUENCY CHECKS)" : "");
        List<AlertConfiguration> configurations = alertConfigurationRepository.findAll();
        log.info("📋 Found {} alert configurations", configurations.size());

        for (AlertConfiguration config : configurations) {
            log.info("🔍 Checking configuration: Type={}, Priority={}, Days={}, Frequency={}, NotificationType={}, Enabled={}",
                config.getDocumentType(), config.getAlertPriority(), config.getAlertDaysBefore(),
                config.getAlertFrequency(), config.getNotificationType(), config.isEnabled());

            if (!config.isEnabled()) {
                log.info("⏭️ Skipping disabled configuration");
                continue;
            }

            LocalDate currentDate = LocalDate.now();

            List<Document> documents = documentRepository.findByDocumentType(config.getDocumentType());
            log.info("📄 Found {} documents of type {}", documents.size(), config.getDocumentType());

            for (Document document : documents) {
                if (document.getExpiryDate() == null) {
                    log.debug("⏭️ Skipping document {} - no expiry date", document.getId());
                    continue;
                }

                long daysUntilExpiry = ChronoUnit.DAYS.between(currentDate, document.getExpiryDate());
                log.info("📅 Document ID {}: Employee={}, ExpiryDate={}, DaysUntilExpiry={}",
                    document.getId(), document.getEmployee().getFullName(),
                    document.getExpiryDate(), daysUntilExpiry);

                // Check if document is expiring within the alert period
                if (daysUntilExpiry > 0 && daysUntilExpiry <= config.getAlertDaysBefore()) {

                log.info("⚠️ Document is within alert period ({}  days <= {} days)",
                    daysUntilExpiry, config.getAlertDaysBefore());

                // Determine if we should send alert based on frequency configuration
                boolean shouldSendAlert;
                if (isManualTest) {
                    log.info("🧪 MANUAL TEST MODE - Bypassing frequency check");
                    shouldSendAlert = true;
                } else {
                    shouldSendAlert = shouldSendAlertBasedOnFrequency(document, config, currentDate);
                }
                log.info("🔔 Should send alert: {}", shouldSendAlert);

                if (shouldSendAlert) {
                        log.info("📤 Sending expiry alert...");
                        sendExpiryAlert(document, config, (int) daysUntilExpiry);

                        // Update alert tracking
                        document.setLastAlertSent(LocalDateTime.now());
                        document.setAlertSentCount(
                            document.getAlertSentCount() != null ? document.getAlertSentCount() + 1 : 1
                        );
                        documentRepository.save(document);
                    }
                } else if (daysUntilExpiry <= 0 && config.isRepeatUntilResolved()) {
                    log.info("⏰ Document EXPIRED but repeat enabled, checking if should send...");
                    // Document expired but repeat until resolved is enabled
                    boolean shouldSendAlert;
                    if (isManualTest) {
                        log.info("🧪 MANUAL TEST MODE - Bypassing frequency check for expired document");
                        shouldSendAlert = true;
                    } else {
                        shouldSendAlert = shouldSendAlertBasedOnFrequency(document, config, currentDate);
                    }
                    log.info("🔔 Should send alert for expired doc: {}", shouldSendAlert);

                    if (shouldSendAlert) {
                        log.info("📤 Sending expiry alert for EXPIRED document...");
                        sendExpiryAlert(document, config, (int) daysUntilExpiry);
                        document.setLastAlertSent(LocalDateTime.now());
                        document.setAlertSentCount(
                            document.getAlertSentCount() != null ? document.getAlertSentCount() + 1 : 1
                        );
                        documentRepository.save(document);
                    }
                } else {
                    log.debug("⏭️ Document not in alert period: daysUntilExpiry={}, alertDaysBefore={}, repeatEnabled={}",
                        daysUntilExpiry, config.getAlertDaysBefore(), config.isRepeatUntilResolved());
                }
            }
        }
        log.info("✅ === Document Expiry Check Complete ===");
    }

    /**
     * Determine if alert should be sent based on configured frequency
     */
    private boolean shouldSendAlertBasedOnFrequency(Document document, AlertConfiguration config, LocalDate currentDate) {
        String frequency = config.getAlertFrequency() != null ? config.getAlertFrequency() : "ONCE";

        log.info("   🔍 Checking frequency rules:");
        log.info("      Configured frequency: {}", frequency);
        log.info("      Last alert sent: {}", document.getLastAlertSent());
        log.info("      Alert sent count: {}", document.getAlertSentCount());

        if (document.getLastAlertSent() == null) {
            // Never sent before, always send
            log.info("      ✅ Result: SEND (never sent before)");
            return true;
        }

        LocalDateTime lastAlert = document.getLastAlertSent();
        LocalDateTime now = LocalDateTime.now();

        switch (frequency) {
            case "HOURLY":
                // Send alert every hour
                long hoursSinceLastAlert = ChronoUnit.HOURS.between(lastAlert, now);
                log.info("      Hours since last alert: {}", hoursSinceLastAlert);
                boolean shouldSendHourly = hoursSinceLastAlert >= 1;
                log.info("      {} Result: {} (need >= 1 hour)", shouldSendHourly ? "✅" : "❌", shouldSendHourly ? "SEND" : "SKIP");
                return shouldSendHourly;

            case "DAILY":
                // Send alert once per day
                long daysSinceLastAlert = ChronoUnit.DAYS.between(lastAlert.toLocalDate(), currentDate);
                log.info("      Days since last alert: {}", daysSinceLastAlert);
                boolean shouldSendDaily = daysSinceLastAlert >= 1;
                log.info("      {} Result: {} (need >= 1 day)", shouldSendDaily ? "✅" : "❌", shouldSendDaily ? "SEND" : "SKIP");
                return shouldSendDaily;

            case "ONCE":
            default:
                // Send only once, then wait 7 days before next alert
                long daysSinceLastAlertOnce = ChronoUnit.DAYS.between(lastAlert.toLocalDate(), currentDate);
                log.info("      Days since last alert: {}", daysSinceLastAlertOnce);
                boolean shouldSendOnce = daysSinceLastAlertOnce >= 7;
                log.info("      {} Result: {} (need >= 7 days for ONCE)", shouldSendOnce ? "✅" : "❌", shouldSendOnce ? "SEND" : "SKIP");
                return shouldSendOnce;
        }
    }

    private void sendExpiryAlert(Document document, AlertConfiguration config, int daysUntilExpiry) {
        String employeeName = document.getEmployee().getFullName();
        String documentType = document.getDocumentType();
        String documentNumber = document.getDocumentNumber() != null ? document.getDocumentNumber() : "N/A";
        String expiryDate = document.getExpiryDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        // Send email alert if configured (wrapped in try-catch to prevent email failures from blocking in-app notifications)
        if ("EMAIL".equals(config.getNotificationType()) || "BOTH".equals(config.getNotificationType())) {
            try {
                // Collect all email recipients: Admin, Super Admin, and Document Owner
                List<String> emailRecipients = new ArrayList<>();
                
                // 1. Add configured alert email if provided
                if (config.getAlertEmail() != null && !config.getAlertEmail().isEmpty()) {
                    emailRecipients.add(config.getAlertEmail());
                }
                
                // 2. Find all ADMIN and SUPER_ADMIN users in the same organization
                String organizationUuid = document.getEmployee().getOrganizationUuid();
                List<User> adminUsers = userRepository.findAll().stream()
                    .filter(u -> {
                        Set<String> roles = u.getRoles();
                        return (roles.contains("ADMIN") || roles.contains("SUPER_ADMIN")) &&
                               organizationUuid != null && organizationUuid.equals(u.getOrganizationUuid());
                    })
                    .collect(Collectors.toList());
                
                for (User adminUser : adminUsers) {
                    if (adminUser.getEmail() != null && !adminUser.getEmail().isEmpty()) {
                        emailRecipients.add(adminUser.getEmail());
                        log.info("📧 Added admin email: {} (Role: {})", adminUser.getEmail(), adminUser.getRoles());
                    }
                }
                
                // 3. Add document owner's email if they have a user account
                Employee employee = document.getEmployee();
                if (employee.getUserId() != null) {
                    User employeeUser = userRepository.findById(employee.getUserId()).orElse(null);
                    if (employeeUser != null && employeeUser.getEmail() != null && !employeeUser.getEmail().isEmpty()) {
                        emailRecipients.add(employeeUser.getEmail());
                        log.info("📧 Added document owner email: {} (Employee: {})", employeeUser.getEmail(), employeeName);
                    }
                }
                
                // Remove duplicates
                emailRecipients = emailRecipients.stream().distinct().collect(Collectors.toList());
                
                if (!emailRecipients.isEmpty()) {
                    emailService.sendDocumentExpiryAlertToMultiple(
                        emailRecipients.toArray(new String[0]),
                        employeeName,
                        documentType,
                        documentNumber,
                        expiryDate,
                        daysUntilExpiry
                    );
                    log.info("📧 Sent email alert to {} recipient(s) for {} - Employee: {}, Days until expiry: {}",
                        emailRecipients.size(), documentType, employeeName, daysUntilExpiry);
                } else {
                    log.warn("⚠️ No email recipients found for document expiry alert");
                }
            } catch (Exception e) {
                log.error("❌ Failed to send email alert (but continuing with in-app notifications): {}", e.getMessage());
                log.error("   Document: {}, Employee: {}", documentType, employeeName);
            }
        }

        // Create in-app notifications if configured
        if ("NOTIFICATION".equals(config.getNotificationType()) || "BOTH".equals(config.getNotificationType())) {
            String organizationUuid = document.getEmployee().getOrganizationUuid();

            // Find the employee's user account
            Employee employee = document.getEmployee();
            User employeeUser = employee.getUserId() != null ?
                userRepository.findById(employee.getUserId()).orElse(null) : null;

            List<User> notifyUsers = new ArrayList<>();

            // 1. Find all SUPER_ADMIN users in the same organization
            List<User> superAdmins = userRepository.findAll().stream()
                    .filter(u -> u.getRoles().contains("SUPER_ADMIN"))
                    .filter(u -> organizationUuid != null && organizationUuid.equals(u.getOrganizationUuid()))
                    .collect(Collectors.toList());

            notifyUsers.addAll(superAdmins);
            log.info("🔔 Found {} SUPER_ADMIN users in organization {}", superAdmins.size(), organizationUuid);

            // 2. Find all ADMIN users in the same organization (not just department admins)
            List<User> admins = userRepository.findAll().stream()
                    .filter(u -> {
                        Set<String> roles = u.getRoles();
                        return roles.contains("ADMIN") && !roles.contains("SUPER_ADMIN") &&
                               organizationUuid != null && organizationUuid.equals(u.getOrganizationUuid());
                    })
                    .collect(Collectors.toList());

            notifyUsers.addAll(admins);
            log.info("🔔 Found {} ADMIN users in organization {}", admins.size(), organizationUuid);

            // 3. Notify the document owner (if they have a user account)
            if (employeeUser != null) {
                notifyUsers.add(employeeUser);
                log.info("🔔 Added document owner: {} (User ID: {}, Roles: {})",
                    employeeUser.getUsername(), employeeUser.getId(), employeeUser.getRoles());
            }

            // Remove duplicates
            notifyUsers = notifyUsers.stream().distinct().collect(Collectors.toList());

            String title = String.format("🔴 %s Expiring Soon", documentType);
            String message;
            if (daysUntilExpiry < 0) {
                message = String.format("%s's %s (No: %s) EXPIRED %d days ago on %s",
                        employeeName, documentType, documentNumber, Math.abs(daysUntilExpiry), expiryDate);
            } else if (daysUntilExpiry == 0) {
                message = String.format("%s's %s (No: %s) EXPIRES TODAY",
                        employeeName, documentType, documentNumber);
            } else {
                message = String.format("%s's %s (No: %s) will expire in %d days on %s",
                        employeeName, documentType, documentNumber, daysUntilExpiry, expiryDate);
            }

            int successCount = 0;
            for (User user : notifyUsers) {
                try {
                    Notification notification = new Notification();
                    notification.setUserId(user.getId());
                    notification.setType("DOCUMENT_EXPIRY");
                    notification.setTitle(title);
                    notification.setMessage(message);
                    notification.setReferenceId(document.getId());
                    notification.setReferenceType("DOCUMENT");
                    notification.setIsRead(false);
                    notification.setOrganizationId(user.getOrganizationId());

                    notificationRepository.save(notification);
                    successCount++;
                    log.info("🔔 Created in-app notification for user: {} (Role: {}, User ID: {})",
                            user.getUsername(), user.getRoles(), user.getId());
                } catch (Exception e) {
                    log.error("❌ Failed to create notification for user {} (ID: {}): {}",
                            user.getUsername(), user.getId(), e.getMessage());
                }
            }

            log.info("🔔 Created {} / {} in-app notifications for document expiry", successCount, notifyUsers.size());
        }
    }

    // Manual trigger method for testing
    public void checkDocumentExpiryManually() {
        log.info("🧪 Manual document expiry check triggered - BYPASSING frequency checks");
        performExpiryCheck(true); // Manual test - bypass frequency rules
    }
}

