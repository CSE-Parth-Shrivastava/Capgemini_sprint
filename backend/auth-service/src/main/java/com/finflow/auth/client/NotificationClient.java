package com.finflow.auth.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * HTTP client that calls the notification-service to fire both an IN_APP
 * and an EMAIL notification for auth-lifecycle events (signup / login).
 */
@Component
public class NotificationClient {

    private static final Logger log = LoggerFactory.getLogger(NotificationClient.class);

    private final RestTemplate restTemplate;

    @Value("${services.notification.url:http://localhost:8085}")
    private String notificationServiceUrl;

    public NotificationClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Sends both an IN_APP and an EMAIL notification for the given event.
     *
     * @param userId      the user's database id
     * @param email       the user's email address
     * @param subject     email subject / in-app title
     * @param message     notification body
     * @param eventType   e.g. "SIGNUP_SUCCESS", "LOGIN_SUCCESS"
     * @param applicationId pass null when not related to a specific application
     */
    public void sendBoth(Long userId, String email, String subject,
                         String message, String eventType, Long applicationId) {
        sendNotification(userId, email, subject, message, "IN_APP",  eventType, applicationId);
        sendNotification(userId, email, subject, message, "EMAIL",   eventType, applicationId);
    }

    private void sendNotification(Long userId, String email, String subject,
                                  String message, String type,
                                  String eventType, Long applicationId) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("userId",        userId);
            payload.put("email",         email);
            payload.put("subject",       subject);
            payload.put("message",       message);
            payload.put("type",          type);
            payload.put("eventType",     eventType);
            if (applicationId != null) payload.put("applicationId", applicationId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            restTemplate.postForObject(
                    notificationServiceUrl + "/notifications/send",
                    new HttpEntity<>(payload, headers),
                    Object.class
            );
            log.info("[NotificationClient] {} notification sent: userId={} event={}", type, userId, eventType);
        } catch (Exception e) {
            log.error("[NotificationClient] Failed to send {} notification for event {}: {}",
                    type, eventType, e.getMessage());
        }
    }
}
