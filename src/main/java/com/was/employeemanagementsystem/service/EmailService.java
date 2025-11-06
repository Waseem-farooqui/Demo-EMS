package com.was.employeemanagementsystem.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.url}")
    private String appUrl;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendVerificationEmail(String toEmail, String username, String token) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Verify Your Email - Employee Management System");

            String verificationUrl = appUrl + "/verify-email?token=" + token;

            String emailBody = "Hello " + username + ",\n\n" +
                    "Thank you for registering with Employee Management System!\n\n" +
                    "Please click the link below to verify your email address:\n" +
                    verificationUrl + "\n\n" +
                    "This link will expire in 24 hours.\n\n" +
                    "If you did not create an account, please ignore this email.\n\n" +
                    "Best regards,\n" +
                    "Employee Management System Team";

            message.setText(emailBody);

            mailSender.send(message);
            log.info("✓ Verification email sent successfully to: {}", toEmail);
        } catch (Exception e) {
            log.error("✗ Failed to send verification email to: {}", toEmail, e);
            log.warn("Note: Email functionality is optional. User registration will still complete.");
            // Log but don't throw - allow registration to complete
        }
    }

    public void sendWelcomeEmail(String toEmail, String username) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Welcome to Employee Management System!");

            String emailBody = "Hello " + username + ",\n\n" +
                    "Your email has been successfully verified!\n\n" +
                    "You can now log in to your account at:\n" +
                    appUrl + "/login\n\n" +
                    "Thank you for joining us!\n\n" +
                    "Best regards,\n" +
                    "Employee Management System Team";

            message.setText(emailBody);

            mailSender.send(message);
            log.info("✓ Welcome email sent successfully to: {}", toEmail);
        } catch (Exception e) {
            log.error("✗ Failed to send welcome email to: {}", toEmail, e);
        }
    }

    public void sendDocumentExpiryAlert(String toEmail, String employeeName, String documentType,
                                       String documentNumber, String expiryDate, int daysUntilExpiry) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("URGENT: Document Expiry Alert - " + documentType);

            String emailBody = "DOCUMENT EXPIRY ALERT\n\n" +
                    "Employee: " + employeeName + "\n" +
                    "Document Type: " + documentType + "\n" +
                    "Document Number: " + (documentNumber != null ? documentNumber : "N/A") + "\n" +
                    "Expiry Date: " + expiryDate + "\n" +
                    "Days Until Expiry: " + daysUntilExpiry + " days\n\n" +
                    "ACTION REQUIRED:\n" +
                    "Please ensure the document is renewed before it expires.\n\n" +
                    "This is an automated alert from the Employee Management System.\n" +
                    "Please do not reply to this email.\n\n" +
                    "Best regards,\n" +
                    "Employee Management System";

            message.setText(emailBody);

            mailSender.send(message);
            log.info("✓ Document expiry alert sent successfully to: {} for {}", toEmail, documentType);
        } catch (Exception e) {
            log.error("✗ Failed to send expiry alert to: {}", toEmail, e);
        }
    }

    public void sendAccountCreationEmail(String toEmail, String fullName, String username, String temporaryPassword) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Your Employee Account Has Been Created");

            String emailBody = "Hello " + fullName + ",\n\n" +
                    "Your employee account has been created in the Employee Management System.\n\n" +
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
                    "Employee Management System Team";

            message.setText(emailBody);

            mailSender.send(message);
            log.info("✓ Account creation email sent successfully to: {}", toEmail);
        } catch (Exception e) {
            log.error("✗ Failed to send account creation email to: {}", toEmail, e);
            log.warn("Note: Admin should manually share credentials with the employee.");
        }
    }

    public void sendPasswordResetEmail(String toEmail, String username, String resetToken) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Reset Your Password - Employee Management System");

            String resetUrl = appUrl + "/reset-password?token=" + resetToken;

            String emailBody = "Hello " + username + ",\n\n" +
                    "We received a request to reset your password for Employee Management System.\n\n" +
                    "Please click the link below to reset your password:\n" +
                    resetUrl + "\n\n" +
                    "This link will expire in 24 hours.\n\n" +
                    "If you did not request a password reset, please ignore this email or contact support if you have concerns.\n\n" +
                    "For security reasons, this link can only be used once.\n\n" +
                    "Best regards,\n" +
                    "Employee Management System Team";

            message.setText(emailBody);

            mailSender.send(message);
            log.info("✓ Password reset email sent successfully to: {}", toEmail);
        } catch (Exception e) {
            log.error("✗ Failed to send password reset email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send password reset email. Please contact support.");
        }
    }

    public void sendOrganizationCreatedEmail(String toEmail, String fullName, String organizationName,
                                            String username, String password, boolean isGeneratedPassword) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Welcome! Your Organization Has Been Created");

            String passwordNote = isGeneratedPassword
                    ? "A temporary password has been automatically generated for you."
                    : "Your password has been set.";

            String emailBody = "Hello " + fullName + ",\n\n" +
                    "Congratulations! Your organization has been successfully created in the Employee Management System.\n\n" +
                    "ORGANIZATION DETAILS:\n" +
                    "Organization Name: " + organizationName + "\n" +
                    "Your Role: SUPER ADMINISTRATOR\n\n" +
                    "LOGIN CREDENTIALS:\n" +
                    "Username: " + username + "\n" +
                    "Password: " + password + "\n\n" +
                    "IMPORTANT - First Time Login:\n" +
                    passwordNote + "\n" +
                    "1. Login at: " + appUrl + "/login\n" +
                    "2. You will be prompted to change your password\n" +
                    "3. After password change, you can access your organization dashboard\n\n" +
                    "As a Super Administrator, you have full control over your organization:\n" +
                    "- Manage departments and employees\n" +
                    "- Configure organization settings\n" +
                    "- Create admin accounts\n" +
                    "- Access all system features\n\n" +
                    "Please keep your credentials secure and do not share them.\n\n" +
                    "If you have any questions, please contact support.\n\n" +
                    "Best regards,\n" +
                    "Employee Management System Team";

            message.setText(emailBody);

            mailSender.send(message);
            log.info("✓ Organization creation email sent successfully to: {}", toEmail);
        } catch (Exception e) {
            log.error("✗ Failed to send organization creation email to: {}", toEmail, e);
            log.warn("Note: Organization was created but email failed. Admin should manually share credentials.");
        }
    }

    /**
     * Send username reminder email
     */
    public void sendUsernameReminderEmail(String toEmail, String username) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Username Reminder - Employee Management System");

            String emailBody = "Hello,\n\n" +
                    "You requested a reminder of your username for the Employee Management System.\n\n" +
                    "Your username is: " + username + "\n\n" +
                    "You can use this username to login at: " + appUrl + "/login\n\n" +
                    "If you also forgot your password, you can reset it using the 'Forgot Password' link on the login page.\n\n" +
                    "If you did not request this reminder, please contact your system administrator.\n\n" +
                    "Best regards,\n" +
                    "Employee Management System Team";

            message.setText(emailBody);

            mailSender.send(message);
            log.info("✓ Username reminder email sent successfully to: {}", toEmail);
        } catch (Exception e) {
            log.error("✗ Failed to send username reminder email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send username reminder email. Please try again.");
        }
    }
}

