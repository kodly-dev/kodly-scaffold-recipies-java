package com.template.base.testsupport;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/** Supplies isolated PostgreSQL connections for JPA and Flyway integration tests. */
@TestConfiguration(proxyBeanMethods = false)
public class SqlTestConfiguration {

  @Bean
  @ServiceConnection
  PostgreSQLContainer postgres() {
    return new PostgreSQLContainer(DockerImageName.parse("postgres:18.6-alpine"))
        .withDatabaseName("template-base")
        .withUsername("postgres")
        .withPassword("postgres");
  }
}
