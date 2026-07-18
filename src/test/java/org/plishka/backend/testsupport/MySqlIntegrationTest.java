package org.plishka.backend.testsupport;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

@ExtendWith(MySqlIntegrationTest.DockerAvailableCondition.class)
public abstract class MySqlIntegrationTest {
    private static final DockerImageName MYSQL_IMAGE = DockerImageName.parse("mysql:8.4.0");

    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>(MYSQL_IMAGE)
            .withDatabaseName("backend_test")
            .withUsername("test")
            .withPassword("test")
            .withUrlParam("allowPublicKeyRetrieval", "true")
            .withUrlParam("connectionTimeZone", "UTC");

    private static Boolean dockerAvailable;

    @DynamicPropertySource
    static void registerMySqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> mySql().getJdbcUrl());
        registry.add("spring.datasource.username", () -> mySql().getUsername());
        registry.add("spring.datasource.password", () -> mySql().getPassword());
        registry.add("spring.datasource.driver-class-name", () -> mySql().getDriverClassName());
        registry.add("spring.liquibase.contexts", () -> "test");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
    }

    private static synchronized MySQLContainer<?> mySql() {
        if (!MYSQL.isRunning()) {
            MYSQL.start();
        }
        return MYSQL;
    }

    private static synchronized boolean isDockerAvailable() {
        if (dockerAvailable == null) {
            dockerAvailable = DockerClientFactory.instance().isDockerAvailable();
        }
        return dockerAvailable;
    }

    static class DockerAvailableCondition implements ExecutionCondition {
        @Override
        public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
            if (isDockerAvailable()) {
                return ConditionEvaluationResult.enabled("Docker is available");
            }
            return ConditionEvaluationResult.disabled("Docker is not available");
        }
    }
}
