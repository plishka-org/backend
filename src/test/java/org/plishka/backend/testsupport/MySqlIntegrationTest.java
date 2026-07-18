package org.plishka.backend.testsupport;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers(disabledWithoutDocker = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class MySqlIntegrationTest {
    private static final DockerImageName MYSQL_IMAGE = DockerImageName.parse("mysql:8.4.0");

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>(MYSQL_IMAGE)
            .withDatabaseName("backend_test")
            .withUsername("test")
            .withPassword("test")
            .withUrlParam("allowPublicKeyRetrieval", "true")
            .withUrlParam("connectionTimeZone", "UTC");

    @DynamicPropertySource
    static void registerMySqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
        registry.add("spring.liquibase.contexts", () -> "test");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
    }
}
