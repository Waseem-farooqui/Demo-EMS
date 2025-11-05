package com.was.employeemanagementsystem.controller;

import com.was.employeemanagementsystem.dto.*;
import com.was.employeemanagementsystem.entity.Organization;
import com.was.employeemanagementsystem.entity.User;
import com.was.employeemanagementsystem.entity.VerificationToken;
import com.was.employeemanagementsystem.repository.OrganizationRepository;
import com.was.employeemanagementsystem.repository.UserRepository;
import com.was.employeemanagementsystem.repository.VerificationTokenRepository;
import com.was.employeemanagementsystem.security.JwtUtils;
import com.was.employeemanagementsystem.service.EmailService;
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
@RequestMapping("/api/auth")
@CrossOrigin(origins = {"http://localhost:4200", "http://127.0.0.1:4200"})
@Slf4j
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final EmailService emailService;
    private final VerificationTokenRepository verificationTokenRepository;

    public AuthController(AuthenticationManager authenticationManager,
                         UserRepository userRepository,
                         OrganizationRepository organizationRepository,
                         PasswordEncoder passwordEncoder,
                         JwtUtils jwtUtils,
                         EmailService emailService,
                         VerificationTokenRepository verificationTokenRepository) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
        this.emailService = emailService;
        this.verificationTokenRepository = verificationTokenRepository;
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

            return ResponseEntity.ok(new JwtResponse(jwt,
                    user.getId(),
                    user.getUsername(),
                    user.getEmail(),
                    roles,
                    user.getOrganizationUuid(),
                    user.isFirstLogin(),
                    user.isProfileCompleted(),
                    user.isTemporaryPassword()));

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

        // Send welcome email
        emailService.sendWelcomeEmail(user.getEmail(), user.getUsername());

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

        // Send verification email
        emailService.sendVerificationEmail(user.getEmail(), user.getUsername(), token);

        return ResponseEntity.ok(new MessageResponse("Verification email sent! Please check your inbox."));
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
            User user = userRepository.findByEmail(request.getEmail()).orElse(null);

            // Always return success message for security (don't reveal if email exists)
            if (user == null) {
                return ResponseEntity.ok(new MessageResponse("If the email exists, a password reset link has been sent."));
            }

            // Generate password reset token
            String resetToken = UUID.randomUUID().toString();
            VerificationToken verificationToken = new VerificationToken();
            verificationToken.setToken(resetToken);
            verificationToken.setUser(user);
            verificationToken.setExpiryDate(java.time.LocalDateTime.now().plusHours(24));
            verificationTokenRepository.save(verificationToken);

            // Send password reset email
            emailService.sendPasswordResetEmail(user.getEmail(), user.getUsername(), resetToken);

            return ResponseEntity.ok(new MessageResponse("If the email exists, a password reset link has been sent."));

        } catch (Exception e) {
            return ResponseEntity.ok(new MessageResponse("If the email exists, a password reset link has been sent."));
        }
    }

    /**
     * Reset password with token
     */
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        try {
            // Validate token
            VerificationToken verificationToken = verificationTokenRepository.findByToken(request.getToken())
                    .orElseThrow(() -> new RuntimeException("Invalid or expired reset token"));

            if (verificationToken.getExpiryDate().isBefore(java.time.LocalDateTime.now())) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Reset token has expired. Please request a new one."));
            }

            // Validate passwords match
            if (!request.getNewPassword().equals(request.getConfirmPassword())) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Passwords do not match"));
            }

            // Validate password strength
            if (request.getNewPassword().length() < 6) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Password must be at least 6 characters long"));
            }

            // Update password
            User user = verificationToken.getUser();
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
            user.setTemporaryPassword(false);
            userRepository.save(user);

            // Delete used token
            verificationTokenRepository.delete(verificationToken);

            return ResponseEntity.ok(new MessageResponse("Password reset successfully. You can now login with your new password."));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error resetting password. Please try again."));
        }
    }
}

