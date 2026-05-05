package com.finflow.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke test that verifies the Config Server application context loads successfully.
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("ConfigServer Application Context Test")
class ConfigServerApplicationTest {

    @Test
    @DisplayName("Spring context loads without errors")
    void contextLoads() {
        // If the context fails to load, this test fails automatically.
    }
}
