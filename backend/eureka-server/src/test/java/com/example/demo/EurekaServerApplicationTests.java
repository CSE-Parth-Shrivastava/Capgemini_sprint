package com.example.demo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Smoke test that verifies the Eureka Server application context loads successfully.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("EurekaServer Application Context Test")
class EurekaServerApplicationTests {

    @Test
    @DisplayName("Spring context loads without errors")
    void contextLoads() {
        // Context load failure causes this test to fail automatically.
    }
}
