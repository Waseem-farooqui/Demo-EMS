package com.was.employeemanagementsystem.service;

import com.was.employeemanagementsystem.entity.SmtpConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

import java.util.Properties;

@Slf4j
@Service
public class DynamicEmailService {

    private final JavaMailSender defaultMailSender;
    private final SmtpConfigurationService smtpConfigService;

    @Value("${app.url}")
    private String appUrl;

    @Value("${app.email.from.name:Employee Management System}")
    private String defaultEmailFromName;

    @Value("${app.email.from.address:${spring.mail.username}}")
    private String defaultEmailFromAddress;

    private static final String EMAIL_FALLBACK_ADDRESS = "noreply@employeemanagementsystem.com";

    @Autowired
    public DynamicEmailService(JavaMailSender defaultMailSender, SmtpConfigurationService smtpConfigService) {
        this.defaultMailSender = defaultMailSender;
        this.smtpConfigService = smtpConfigService;
    }

    private String sanitizeEmail(String address, String context) {
        if (address == null || address.trim().isEmpty()) {
            log.warn("⚠ No from email configured for {}. Using fallback {}", context, EMAIL_FALLBACK_ADDRESS);
            return EMAIL_FALLBACK_ADDRESS;
        }

        String cleaned = address.trim();
        if (!cleaned.contains("@")) {
            log.warn("⚠ Invalid from email '{}' for {}. Using fallback {}", cleaned, context, EMAIL_FALLBACK_ADDRESS);
            return EMAIL_FALLBACK_ADDRESS;
        }
        return cleaned;
    }

    /**
     * Get the appropriate mail sender for the organization
     */
    private JavaMailSender getMailSender(Long organizationId) {
        if (organizationId == null) {
            return defaultMailSender;
        }

        SmtpConfiguration config = smtpConfigService.getSmtpConfigForSending(organizationId);
        
        if (config == null || config.getUseDefault() || !config.getEnabled()) {
            log.debug("Using default mail sender for organization {}", organizationId);
            return defaultMailSender;
        }

        // Create custom mail sender with configured SMTP
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(config.getHost());
        mailSender.setPort(config.getPort());
        mailSender.setUsername(config.getUsername());
        
        // Decrypt password
        String password = smtpConfigService.decryptPassword(config.getPassword());
        mailSender.setPassword(password);

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.ssl.trust", config.getHost());
        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.timeout", "5000");
        props.put("mail.smtp.writetimeout", "5000");

        log.info("Using configured SMTP for organization {}: {}:{}", organizationId, config.getHost(), config.getPort());
        return mailSender;
    }

    /**
     * Get from email address for organization
     */
    private String getFromEmail(Long organizationId) {
        String context = organizationId == null ? "default organization" : "organization " + organizationId;
        String resolved = null;
        
        if (organizationId == null) {
            resolved = defaultEmailFromAddress;
        } else {
            SmtpConfiguration config = smtpConfigService.getSmtpConfigForSending(organizationId);
            if (config != null && !config.getUseDefault() && config.getEnabled() && config.getFromEmail() != null) {
                resolved = config.getFromEmail();
            }
            if (resolved == null) {
                resolved = defaultEmailFromAddress;
            }
        }

        return sanitizeEmail(resolved, context);
    }

    /**
     * Get from name for organization
     */
    private String getFromName(Long organizationId) {
        if (organizationId == null) {
            return defaultEmailFromName;
        }

        SmtpConfiguration config = smtpConfigService.getSmtpConfigForSending(organizationId);
        if (config != null && !config.getUseDefault() && config.getEnabled() && config.getFromName() != null) {
            return config.getFromName();
        }
        return defaultEmailFromName;
    }

    /**
     * Create email message with organization-specific from address
     */
    private SimpleMailMessage createMessage(String to, String subject, Long organizationId) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(getFromEmail(organizationId));
        message.setTo(to);
        message.setSubject(subject);
        return message;
    }

    /**
     * Send email using organization-specific SMTP configuration
     */
    public void sendEmail(String toEmail, String subject, String body, Long organizationId) {
        try {
            JavaMailSender mailSender = getMailSender(organizationId);
            SimpleMailMessage message = createMessage(toEmail, subject, organizationId);
            message.setText(body);
            mailSender.send(message);
            log.info("✓ Email sent successfully to: {} from: {} (org: {})", 
                    toEmail, getFromEmail(organizationId), organizationId);
        } catch (Exception e) {
            log.error("✗ Failed to send email to: {} (org: {})", toEmail, organizationId, e);
            throw new RuntimeException("Failed to send email: " + e.getMessage(), e);
        }
    }

    // Delegate methods for backward compatibility
    public void sendVerificationEmail(String toEmail, String username, String token, Long organizationId) {
        String subject = "Verify Your Email - " + getFromName(organizationId);
        String verificationUrl = appUrl + "/verify-email?token=" + token;
        String body = "Hello " + username + ",\n\n" +
                "Thank you for registering with " + getFromName(organizationId) + "!\n\n" +
                "Please click the link below to verify your email address:\n" +
                verificationUrl + "\n\n" +
                "This link will expire in 24 hours.\n\n" +
                "If you did not create an account, please ignore this email.\n\n" +
                "Best regards,\n" +
                getFromName(organizationId) + " Team";
        sendEmail(toEmail, subject, body, organizationId);
    }

    public void sendAccountCreationEmail(String toEmail, String fullName, String username, 
                                        String temporaryPassword, Long organizationId) {
        String subject = "Your Employee Account Has Been Created";
        String body = "Hello " + fullName + ",\n\n" +
                "Your employee account has been created in the " + getFromName(organizationId) + ".\n\n" +
                "LOGIN CREDENTIALS:\n" +
                "Username: " + username + "\n" +
                "Temporary Password: " + temporaryPassword + "\n\n" +
                "IMPORTANT - First Time Login:\n" +
                "1. Login at: " + appUrl + "/login\n" +
                "2. You will be prompted to complete your profile\n" +
                "3. You MUST change your temporary password\n" +
                "4. Fill in all required employee details\n\n" +
                "Your temporary password will expire after first login.\n" +
                "Please keep your new password secure and do not share it.\n\n" +
                "If you did not expect this account, please contact your administrator.\n\n" +
                "Best regards,\n" +
                getFromName(organizationId) + " Team";
        sendEmail(toEmail, subject, body, organizationId);
    }

    public void sendDocumentExpiryAlert(String toEmail, String employeeName, String documentType,
                                       String documentNumber, String expiryDate, int daysUntilExpiry, Long organizationId) {
        String subject = "URGENT: Document Expiry Alert - " + documentType;
        String body = "DOCUMENT EXPIRY ALERT\n\n" +
                "Employee: " + employeeName + "\n" +
                "Document Type: " + documentType + "\n" +
                "Document Number: " + (documentNumber != null ? documentNumber : "N/A") + "\n" +
                "Expiry Date: " + expiryDate + "\n" +
                "Days Until Expiry: " + daysUntilExpiry + " days\n\n" +
                "ACTION REQUIRED:\n" +
                "Please ensure the document is renewed before it expires.\n\n" +
                "This is an automated alert from the " + getFromName(organizationId) + ".\n" +
                "Please do not reply to this email.\n\n" +
                "Best regards,\n" +
                getFromName(organizationId);
        sendEmail(toEmail, subject, body, organizationId);
    }

    // Helper methods for getting from email/name (made public for EmailService)
    public String getFromEmailForService(Long organizationId) {
        return getFromEmail(organizationId);
    }

    public String getFromNameForService(Long organizationId) {
        return getFromName(organizationId);
    }
}

