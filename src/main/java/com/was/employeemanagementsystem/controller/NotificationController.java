package com.was.employeemanagementsystem.controller;

import com.was.employeemanagementsystem.constants.AppConstants;
import com.was.employeemanagementsystem.dto.NotificationDTO;
import com.was.employeemanagementsystem.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping(AppConstants.API_NOTIFICATIONS_PATH)
@CrossOrigin(origins = "${app.cors.origins}")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * Get all notifications for current user
     */
    @GetMapping
    public ResponseEntity<List<NotificationDTO>> getMyNotifications() {
        try {
            List<NotificationDTO> notifications = notificationService.getMyNotifications();
            return ResponseEntity.ok(notifications);
        } catch (RuntimeException e) {
            log.error("Error fetching notifications: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get unread notifications
     */
    @GetMapping("/unread")
    public ResponseEntity<List<NotificationDTO>> getUnreadNotifications() {
        try {
            List<NotificationDTO> notifications = notificationService.getUnreadNotifications();
            return ResponseEntity.ok(notifications);
        } catch (RuntimeException e) {
            log.error("Error fetching unread notifications: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get unread notification count
     */
    @GetMapping("/unread/count")
    public ResponseEntity<Map<String, Long>> getUnreadCount() {
        try {
            Long count = notificationService.getUnreadCount();
            Map<String, Long> response = new HashMap<>();
            response.put("count", count);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching unread count: {}", e.getMessage());
            Map<String, Long> response = new HashMap<>();
            response.put("count", 0L);
            return ResponseEntity.ok(response);
        }
    }

    /**
     * Get recent notifications (last 10)
     */
    @GetMapping("/recent")
    public ResponseEntity<List<NotificationDTO>> getRecentNotifications() {
        try {
            List<NotificationDTO> notifications = notificationService.getRecentNotifications();
            return ResponseEntity.ok(notifications);
        } catch (RuntimeException e) {
            log.error("Error fetching recent notifications: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Mark notification as read
     */
    @PutMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(@PathVariable Long id) {
        try {
            NotificationDTO notification = notificationService.markAsRead(id);
            return ResponseEntity.ok(notification);
        } catch (RuntimeException e) {
            log.error("Error marking notification as read: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Mark all notifications as read
     */
    @PutMapping("/read-all")
    public ResponseEntity<?> markAllAsRead() {
        try {
            notificationService.markAllAsRead();
            Map<String, String> response = new HashMap<>();
            response.put("message", "All notifications marked as read");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Error marking all notifications as read: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Delete notification
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteNotification(@PathVariable Long id) {
        try {
            notificationService.deleteNotification(id);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Notification deleted successfully");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Error deleting notification: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}

