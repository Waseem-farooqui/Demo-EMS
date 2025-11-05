package com.was.employeemanagementsystem.service;

import com.was.employeemanagementsystem.dto.NotificationDTO;
import com.was.employeemanagementsystem.entity.Employee;
import com.was.employeemanagementsystem.entity.Leave;
import com.was.employeemanagementsystem.entity.Notification;
import com.was.employeemanagementsystem.entity.User;
import com.was.employeemanagementsystem.repository.EmployeeRepository;
import com.was.employeemanagementsystem.repository.NotificationRepository;
import com.was.employeemanagementsystem.repository.UserRepository;
import com.was.employeemanagementsystem.security.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final SecurityUtils securityUtils;

    public NotificationService(NotificationRepository notificationRepository,
                              UserRepository userRepository,
                              EmployeeRepository employeeRepository,
                              SecurityUtils securityUtils) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.employeeRepository = employeeRepository;
        this.securityUtils = securityUtils;
    }

    /**
     * Create leave request notification for appropriate approvers
     */
    public void createLeaveRequestNotification(Leave leave) {
        log.info("🔔 Creating leave request notification for leave ID: {}", leave.getId());

        Employee requestingEmployee = leave.getEmployee();
        if (requestingEmployee.getUserId() == null) {
            log.warn("Employee {} has no user account, skipping notification", requestingEmployee.getId());
            return;
        }

        User requestingUser = userRepository.findById(requestingEmployee.getUserId())
                .orElse(null);

        if (requestingUser == null) {
            log.warn("User not found for employee {}", requestingEmployee.getId());
            return;
        }

        List<Long> notifyUserIds = new ArrayList<>();

        // Determine who should be notified based on requester's role
        if (requestingUser.getRoles().contains("ADMIN")) {
            // ADMIN leave request → notify all SUPER_ADMINs in the organization
            log.info("ADMIN leave request - notifying SUPER_ADMINs");
            List<User> superAdmins = userRepository.findAll().stream()
                    .filter(u -> u.getRoles().contains("SUPER_ADMIN"))
                    .filter(u -> requestingUser.getOrganizationId().equals(u.getOrganizationId()))
                    .collect(Collectors.toList());

            notifyUserIds.addAll(superAdmins.stream()
                    .map(User::getId)
                    .collect(Collectors.toList()));

        } else if (requestingUser.getRoles().contains("USER")) {
            // USER leave request → notify department ADMIN
            log.info("USER leave request - notifying department ADMIN");
            if (requestingEmployee.getDepartment() != null) {
                // Find ADMINs in the same department
                List<Employee> deptEmployees = employeeRepository.findByDepartmentId(
                        requestingEmployee.getDepartment().getId());

                for (Employee emp : deptEmployees) {
                    if (emp.getUserId() != null) {
                        User user = userRepository.findById(emp.getUserId()).orElse(null);
                        if (user != null && user.getRoles().contains("ADMIN") &&
                            !user.getRoles().contains("SUPER_ADMIN")) {
                            notifyUserIds.add(user.getId());
                        }
                    }
                }

                // Also notify SUPER_ADMINs in the organization
                List<User> superAdmins = userRepository.findAll().stream()
                        .filter(u -> u.getRoles().contains("SUPER_ADMIN"))
                        .filter(u -> requestingUser.getOrganizationId().equals(u.getOrganizationId()))
                        .collect(Collectors.toList());

                notifyUserIds.addAll(superAdmins.stream()
                        .map(User::getId)
                        .collect(Collectors.toList()));
            }
        }

        // Remove duplicates
        notifyUserIds = notifyUserIds.stream().distinct().collect(Collectors.toList());

        // Create notifications
        for (Long userId : notifyUserIds) {
            Notification notification = new Notification();
            notification.setUserId(userId);
            notification.setType("LEAVE_REQUEST");
            notification.setTitle("New Leave Request");
            notification.setMessage(String.format("%s has requested %s leave from %s to %s",
                    requestingEmployee.getFullName(),
                    leave.getLeaveType(),
                    leave.getStartDate(),
                    leave.getEndDate()));
            notification.setReferenceId(leave.getId());
            notification.setReferenceType("LEAVE");
            notification.setIsRead(false);
            notification.setOrganizationId(requestingUser.getOrganizationId());

            notificationRepository.save(notification);
            log.info("✅ Notification created for user ID: {}", userId);
        }

        log.info("🔔 Total {} notifications created for leave request", notifyUserIds.size());
    }

    /**
     * Create leave approval notification
     */
    public void createLeaveApprovalNotification(Leave leave, String approverName) {
        log.info("🔔 Creating leave approval notification for leave ID: {}", leave.getId());

        Employee employee = leave.getEmployee();
        if (employee.getUserId() != null) {
            Notification notification = new Notification();
            notification.setUserId(employee.getUserId());
            notification.setType("LEAVE_APPROVED");
            notification.setTitle("Leave Request Approved");
            notification.setMessage(String.format("Your %s leave request from %s to %s has been approved by %s",
                    leave.getLeaveType(),
                    leave.getStartDate(),
                    leave.getEndDate(),
                    approverName));
            notification.setReferenceId(leave.getId());
            notification.setReferenceType("LEAVE");
            notification.setIsRead(false);

            User user = userRepository.findById(employee.getUserId()).orElse(null);
            if (user != null) {
                notification.setOrganizationId(user.getOrganizationId());
            }

            notificationRepository.save(notification);
            log.info("✅ Leave approval notification created for user ID: {}", employee.getUserId());
        }
    }

    /**
     * Create leave rejection notification
     */
    public void createLeaveRejectionNotification(Leave leave, String rejectorName, String remarks) {
        log.info("🔔 Creating leave rejection notification for leave ID: {}", leave.getId());

        Employee employee = leave.getEmployee();
        if (employee.getUserId() != null) {
            Notification notification = new Notification();
            notification.setUserId(employee.getUserId());
            notification.setType("LEAVE_REJECTED");
            notification.setTitle("Leave Request Rejected");
            notification.setMessage(String.format("Your %s leave request from %s to %s has been rejected by %s. Reason: %s",
                    leave.getLeaveType(),
                    leave.getStartDate(),
                    leave.getEndDate(),
                    rejectorName,
                    remarks != null ? remarks : "Not specified"));
            notification.setReferenceId(leave.getId());
            notification.setReferenceType("LEAVE");
            notification.setIsRead(false);

            User user = userRepository.findById(employee.getUserId()).orElse(null);
            if (user != null) {
                notification.setOrganizationId(user.getOrganizationId());
            }

            notificationRepository.save(notification);
            log.info("✅ Leave rejection notification created for user ID: {}", employee.getUserId());
        }
    }

    /**
     * Get all notifications for current user
     */
    public List<NotificationDTO> getMyNotifications() {
        User currentUser = securityUtils.getCurrentUser();
        if (currentUser == null) {
            throw new RuntimeException("User not authenticated");
        }

        return notificationRepository.findByUserIdOrderByCreatedAtDesc(currentUser.getId())
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get unread notifications for current user
     */
    public List<NotificationDTO> getUnreadNotifications() {
        User currentUser = securityUtils.getCurrentUser();
        if (currentUser == null) {
            throw new RuntimeException("User not authenticated");
        }

        return notificationRepository.findByUserIdAndIsReadOrderByCreatedAtDesc(currentUser.getId(), false)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get unread notification count
     */
    public Long getUnreadCount() {
        User currentUser = securityUtils.getCurrentUser();
        if (currentUser == null) {
            return 0L;
        }

        return notificationRepository.countByUserIdAndIsRead(currentUser.getId(), false);
    }

    /**
     * Mark notification as read
     */
    public NotificationDTO markAsRead(Long notificationId) {
        User currentUser = securityUtils.getCurrentUser();
        if (currentUser == null) {
            throw new RuntimeException("User not authenticated");
        }

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        // Verify ownership
        if (!notification.getUserId().equals(currentUser.getId())) {
            throw new RuntimeException("Access denied. This notification doesn't belong to you.");
        }

        notification.setIsRead(true);
        notification.setReadAt(LocalDateTime.now());

        Notification updated = notificationRepository.save(notification);
        log.info("✅ Notification {} marked as read", notificationId);

        return convertToDTO(updated);
    }

    /**
     * Mark all notifications as read for current user
     */
    public void markAllAsRead() {
        User currentUser = securityUtils.getCurrentUser();
        if (currentUser == null) {
            throw new RuntimeException("User not authenticated");
        }

        List<Notification> unreadNotifications = notificationRepository
                .findByUserIdAndIsReadOrderByCreatedAtDesc(currentUser.getId(), false);

        for (Notification notification : unreadNotifications) {
            notification.setIsRead(true);
            notification.setReadAt(LocalDateTime.now());
            notificationRepository.save(notification);
        }

        log.info("✅ Marked {} notifications as read for user {}",
                unreadNotifications.size(), currentUser.getUsername());
    }

    /**
     * Delete notification
     */
    public void deleteNotification(Long notificationId) {
        User currentUser = securityUtils.getCurrentUser();
        if (currentUser == null) {
            throw new RuntimeException("User not authenticated");
        }

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        // Verify ownership
        if (!notification.getUserId().equals(currentUser.getId())) {
            throw new RuntimeException("Access denied. This notification doesn't belong to you.");
        }

        notificationRepository.deleteById(notificationId);
        log.info("✅ Notification {} deleted", notificationId);
    }

    /**
     * Get recent notifications (last 10)
     */
    public List<NotificationDTO> getRecentNotifications() {
        User currentUser = securityUtils.getCurrentUser();
        if (currentUser == null) {
            throw new RuntimeException("User not authenticated");
        }

        return notificationRepository.findTop10ByUserIdOrderByCreatedAtDesc(currentUser.getId())
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private NotificationDTO convertToDTO(Notification notification) {
        NotificationDTO dto = new NotificationDTO();
        dto.setId(notification.getId());
        dto.setUserId(notification.getUserId());
        dto.setType(notification.getType());
        dto.setTitle(notification.getTitle());
        dto.setMessage(notification.getMessage());
        dto.setReferenceId(notification.getReferenceId());
        dto.setReferenceType(notification.getReferenceType());
        dto.setIsRead(notification.getIsRead());
        dto.setCreatedAt(notification.getCreatedAt());
        dto.setReadAt(notification.getReadAt());
        dto.setOrganizationId(notification.getOrganizationId());
        return dto;
    }
}

