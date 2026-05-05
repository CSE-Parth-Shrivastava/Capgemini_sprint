package com.finflow.application.client;

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
 * HTTP client for firing workflow notifications from application-service.
 * Calls notification-service /notifications/send for each event.
 * All failures are logged and swallowed so the main workflow is never blocked.
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
     */
    public void sendBoth(Long userId, String email, String subject,
                         String message, String eventType, Long applicationId) {
        send(userId, email, subject, message, "IN_APP", eventType, applicationId);
        send(userId, email, subject, message, "EMAIL",  eventType, applicationId);
    }

    /**
     * Sends only an IN_APP notification (when email address is not available).
     */
    public void sendInApp(Long userId, String subject,
                          String message, String eventType, Long applicationId) {
        send(userId, null, subject, message, "IN_APP", eventType, applicationId);
    }

    private void send(Long userId, String email, String subject, String message,
                      String type, String eventType, Long applicationId) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("userId",        userId);
            payload.put("subject",       subject);
            payload.put("message",       message);
            payload.put("type",          type);
            payload.put("eventType",     eventType);
            if (email         != null) payload.put("email",         email);
            if (applicationId != null) payload.put("applicationId", applicationId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            restTemplate.postForObject(
                    notificationServiceUrl + "/notifications/send",
                    new HttpEntity<>(payload, headers),
                    Object.class
            );
            log.info("[NotificationClient] {} sent | userId={} event={}", type, userId, eventType);
        } catch (Exception e) {
            log.error("[NotificationClient] Failed to send {} for event {} (userId={}): {}",
                    type, eventType, userId, e.getMessage());
        }
    }
}
