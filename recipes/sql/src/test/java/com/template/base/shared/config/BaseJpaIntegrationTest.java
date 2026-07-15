package com.template.base.shared.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.template.base.BaseApplication;
import java.sql.Connection;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(classes = BaseApplication.class)
@Testcontainers(disabledWithoutDocker = true)
class BaseJpaIntegrationTest {

  @Container
  @ServiceConnection
  static PostgreSQLContainer postgres =
      new PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine")).withDatabaseName("template-base")
        .withUsername("postgres")
        .withPassword("postgres");

  @Autowired
  private DataSource dataSource;

  @Test
  void contextLoadsAgainstPostgresContainer() throws Exception {
    try (Connection connection = dataSource.getConnection()) {
      assertThat(connection.isValid(5)).isTrue();
    }
  }

  @Test
  void flywayPlaceholderMigrationApplied() throws Exception {
    try (Connection connection = dataSource.getConnection();
        var statement = connection.createStatement();
        var resultSet = statement.executeQuery("SELECT 1")) {
      assertThat(resultSet.next()).isTrue();
      assertThat(resultSet.getInt(1)).isEqualTo(1);
    }
  }
}
