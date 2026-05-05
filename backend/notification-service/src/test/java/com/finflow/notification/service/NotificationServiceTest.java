package com.finflow.notification.service;

import com.finflow.notification.dto.NotificationRequest;
import com.finflow.notification.entity.Notification;
import com.finflow.notification.entity.NotificationType;
import com.finflow.notification.exception.ResourceNotFoundException;
import com.finflow.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationService Unit Tests")
class NotificationServiceTest {

    @Mock private NotificationRepository repository;
    @Mock private JavaMailSender mailSender;

    @InjectMocks
    private NotificationService service;

    private NotificationRequest inAppRequest;
    private NotificationRequest emailRequest;
    private Notification savedNotification;

    @BeforeEach
    void setUp() {
        inAppRequest = new NotificationRequest();
        inAppRequest.setUserId(1L);
        inAppRequest.setEmail("user@example.com");
        inAppRequest.setSubject("Test Subject");
        inAppRequest.setMessage("Test Message");
        inAppRequest.setEventType("APPLICATION_CREATED");
        inAppRequest.setType("IN_APP");

        emailRequest = new NotificationRequest();
        emailRequest.setUserId(1L);
        emailRequest.setEmail("user@example.com");
        emailRequest.setSubject("Email Subject");
        emailRequest.setMessage("Email Body");
        emailRequest.setEventType("APPLICATION_APPROVED");
        emailRequest.setType("EMAIL");

        savedNotification = new Notification();
        savedNotification.setId(1L);
        savedNotification.setUserId(1L);
        savedNotification.setRead(false);
    }

    // ── send ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("send: IN_APP notification is persisted and not emailed")
    void send_inApp_persistsAndDoesNotSendEmail() {
        when(repository.save(any(Notification.class))).thenReturn(savedNotification);

        service.send(inAppRequest);

        verify(repository).save(any(Notification.class));
        verifyNoInteractions(mailSender);
    }

    @Test
    @DisplayName("send: EMAIL notification is persisted and email is sent")
    void send_email_persistsAndSendsEmail() {
        Notification emailNotification = new Notification();
        emailNotification.setId(2L);
        emailNotification.setType(NotificationType.EMAIL);
        when(repository.save(any(Notification.class))).thenReturn(emailNotification);

        service.send(emailRequest);

        verify(mailSender).send(any(SimpleMailMessage.class));
        // save called twice: once to persist, once to mark emailSent=true
        verify(repository, times(2)).save(any(Notification.class));
    }

    @Test
    @DisplayName("send: email failure does not throw — notification still persisted")
    void send_emailFailure_doesNotPropagate() {
        when(repository.save(any(Notification.class))).thenReturn(savedNotification);
        doThrow(new RuntimeException("SMTP down")).when(mailSender).send(any(SimpleMailMessage.class));

        assertDoesNotThrow(() -> service.send(emailRequest));
    }

    @Test
    @DisplayName("send: null type defaults to IN_APP")
    void send_nullType_defaultsToInApp() {
        inAppRequest.setType(null);
        when(repository.save(any())).thenReturn(savedNotification);

        service.send(inAppRequest);

        verify(repository).save(argThat(n -> n.getType() == NotificationType.IN_APP));
    }

    // ── getUserNotifications ──────────────────────────────────────────────────

    @Test
    @DisplayName("getUserNotifications: returns ordered list for user")
    void getUserNotifications_returnsListForUser() {
        when(repository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(savedNotification));

        List<Notification> result = service.getUserNotifications(1L);

        assertThat(result).hasSize(1);
    }

    // ── getUnreadNotifications ────────────────────────────────────────────────

    @Test
    @DisplayName("getUnreadNotifications: returns only unread notifications")
    void getUnreadNotifications_returnsUnreadOnly() {
        when(repository.findByUserIdAndReadFalse(1L)).thenReturn(List.of(savedNotification));

        List<Notification> result = service.getUnreadNotifications(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).isRead()).isFalse();
    }

    // ── getUnreadCount ────────────────────────────────────────────────────────

    @Test
    @DisplayName("getUnreadCount: returns correct unread count")
    void getUnreadCount_returnsCount() {
        when(repository.countByUserIdAndReadFalse(1L)).thenReturn(5L);

        long count = service.getUnreadCount(1L);

        assertThat(count).isEqualTo(5L);
    }

    // ── markAsRead ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("markAsRead: marks notification as read")
    void markAsRead_setsReadTrue() {
        when(repository.findById(1L)).thenReturn(Optional.of(savedNotification));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Notification result = service.markAsRead(1L);

        assertThat(result.isRead()).isTrue();
    }

    @Test
    @DisplayName("markAsRead: not found throws ResourceNotFoundException")
    void markAsRead_notFound_throwsException() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.markAsRead(99L));
    }

    // ── markAllAsRead ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("markAllAsRead: marks all unread notifications for user")
    void markAllAsRead_marksAllUnread() {
        Notification n1 = new Notification();
        n1.setRead(false);
        Notification n2 = new Notification();
        n2.setRead(false);
        when(repository.findByUserIdAndReadFalse(1L)).thenReturn(List.of(n1, n2));

        service.markAllAsRead(1L);

        verify(repository).saveAll(argThat(notifications ->
                ((List<Notification>) notifications).stream().allMatch(Notification::isRead)));
    }

    @Test
    @DisplayName("markAllAsRead: does nothing when no unread notifications exist")
    void markAllAsRead_noUnread_savesEmptyList() {
        when(repository.findByUserIdAndReadFalse(1L)).thenReturn(List.of());

        service.markAllAsRead(1L);

        verify(repository).saveAll(argThat(list -> ((List<?>) list).isEmpty()));
    }
}
