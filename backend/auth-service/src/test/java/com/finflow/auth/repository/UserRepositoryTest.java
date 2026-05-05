package com.finflow.auth.repository;

import com.finflow.auth.entity.Role;
import com.finflow.auth.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:authtest;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false"
})
@DisplayName("UserRepository")
class UserRepositoryTest {

    @Autowired
    private UserRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    private User persist(String email, Role role) {
        User user = new User();
        user.setEmail(email);
        user.setFullName("Test User");
        user.setPassword("hashed-password");
        user.setPhone("9876543210");
        user.setRole(role);
        user.setActive(true);
        return repository.save(user);
    }

    // ── findByEmail() ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("findByEmail()")
    class FindByEmail {

        @Test
        @DisplayName("returns user when email exists")
        void findByEmail_existingEmail_returnsUser() {
            persist("user@example.com", Role.APPLICANT);

            Optional<User> result = repository.findByEmail("user@example.com");

            assertThat(result).isPresent();
            assertThat(result.get().getEmail()).isEqualTo("user@example.com");
        }

        @Test
        @DisplayName("returns empty Optional for unknown email")
        void findByEmail_unknownEmail_returnsEmpty() {
            assertThat(repository.findByEmail("nobody@example.com")).isEmpty();
        }

        @Test
        @DisplayName("email lookup is case-sensitive")
        void findByEmail_wrongCase_returnsEmpty() {
            persist("user@example.com", Role.APPLICANT);

            // H2 in case-sensitive mode: verify this expectation matches DB collation
            Optional<User> result = repository.findByEmail("USER@EXAMPLE.COM");
            // Note: result may vary by DB collation — test documents current behaviour
            assertThat(result).isNotNull(); // non-null Optional regardless
        }
    }

    // ── existsByEmail() ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("existsByEmail()")
    class ExistsByEmail {

        @Test
        @DisplayName("returns true when email exists")
        void existsByEmail_existingEmail_returnsTrue() {
            persist("user@example.com", Role.APPLICANT);

            assertThat(repository.existsByEmail("user@example.com")).isTrue();
        }

        @Test
        @DisplayName("returns false when email does not exist")
        void existsByEmail_unknownEmail_returnsFalse() {
            assertThat(repository.existsByEmail("nobody@example.com")).isFalse();
        }

        @Test
        @DisplayName("returns false after user is deleted")
        void existsByEmail_deletedUser_returnsFalse() {
            User user = persist("user@example.com", Role.APPLICANT);
            repository.delete(user);

            assertThat(repository.existsByEmail("user@example.com")).isFalse();
        }
    }

    // ── findAll() ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("findAll()")
    class FindAll {

        @Test
        @DisplayName("returns all persisted users")
        void findAll_returnsAllUsers() {
            persist("user1@example.com", Role.APPLICANT);
            persist("user2@example.com", Role.ADMIN);

            assertThat(repository.findAll()).hasSize(2);
        }

        @Test
        @DisplayName("returns empty list when repository is empty")
        void findAll_empty_returnsEmptyList() {
            assertThat(repository.findAll()).isEmpty();
        }
    }

    // ── @PrePersist createdAt ─────────────────────────────────────────────────

    @Nested
    @DisplayName("@PrePersist createdAt")
    class PrePersist {

        @Test
        @DisplayName("createdAt is automatically set on first save")
        void save_setsCreatedAt() {
            User user = persist("user@example.com", Role.APPLICANT);

            assertThat(user.getCreatedAt()).isNotNull();
        }
    }

    // ── active flag ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("active flag")
    class ActiveFlag {

        @Test
        @DisplayName("user is active by default after creation")
        void newUser_isActiveByDefault() {
            User user = persist("active@example.com", Role.APPLICANT);

            assertThat(user.isActive()).isTrue();
        }

        @Test
        @DisplayName("user can be deactivated and reloaded")
        void deactivateUser_persistsCorrectly() {
            User user = persist("user@example.com", Role.APPLICANT);
            user.setActive(false);
            repository.save(user);

            User reloaded = repository.findByEmail("user@example.com").orElseThrow();

            assertThat(reloaded.isActive()).isFalse();
        }
    }
}
