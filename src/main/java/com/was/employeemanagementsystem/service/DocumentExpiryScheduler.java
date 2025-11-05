package com.was.employeemanagementsystem.service;

import com.was.employeemanagementsystem.entity.AlertConfiguration;
import com.was.employeemanagementsystem.entity.Document;
import com.was.employeemanagementsystem.repository.AlertConfigurationRepository;
import com.was.employeemanagementsystem.repository.DocumentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
public class DocumentExpiryScheduler {

    private final DocumentRepository documentRepository;
    private final AlertConfigurationRepository alertConfigurationRepository;
    private final EmailService emailService;

    public DocumentExpiryScheduler(DocumentRepository documentRepository,
                                  AlertConfigurationRepository alertConfigurationRepository,
                                  EmailService emailService) {
        this.documentRepository = documentRepository;
        this.alertConfigurationRepository = alertConfigurationRepository;
        this.emailService = emailService;
    }

    // Run every day at 9:00 AM
    @Scheduled(cron = "0 0 9 * * ?")
    public void checkDocumentExpiry() {
        log.info("Running document expiry check at: {}", LocalDateTime.now());

        List<AlertConfiguration> configurations = alertConfigurationRepository.findAll();

        for (AlertConfiguration config : configurations) {
            if (!config.isEnabled()) {
                continue;
            }

            LocalDate currentDate = LocalDate.now();
            LocalDate alertDate = currentDate.plusDays(config.getAlertDaysBefore());

            List<Document> documents = documentRepository.findByDocumentType(config.getDocumentType());

            for (Document document : documents) {
                if (document.getExpiryDate() == null) {
                    continue;
                }

                long daysUntilExpiry = ChronoUnit.DAYS.between(currentDate, document.getExpiryDate());

                // Check if document is expiring within the alert period
                if (daysUntilExpiry > 0 && daysUntilExpiry <= config.getAlertDaysBefore()) {
                    // Check if alert was already sent recently (don't spam)
                    boolean shouldSendAlert = document.getLastAlertSent() == null ||
                            ChronoUnit.DAYS.between(document.getLastAlertSent().toLocalDate(), currentDate) >= 7;

                    if (shouldSendAlert) {
                        sendExpiryAlert(document, config, (int) daysUntilExpiry);

                        // Update alert tracking
                        document.setLastAlertSent(LocalDateTime.now());
                        document.setAlertSentCount(
                            document.getAlertSentCount() != null ? document.getAlertSentCount() + 1 : 1
                        );
                        documentRepository.save(document);
                    }
                }
            }
        }
    }

    private void sendExpiryAlert(Document document, AlertConfiguration config, int daysUntilExpiry) {
        String employeeName = document.getEmployee().getFullName();
        String documentType = document.getDocumentType();
        String documentNumber = document.getDocumentNumber() != null ? document.getDocumentNumber() : "N/A";
        String expiryDate = document.getExpiryDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        emailService.sendDocumentExpiryAlert(
            config.getAlertEmail(),
            employeeName,
            documentType,
            documentNumber,
            expiryDate,
            daysUntilExpiry
        );

        log.info("Sent expiry alert for {} - Employee: {}, Days until expiry: {}",
            documentType, employeeName, daysUntilExpiry);
    }

    // Manual trigger method for testing
    public void checkDocumentExpiryManually() {
        checkDocumentExpiry();
    }
}

