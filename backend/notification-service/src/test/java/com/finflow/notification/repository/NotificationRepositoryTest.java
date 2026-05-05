package com.finflow.notification.repository;

import com.finflow.notification.entity.Notification;
import com.finflow.notification.entity.NotificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:notiftest;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false"
})
@DisplayName("NotificationRepository")
class NotificationRepositoryTest {

    @Autowired
    private NotificationRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Notification persist(Long userId, NotificationType type,
                                  boolean read, String eventType) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setSubject("Subject for " + eventType);
        n.setMessage("Message for " + eventType);
        n.setType(type);
        n.setRead(read);
        n.setEventType(eventType);
        n.setApplicationId(100L);
        return repository.save(n);
    }

    // ── findByUserIdOrderByCreatedAtDesc() ────────────────────────────────────

    @Nested
    @DisplayName("findByUserIdOrderByCreatedAtDesc()")
    class FindByUserIdOrderByCreatedAtDesc {

        @Test
        @DisplayName("returns all notifications for given user")
        void returnsAllNotificationsForUser() {
            persist(10L, NotificationType.IN_APP, false, "APP_CREATED");
            persist(10L, NotificationType.EMAIL,  true,  "APP_SUBMITTED");
            persist(11L, NotificationType.IN_APP, false, "APP_CREATED"); // different user

            List<Notification> result = repository.findByUserIdOrderByCreatedAtDesc(10L);

            assertThat(result).hasSize(2);
            assertThat(result).extracting(Notification::getUserId).containsOnly(10L);
        }

        @Test
        @DisplayName("returns empty list for user with no notifications")
        void unknownUser_returnsEmpty() {
            persist(10L, NotificationType.IN_APP, false, "APP_CREATED");

            assertThat(repository.findByUserIdOrderByCreatedAtDesc(999L)).isEmpty();
        }

        @Test
        @DisplayName("includes both read and unread notifications")
        void includesBothReadAndUnread() {
            persist(10L, NotificationType.IN_APP, false, "APP_CREATED");
            persist(10L, NotificationType.IN_APP, true,  "APP_SUBMITTED");

            List<Notification> result = repository.findByUserIdOrderByCreatedAtDesc(10L);

            assertThat(result).hasSize(2);
            assertThat(result).extracting(Notification::isRead)
                    .containsExactlyInAnyOrder(true, false);
        }
    }

    // ── findByUserIdAndReadFalse() ────────────────────────────────────────────

    @Nested
    @DisplayName("findByUserIdAndReadFalse()")
    class FindByUserIdAndReadFalse {

        @Test
        @DisplayName("returns only unread notifications for user")
        void returnsOnlyUnreadForUser() {
            persist(10L, NotificationType.IN_APP, false, "APP_CREATED");   // unread
            persist(10L, NotificationType.EMAIL,  true,  "APP_SUBMITTED"); // read — excluded
            persist(10L, NotificationType.IN_APP, false, "DOC_UPLOADED");  // unread

            List<Notification> result = repository.findByUserIdAndReadFalse(10L);

            assertThat(result).hasSize(2);
            assertThat(result).extracting(Notification::isRead).containsOnly(false);
        }

        @Test
        @DisplayName("returns empty list when all notifications are read")
        void allRead_returnsEmpty() {
            persist(10L, NotificationType.IN_APP, true, "APP_CREATED");
            persist(10L, NotificationType.EMAIL,  true, "APP_SUBMITTED");

            assertThat(repository.findByUserIdAndReadFalse(10L)).isEmpty();
        }

        @Test
        @DisplayName("does not return notifications belonging to other users")
        void doesNotLeakOtherUserNotifications() {
            persist(10L, NotificationType.IN_APP, false, "APP_CREATED");
            persist(11L, NotificationType.IN_APP, false, "APP_CREATED"); // other user

            List<Notification> result = repository.findByUserIdAndReadFalse(10L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getUserId()).isEqualTo(10L);
        }
    }

    // ── countByUserIdAndReadFalse() ───────────────────────────────────────────

    @Nested
    @DisplayName("countByUserIdAndReadFalse()")
    class CountByUserIdAndReadFalse {

        @Test
        @DisplayName("returns exact count of unread notifications for user")
        void returnsCorrectUnreadCount() {
            persist(10L, NotificationType.IN_APP, false, "APP_CREATED");
            persist(10L, NotificationType.IN_APP, false, "DOC_UPLOADED");
            persist(10L, NotificationType.EMAIL,  true,  "APP_SUBMITTED");

            assertThat(repository.countByUserIdAndReadFalse(10L)).isEqualTo(2L);
        }

        @Test
        @DisplayName("returns 0 when user has no unread notifications")
        void allRead_returnsZero() {
            persist(10L, NotificationType.IN_APP, true, "APP_CREATED");

            assertThat(repository.countByUserIdAndReadFalse(10L)).isZero();
        }

        @Test
        @DisplayName("returns 0 for user with no notifications")
        void unknownUser_returnsZero() {
            assertThat(repository.countByUserIdAndReadFalse(999L)).isZero();
        }

        @Test
        @DisplayName("count is isolated per user")
        void countIsIsolatedPerUser() {
            persist(10L, NotificationType.IN_APP, false, "APP_CREATED");
            persist(10L, NotificationType.IN_APP, false, "DOC_UPLOADED");
            persist(11L, NotificationType.IN_APP, false, "APP_CREATED");

            assertThat(repository.countByUserIdAndReadFalse(10L)).isEqualTo(2L);
            assertThat(repository.countByUserIdAndReadFalse(11L)).isEqualTo(1L);
        }
    }

    // ── @PrePersist ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("@PrePersist createdAt")
    class PrePersist {

        @Test
        @DisplayName("createdAt is automatically set on save")
        void save_setsCreatedAt() {
            Notification n = persist(10L, NotificationType.IN_APP, false, "APP_CREATED");

            assertThat(n.getCreatedAt()).isNotNull();
        }
    }
}
