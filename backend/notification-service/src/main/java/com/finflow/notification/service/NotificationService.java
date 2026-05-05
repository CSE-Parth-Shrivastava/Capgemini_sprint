package com.finflow.notification.service;

import com.finflow.notification.dto.NotificationRequest;
import com.finflow.notification.entity.Notification;
import com.finflow.notification.entity.NotificationType;
import com.finflow.notification.exception.ResourceNotFoundException;
import com.finflow.notification.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository repository;
    private final JavaMailSender mailSender;

    public NotificationService(NotificationRepository repository, JavaMailSender mailSender) {
        this.repository = repository;
        this.mailSender = mailSender;
    }

    public Notification send(NotificationRequest request) {
        Notification notification = new Notification();
        notification.setUserId(request.getUserId());
        notification.setEmail(request.getEmail());
        notification.setSubject(request.getSubject());
        notification.setMessage(request.getMessage());
        notification.setApplicationId(request.getApplicationId());
        notification.setEventType(request.getEventType());

        NotificationType type = request.getType() != null
                ? NotificationType.valueOf(request.getType().toUpperCase())
                : NotificationType.IN_APP;
        notification.setType(type);

        notification = repository.save(notification);

        if (type == NotificationType.EMAIL && request.getEmail() != null) {
            try {
                SimpleMailMessage mail = new SimpleMailMessage();
                mail.setTo(request.getEmail());
                mail.setSubject(request.getSubject());
                mail.setText(request.getMessage());
                mailSender.send(mail);
                notification.setEmailSent(true);
                notification.setSentAt(LocalDateTime.now());
                notification = repository.save(notification);
            } catch (Exception e) {
                log.error("Failed to send email to {}: {}", request.getEmail(), e.getMessage());
            }
        }

        return notification;
    }

    public List<Notification> getUserNotifications(Long userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<Notification> getUnreadNotifications(Long userId) {
        return repository.findByUserIdAndReadFalse(userId);
    }

    public long getUnreadCount(Long userId) {
        return repository.countByUserIdAndReadFalse(userId);
    }

    public Notification markAsRead(Long notificationId) {
        Notification notification = repository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", notificationId));
        notification.setRead(true);
        return repository.save(notification);
    }

    public void markAllAsRead(Long userId) {
        List<Notification> unread = repository.findByUserIdAndReadFalse(userId);
        unread.forEach(n -> n.setRead(true));
        repository.saveAll(unread);
    }
}