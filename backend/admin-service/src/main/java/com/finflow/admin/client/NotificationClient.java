package com.finflow.admin.client;

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
 * Fires both IN_APP and EMAIL notifications when admin makes a decision
 * (APPROVED / REJECTED) on a loan application.
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
     * Sends both an IN_APP and an EMAIL notification.
     */
    public void sendBoth(Long userId, String email, String subject,
                         String message, String eventType, Long applicationId) {
        send(userId, email, subject, message, "IN_APP", eventType, applicationId);
        send(userId, email, subject, message, "EMAIL",  eventType, applicationId);
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
            log.info("[NotificationClient] {} sent | userId={} event={} appId={}",
                    type, userId, eventType, applicationId);
        } catch (Exception e) {
            log.error("[NotificationClient] Failed to send {} | event={} appId={}: {}",
                    type, eventType, applicationId, e.getMessage());
        }
    }
}
