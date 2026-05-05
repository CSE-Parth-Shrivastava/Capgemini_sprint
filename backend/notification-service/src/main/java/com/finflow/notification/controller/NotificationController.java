package com.finflow.notification.controller;

import com.finflow.notification.dto.NotificationRequest;
import com.finflow.notification.entity.Notification;
import com.finflow.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notifications")
@Tag(name = "Notifications",
     description = "Manage in-app and email notifications. " +
                   "Notifications are created automatically by other services on key events: " +
                   "SIGNUP_SUCCESS, LOGIN_SUCCESS, APPLICATION_CREATED, APPLICATION_SUBMITTED, " +
                   "DOCUMENT_UPLOADED, DOCUMENT_VERIFIED, DOCUMENT_REJECTED, " +
                   "APPLICATION_APPROVED, APPLICATION_REJECTED. " +
                   "All notifications are persisted to the database and retrievable via these APIs.")
public class NotificationController {

    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    @Operation(
        summary = "Send a notification (internal use)",
        description = "Creates and persists a notification. If type=EMAIL and an email address is provided, " +
                      "also dispatches an email via SMTP. " +
                      "Called automatically by auth-service, application-service, admin-service, and document-service. " +
                      "Can also be triggered manually for testing via Swagger."
    )
    @PostMapping("/send")
    public ResponseEntity<Notification> send(@RequestBody NotificationRequest request) {
        return ResponseEntity.ok(service.send(request));
    }

    @Operation(
        summary = "Get all my notifications",
        description = "Returns all notifications (read and unread) for the authenticated user, " +
                      "ordered newest-first. Covers all event types."
    )
    @GetMapping("/my")
    public ResponseEntity<List<Notification>> myNotifications(
            @Parameter(description = "Injected by API Gateway from JWT", required = true)
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(service.getUserNotifications(userId));
    }

    @Operation(
        summary = "Get my unread notifications",
        description = "Returns only unread notifications for the authenticated user."
    )
    @GetMapping("/my/unread")
    public ResponseEntity<List<Notification>> unreadNotifications(
            @Parameter(description = "Injected by API Gateway from JWT", required = true)
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(service.getUnreadNotifications(userId));
    }

    @Operation(
        summary = "Get unread notification count",
        description = "Returns the count of unread notifications. Useful for showing a badge in the UI."
    )
    @GetMapping("/my/count")
    public ResponseEntity<Map<String, Long>> unreadCount(
            @Parameter(description = "Injected by API Gateway from JWT", required = true)
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(Map.of("unreadCount", service.getUnreadCount(userId)));
    }

    @Operation(
        summary = "Mark a single notification as read",
        description = "Marks the specified notification as read. Returns the updated notification."
    )
    @PutMapping("/{id}/read")
    public ResponseEntity<Notification> markAsRead(
            @Parameter(description = "Notification ID to mark as read", required = true)
            @PathVariable Long id) {
        return ResponseEntity.ok(service.markAsRead(id));
    }

    @Operation(
        summary = "Mark all my notifications as read",
        description = "Bulk-marks every unread notification for the authenticated user as read."
    )
    @PutMapping("/my/read-all")
    public ResponseEntity<Void> markAllAsRead(
            @Parameter(description = "Injected by API Gateway from JWT", required = true)
            @RequestHeader("X-User-Id") Long userId) {
        service.markAllAsRead(userId);
        return ResponseEntity.ok().build();
    }
}
