package com.finflow.notification.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finflow.notification.config.SecurityConfig;
import com.finflow.notification.dto.NotificationRequest;
import com.finflow.notification.entity.Notification;
import com.finflow.notification.entity.NotificationType;
import com.finflow.notification.exception.ResourceNotFoundException;
import com.finflow.notification.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NotificationController.class)
@Import(SecurityConfig.class)
@DisplayName("NotificationController")
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private NotificationService notificationService;

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Notification makeNotification(Long id, Long userId,
                                           NotificationType type, boolean read) {
        Notification n = new Notification();
        n.setId(id);
        n.setUserId(userId);
        n.setSubject("Test notification");
        n.setMessage("Test message");
        n.setType(type);
        n.setRead(read);
        n.setEventType("APPLICATION_CREATED");
        n.setApplicationId(100L);
        return n;
    }

    private NotificationRequest buildRequest(Long userId, String type) {
        NotificationRequest req = new NotificationRequest();
        req.setUserId(userId);
        req.setSubject("Test subject");
        req.setMessage("Test message");
        req.setType(type);
        req.setEventType("APPLICATION_CREATED");
        req.setApplicationId(100L);
        return req;
    }

    // ── POST /notifications/send ──────────────────────────────────────────────

    @Nested
    @DisplayName("POST /notifications/send")
    class SendEndpoint {

        @Test
        @DisplayName("returns 200 with saved notification on valid IN_APP request")
        void send_inApp_returns200() throws Exception {
            NotificationRequest req = buildRequest(10L, "IN_APP");
            Notification saved = makeNotification(1L, 10L, NotificationType.IN_APP, false);
            when(notificationService.send(any())).thenReturn(saved);

            mockMvc.perform(post("/notifications/send")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.type").value("IN_APP"))
                    .andExpect(jsonPath("$.read").value(false));
        }

        @Test
        @DisplayName("returns 200 with saved notification on EMAIL request")
        void send_email_returns200() throws Exception {
            NotificationRequest req = buildRequest(10L, "EMAIL");
            req.setEmail("user@test.com");
            Notification saved = makeNotification(2L, 10L, NotificationType.EMAIL, false);
            when(notificationService.send(any())).thenReturn(saved);

            mockMvc.perform(post("/notifications/send")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.type").value("EMAIL"));
        }

        @Test
        @DisplayName("is accessible without auth headers (internal endpoint)")
        void send_noAuthHeaders_returns200() throws Exception {
            // /notifications/send is permitted without auth (called by other services)
            NotificationRequest req = buildRequest(10L, "IN_APP");
            Notification saved = makeNotification(1L, 10L, NotificationType.IN_APP, false);
            when(notificationService.send(any())).thenReturn(saved);

            mockMvc.perform(post("/notifications/send")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk());
        }
    }

    // ── GET /notifications/my ─────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /notifications/my")
    class MyNotificationsEndpoint {

        @Test
        @DisplayName("returns 200 with user's notification list")
        void myNotifications_returns200WithList() throws Exception {
            List<Notification> notifications = List.of(
                    makeNotification(1L, 10L, NotificationType.IN_APP, true),
                    makeNotification(2L, 10L, NotificationType.EMAIL,  false)
            );
            when(notificationService.getUserNotifications(10L)).thenReturn(notifications);

            mockMvc.perform(get("/notifications/my")
                            .header("X-User-Id", "10")
                            .header("X-User-Role", "APPLICANT"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].userId").value(10))
                    .andExpect(jsonPath("$[1].read").value(false));
        }

        @Test
        @DisplayName("returns 200 with empty list when user has no notifications")
        void myNotifications_empty_returns200EmptyList() throws Exception {
            when(notificationService.getUserNotifications(10L)).thenReturn(List.of());

            mockMvc.perform(get("/notifications/my")
                            .header("X-User-Id", "10")
                            .header("X-User-Role", "APPLICANT"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @DisplayName("returns 403 when auth headers are missing")
        void myNotifications_noAuth_returns403() throws Exception {
            mockMvc.perform(get("/notifications/my"))
                    .andExpect(status().isForbidden());
        }
    }

    // ── GET /notifications/my/unread ──────────────────────────────────────────

    @Nested
    @DisplayName("GET /notifications/my/unread")
    class UnreadNotificationsEndpoint {

        @Test
        @DisplayName("returns 200 with only unread notifications")
        void unreadNotifications_returns200() throws Exception {
            List<Notification> unread = List.of(
                    makeNotification(1L, 10L, NotificationType.IN_APP, false)
            );
            when(notificationService.getUnreadNotifications(10L)).thenReturn(unread);

            mockMvc.perform(get("/notifications/my/unread")
                            .header("X-User-Id", "10")
                            .header("X-User-Role", "APPLICANT"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].read").value(false));
        }

        @Test
        @DisplayName("returns 403 when auth headers are missing")
        void unreadNotifications_noAuth_returns403() throws Exception {
            mockMvc.perform(get("/notifications/my/unread"))
                    .andExpect(status().isForbidden());
        }
    }

    // ── GET /notifications/my/count ───────────────────────────────────────────

    @Nested
    @DisplayName("GET /notifications/my/count")
    class UnreadCountEndpoint {

        @Test
        @DisplayName("returns 200 with correct unread count")
        void unreadCount_returns200WithCount() throws Exception {
            when(notificationService.getUnreadCount(10L)).thenReturn(3L);

            mockMvc.perform(get("/notifications/my/count")
                            .header("X-User-Id", "10")
                            .header("X-User-Role", "APPLICANT"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.unreadCount").value(3));
        }

        @Test
        @DisplayName("returns 200 with 0 when no unread notifications")
        void unreadCount_zero_returns200() throws Exception {
            when(notificationService.getUnreadCount(10L)).thenReturn(0L);

            mockMvc.perform(get("/notifications/my/count")
                            .header("X-User-Id", "10")
                            .header("X-User-Role", "APPLICANT"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.unreadCount").value(0));
        }
    }

    // ── PUT /notifications/{id}/read ──────────────────────────────────────────

    @Nested
    @DisplayName("PUT /notifications/{id}/read")
    class MarkAsReadEndpoint {

        @Test
        @DisplayName("returns 200 with updated notification marked as read")
        void markAsRead_returns200() throws Exception {
            Notification readNotification = makeNotification(1L, 10L, NotificationType.IN_APP, true);
            when(notificationService.markAsRead(1L)).thenReturn(readNotification);

            mockMvc.perform(put("/notifications/1/read")
                            .header("X-User-Id", "10")
                            .header("X-User-Role", "APPLICANT"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.read").value(true));
        }

        @Test
        @DisplayName("returns 404 when notification does not exist")
        void markAsRead_notFound_returns404() throws Exception {
            when(notificationService.markAsRead(99L))
                    .thenThrow(new ResourceNotFoundException("Notification", 99L));

            mockMvc.perform(put("/notifications/99/read")
                            .header("X-User-Id", "10")
                            .header("X-User-Role", "APPLICANT"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("returns 403 when auth headers are missing")
        void markAsRead_noAuth_returns403() throws Exception {
            mockMvc.perform(put("/notifications/1/read"))
                    .andExpect(status().isForbidden());
        }
    }

    // ── PUT /notifications/my/read-all ────────────────────────────────────────

    @Nested
    @DisplayName("PUT /notifications/my/read-all")
    class MarkAllAsReadEndpoint {

        @Test
        @DisplayName("returns 200 OK after marking all as read")
        void markAllAsRead_returns200() throws Exception {
            doNothing().when(notificationService).markAllAsRead(10L);

            mockMvc.perform(put("/notifications/my/read-all")
                            .header("X-User-Id", "10")
                            .header("X-User-Role", "APPLICANT"))
                    .andExpect(status().isOk());

            verify(notificationService).markAllAsRead(10L);
        }

        @Test
        @DisplayName("returns 403 when auth headers are missing")
        void markAllAsRead_noAuth_returns403() throws Exception {
            mockMvc.perform(put("/notifications/my/read-all"))
                    .andExpect(status().isForbidden());
        }
    }
}
