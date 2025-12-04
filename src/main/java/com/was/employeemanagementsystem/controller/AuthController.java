package com.was.employeemanagementsystem.controller;

import com.was.employeemanagementsystem.constants.AppConstants;
import com.was.employeemanagementsystem.dto.*;
import com.was.employeemanagementsystem.entity.Organization;
import com.was.employeemanagementsystem.entity.User;
import com.was.employeemanagementsystem.entity.VerificationToken;
import com.was.employeemanagementsystem.repository.OrganizationRepository;
import com.was.employeemanagementsystem.repository.UserRepository;
import com.was.employeemanagementsystem.repository.VerificationTokenRepository;
import com.was.employeemanagementsystem.security.JwtUtils;
import com.was.employeemanagementsystem.service.EmailService;
import com.was.employeemanagementsystem.service.SmtpConfigurationService;
import lombok.extern.slf4j.Slf4j;
import lombok.extern.slf4j.XSlf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping(AppConstants.API_AUTH_PATH)
@CrossOrigin(origins = "${app.cors.origins}")
@Slf4j
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final EmailService emailService;
    private final VerificationTokenRepository verificationTokenRepository;
    private final SmtpConfigurationService smtpConfigurationService;

    public AuthController(AuthenticationManager authenticationManager,
                         UserRepository userRepository,
                         OrganizationRepository organizationRepository,
                         PasswordEncoder passwordEncoder,
                         JwtUtils jwtUtils,
                         EmailService emailService,
                         VerificationTokenRepository verificationTokenRepository,
                         SmtpConfigurationService smtpConfigurationService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
        this.emailService = emailService;
        this.verificationTokenRepository = verificationTokenRepository;
        this.smtpConfigurationService = smtpConfigurationService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            // Check if user exists and is disabled BEFORE authentication
            User user = userRepository.findByUsername(loginRequest.getUsername()).orElse(null);

            if (user != null && !user.isEnabled()) {
                // User is disabled - check if it's due to organization deactivation
                if (user.getOrganizationId() != null) {
                    Organization organization = organizationRepository.findById(user.getOrganizationId()).orElse(null);

                    if (organization != null && !organization.getIsActive()) {
                        // Organization is deactivated - provide detailed message
                        log.warn("⚠️ Login attempt blocked: User '{}' from deactivated organization '{}'",
                                user.getUsername(), organization.getName());

                        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .body(new MessageResponse(
                                    "⛔ ACCESS BLOCKED\n\n" +
                                    "Your organization '" + organization.getName() + "' has been deactivated by the system administrator.\n\n" +
                                    "📧 For support and reactivation, please contact:\n" +
                                    "Waseem ud Din\n" +
                                    "Email: waseem.farooqui19@gmail.com\n\n" +
                                    "Please include your organization name and username in your support request."
                                ));
                    }
                }

                // User disabled for other reasons
                log.warn("⚠️ Login attempt blocked: User '{}' is disabled", user.getUsername());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new MessageResponse("Your account has been disabled. Please contact the administrator."));
            }

            // Proceed with authentication
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = jwtUtils.generateJwtToken(authentication);

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            Set<String> roles = userDetails.getAuthorities().stream()
                    .map(item -> item.getAuthority().replace("ROLE_", ""))
                    .collect(Collectors.toSet());

            user = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // CHECK: If user belongs to an organization
            if (user.getOrganizationId() != null && !roles.contains("ROOT")) {
                Organization organization = organizationRepository.findById(user.getOrganizationId())
                        .orElseThrow(() -> new RuntimeException("Organization not found"));

                // SUPER_ADMIN First Login: Activate organization
                if (roles.contains("SUPER_ADMIN") && !organization.getIsActive()) {
                    organization.setIsActive(true);
                    organizationRepository.save(organization);
                    log.info("✅ Organization ACTIVATED: {} (ID: {}) - SUPER_ADMIN first login",
                            organization.getName(), organization.getId());
                }

                // Check if organization is active (after potential activation)
                if (!organization.getIsActive()) {
                    // Organization is deactivated - block access
                    log.warn("⚠️ Login blocked: User '{}' from inactive organization '{}'",
                            user.getUsername(), organization.getName());

                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(new MessageResponse(
                                "⛔ ACCESS BLOCKED\n\n" +
                                "Your organization '" + organization.getName() + "' has been deactivated by the system administrator.\n\n" +
                                "📧 For support and reactivation, please contact:\n" +
                                "Waseem ud Din\n" +
                                "Email: waseem.farooqui19@gmail.com\n\n" +
                                "Please include your organization name and username in your support request."
                            ));
                }
            }

            // Check if SMTP is configured for SUPER_ADMIN
            boolean smtpConfigured = true;
            if (roles.contains("SUPER_ADMIN") && user.getOrganizationId() != null) {
                try {
                    smtpConfigured = smtpConfigurationService.isSmtpConfigured();
                } catch (Exception e) {
                    log.warn("Error checking SMTP configuration: {}", e.getMessage());
                    smtpConfigured = false;
                }
            }

            return ResponseEntity.ok(new JwtResponse(jwt,
                    user.getId(),
                    user.getUsername(),
                    user.getEmail(),
                    roles,
                    user.getOrganizationUuid(),
                    user.isFirstLogin(),
                    user.isProfileCompleted(),
                    user.isTemporaryPassword(),
                    smtpConfigured));

        } catch (org.springframework.security.authentication.DisabledException e) {
            // Catch DisabledException from Spring Security authentication
            log.error("⚠️ DisabledException during login for user: {}", loginRequest.getUsername());

            User user = userRepository.findByUsername(loginRequest.getUsername()).orElse(null);
            if (user != null && user.getOrganizationId() != null) {
                Organization organization = organizationRepository.findById(user.getOrganizationId()).orElse(null);
                if (organization != null && !organization.getIsActive()) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(new MessageResponse(
                                "⛔ ACCESS BLOCKED\n\n" +
                                "Your organization '" + organization.getName() + "' has been deactivated by the system administrator.\n\n" +
                                "📧 For support and reactivation, please contact:\n" +
                                "Waseem ud Din\n" +
                                "Email: waseem.farooqui19@gmail.com\n\n" +
                                "Please include your organization name and username in your support request."
                            ));
                }
            }

            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new MessageResponse("Your account has been disabled. Please contact the administrator."));

        } catch (org.springframework.security.authentication.BadCredentialsException e) {
            log.warn("⚠️ Failed login attempt for user: {}", loginRequest.getUsername());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new MessageResponse("Invalid username or password"));
        } catch (Exception e) {
            log.error("❌ Login error for user: {}", loginRequest.getUsername(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("An error occurred during login. Please try again."));
        }
    }

    /*
     * SIGNUP DISABLED - User creation is now managed by authenticated admins
     * Users are created through the employee management system
     */

    // @PostMapping("/signup")
    // public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
    //     ... signup code disabled ...
    // }


    @GetMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@RequestParam("token") String token) {
        VerificationToken verificationToken = verificationTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid verification token"));

        if (verificationToken.isUsed()) {
            return ResponseEntity.badRequest().body(new MessageResponse("Verification token has already been used"));
        }

        if (verificationToken.isExpired()) {
            return ResponseEntity.badRequest().body(new MessageResponse("Verification token has expired"));
        }

        User user = verificationToken.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);

        verificationToken.setUsed(true);
        verificationTokenRepository.save(verificationToken);

        // Send welcome email (don't fail verification if email fails)
        try {
            emailService.sendWelcomeEmail(user.getEmail(), user.getUsername());
        } catch (Exception e) {
            log.error("Failed to send welcome email to {} (verification still successful): {}",
                user.getEmail(), e.getMessage());
        }

        return ResponseEntity.ok(new MessageResponse("Email verified successfully! You can now log in."));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<?> resendVerification(@RequestBody ResendVerificationRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found with email: " + request.getEmail()));

        if (user.isEmailVerified()) {
            return ResponseEntity.badRequest().body(new MessageResponse("Email is already verified"));
        }

        // Delete old token if exists
        verificationTokenRepository.findByUser(user).ifPresent(verificationTokenRepository::delete);

        // Generate new verification token
        String token = UUID.randomUUID().toString();
        VerificationToken verificationToken = new VerificationToken(token, user);
        verificationTokenRepository.save(verificationToken);

        // Send verification email (handle email failures gracefully)
        try {
            emailService.sendVerificationEmail(user.getEmail(), user.getUsername(), token);
            return ResponseEntity.ok(new MessageResponse("Verification email sent! Please check your inbox."));
        } catch (Exception e) {
            log.error("Failed to send verification email to {}: {}", user.getEmail(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new MessageResponse("Token created but email sending failed. Please contact administrator with verification token: " + token));
        }
    }

    @PostMapping("/complete-profile")
    public ResponseEntity<?> completeProfile(@Valid @RequestBody CompleteProfileRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Validate passwords match
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Passwords do not match"));
        }

        // Validate password strength
        if (request.getNewPassword().length() < 8) {
            return ResponseEntity.badRequest().body(new MessageResponse("Password must be at least 8 characters long"));
        }

        // Update user password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setFirstLogin(false);
        user.setProfileCompleted(true);
        user.setTemporaryPassword(false);
        userRepository.save(user);

        return ResponseEntity.ok(new MessageResponse("Profile completed successfully! Please login with your new password."));
    }



    /**
     * Change password - for first-time login with temporary password
     * Requires: current (temporary) password, new password, confirm password
     */
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@Valid @RequestBody PasswordChangeRequest request) {
        try {
            // Get current authenticated user
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();

            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Verify current password
            if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Current password is incorrect"));
            }

            // Verify new password and confirm password match
            if (!request.getNewPassword().equals(request.getConfirmPassword())) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("New password and confirm password do not match"));
            }

            // Validate new password strength (minimum 6 characters)
            if (request.getNewPassword().length() < 6) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("New password must be at least 6 characters long"));
            }

            // Update password
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
            user.setTemporaryPassword(false);
            user.setFirstLogin(false);

            userRepository.save(user);

            return ResponseEntity.ok(new MessageResponse("Password changed successfully. Please login with your new password."));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error changing password: " + e.getMessage()));
        }
    }

    /**
     * Forgot password - sends reset link to email
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        try {
            log.info("📧 Forgot password request received for email: {}", request.getEmail());
            
            User user = userRepository.findByEmail(request.getEmail()).orElse(null);

            // Always return success message for security (don't reveal if email exists)
            if (user == null) {
                log.info("ℹ️ Email not found in database (returning generic success message for security)");
                return ResponseEntity.ok(new MessageResponse("If the email exists, a password reset link has been sent."));
            }

            log.info("✓ User found: {}", user.getEmail());

            // Invalidate any existing unused reset tokens for this user
            List<VerificationToken> existingTokens = verificationTokenRepository.findByUserAndUsedFalse(user);
            for (VerificationToken existingToken : existingTokens) {
                // Only invalidate password reset tokens (not email verification tokens)
                // Since we don't have tokenType field, we'll invalidate all unused tokens
                // This prevents multiple active reset tokens
                existingToken.setUsed(true);
                verificationTokenRepository.save(existingToken);
                log.info("✓ Invalidated existing unused token for user: {}", user.getEmail());
            }

            // Generate password reset token
            String resetToken = UUID.randomUUID().toString();
            VerificationToken verificationToken = new VerificationToken();
            verificationToken.setToken(resetToken);
            verificationToken.setUser(user);
            verificationToken.setExpiryDate(java.time.LocalDateTime.now().plusHours(24));
            verificationToken.setUsed(false);
            verificationTokenRepository.save(verificationToken);
            log.info("✓ Password reset token created for user: {} (expires in 24 hours)", user.getEmail());

            // Send password reset email (don't fail the operation if email fails)
            try {
                emailService.sendPasswordResetEmail(user.getEmail(), user.getUsername(), resetToken);
                log.info("✓ Password reset email sent successfully to: {}", user.getEmail());
            } catch (Exception emailEx) {
                log.error("❌ Failed to send password reset email to {} (token still created): {}",
                    user.getEmail(), emailEx.getMessage());
                // Token is still saved, user can contact admin with token
            }

            return ResponseEntity.ok(new MessageResponse("If the email exists, a password reset link has been sent."));

        } catch (Exception e) {
            log.error("❌ Error processing forgot password request: {}", e.getMessage(), e);
            // Always return success message for security
            return ResponseEntity.ok(new MessageResponse("If the email exists, a password reset link has been sent."));
        }
    }

    /**
     * Reset password with token
     */
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        try {
            log.info("🔄 Password reset request received for token: {}", request.getToken().substring(0, Math.min(8, request.getToken().length())) + "...");
            
            // Validate token
            VerificationToken verificationToken = verificationTokenRepository.findByToken(request.getToken())
                    .orElseThrow(() -> {
                        log.warn("❌ Invalid reset token provided");
                        return new RuntimeException("Invalid or expired reset token");
                    });

            // Check if token has already been used
            if (verificationToken.isUsed()) {
                log.warn("⚠️ Reset token has already been used: {}", verificationToken.getToken().substring(0, Math.min(8, verificationToken.getToken().length())) + "...");
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("This reset link has already been used. Please request a new password reset."));
            }

            // Check if token has expired
            if (verificationToken.getExpiryDate().isBefore(java.time.LocalDateTime.now())) {
                log.warn("⚠️ Reset token has expired: {}", verificationToken.getToken().substring(0, Math.min(8, verificationToken.getToken().length())) + "...");
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Reset token has expired. Please request a new one."));
            }

            // Validate passwords match
            if (!request.getNewPassword().equals(request.getConfirmPassword())) {
                log.warn("⚠️ Password mismatch in reset request");
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Passwords do not match"));
            }

            // Validate password strength
            if (request.getNewPassword().length() < 6) {
                log.warn("⚠️ Password too short in reset request");
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Password must be at least 6 characters long"));
            }

            // Update password
            User user = verificationToken.getUser();
            log.info("✓ Updating password for user: {}", user.getEmail());
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
            user.setTemporaryPassword(false);
            userRepository.save(user);

            // Mark token as used and delete it
            verificationToken.setUsed(true);
            verificationTokenRepository.save(verificationToken);
            verificationTokenRepository.delete(verificationToken);
            log.info("✓ Password reset completed successfully for user: {}", user.getEmail());

            return ResponseEntity.ok(new MessageResponse("Password reset successfully. You can now login with your new password."));

        } catch (RuntimeException e) {
            log.error("❌ Error in password reset: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new MessageResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("❌ Unexpected error resetting password: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error resetting password. Please try again."));
        }
    }

    /**
     * Forgot Username - Send username to user's email
     */
    @PostMapping("/forgot-username")
    public ResponseEntity<?> forgotUsername(@Valid @RequestBody ForgotUsernameRequest request) {
        try {
            log.info("🔑 Forgot username request for email: {}", request.getEmail());

            // Find user by email
            User user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new RuntimeException("No account found with this email address"));

            // Send email with username (handle email failure gracefully)
            try {
                emailService.sendUsernameReminderEmail(
                        user.getEmail(),
                        user.getUsername()
                );
                log.info("✅ Username sent to email: {}", request.getEmail());
                return ResponseEntity.ok(new MessageResponse("Your username has been sent to your email address."));
            } catch (Exception emailEx) {
                log.error("❌ Failed to send username reminder email: {}", emailEx.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Email sending failed. Your username is: " + user.getUsername()));
            }

        } catch (RuntimeException e) {
            log.error("❌ Forgot username error: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new MessageResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("❌ Error sending username", e);
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error sending username. Please try again."));
        }
    }
}

