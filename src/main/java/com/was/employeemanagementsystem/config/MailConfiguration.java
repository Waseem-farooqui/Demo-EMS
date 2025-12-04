package com.was.employeemanagementsystem.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import javax.annotation.PostConstruct;
import java.util.Properties;

/**
 * Mail Configuration
 * Conditionally configures SMTP socket factory for port 465 (SSL)
 * For port 587 (STARTTLS), socket factory should NOT be set to avoid SSL errors
 */
@Slf4j
@Configuration
public class MailConfiguration {

    @Autowired(required = false)
    private JavaMailSenderImpl mailSender;

    @Value("${spring.mail.port:587}")
    private int mailPort;

    @Value("${spring.mail.properties.mail.smtp.ssl.enable:false}")
    private String sslEnable;

    @Value("${MAIL_SMTP_SSL_SOCKET_FACTORY_PORT:}")
    private String socketFactoryPort;

    @PostConstruct
    public void configureMailProperties() {
        if (mailSender == null) {
            log.warn("JavaMailSenderImpl not found, skipping mail configuration");
            return;
        }

        Properties props = mailSender.getJavaMailProperties();
        
        // Only set socket factory for port 465 (SSL)
        if (mailPort == 465 && "true".equalsIgnoreCase(sslEnable)) {
            // Set socket factory for port 465
            if (socketFactoryPort != null && !socketFactoryPort.isEmpty()) {
                props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
                props.put("mail.smtp.socketFactory.port", socketFactoryPort);
                props.put("mail.smtp.socketFactory.fallback", "false");
                log.info("✓ Socket factory configured for port 465: {}", socketFactoryPort);
            } else {
                // Use default SSL socket factory for port 465
                props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
                props.put("mail.smtp.socketFactory.port", "465");
                props.put("mail.smtp.socketFactory.fallback", "false");
                log.info("✓ Socket factory configured for port 465 (default)");
            }
        } else if (mailPort == 587) {
            // For port 587 (STARTTLS), explicitly remove socket factory to avoid SSL errors
            props.remove("mail.smtp.socketFactory.class");
            props.remove("mail.smtp.socketFactory.port");
            props.remove("mail.smtp.socketFactory.fallback");
            log.info("✓ Socket factory removed for port 587 (STARTTLS) - this prevents SSL errors");
        }
    }
}

