package com.rishi.digitalbankingapi.integration;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base class for integration tests: boots the full Spring context (real
 * Spring Security filter chain included) against an actual Postgres
 * container rather than H2, so Flyway migrations and Postgres-specific
 * behavior (e.g. row locking) are exercised exactly as in production.
 *
 * Uses @DynamicPropertySource + manual container start instead of Boot 4.1's
 * @ServiceConnection: the latter's reflection-based Container-type check
 * fails at runtime under Surefire in this project (confirmed the class
 * hierarchy itself is fine via a standalone reflection check), so this
 * older, more battle-tested wiring mechanism is used instead.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
public abstract class AbstractIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
