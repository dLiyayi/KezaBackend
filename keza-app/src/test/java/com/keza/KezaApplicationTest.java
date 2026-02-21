package com.keza;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test that loads the full Spring application context.
 * Requires Docker Compose services running (PostgreSQL, Redis, RabbitMQ).
 * Start with: docker compose -f docker/docker-compose.yml up -d
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("KezaApplication - Context Loading")
class KezaApplicationTest {

    @Test
    @DisplayName("should load the Spring application context successfully")
    void contextLoads() {
        // If this test passes, the entire Spring context was assembled correctly,
        // including all module beans, JPA entities, security config, and message brokers.
        assertThat(true).isTrue();
    }
}
