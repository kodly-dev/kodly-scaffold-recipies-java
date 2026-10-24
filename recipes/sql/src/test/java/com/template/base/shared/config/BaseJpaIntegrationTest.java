package com.template.base.shared.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.template.base.BaseIntegrationTest;
import java.sql.Connection;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class BaseJpaIntegrationTest extends BaseIntegrationTest {

  @Autowired
  private DataSource dataSource;

  @Test
  void contextLoadsAgainstTheConfiguredTestProvider() throws Exception {
    try (Connection connection = dataSource.getConnection()) {
      assertThat(connection.isValid(5)).isTrue();
      assertThat(connection.getMetaData()
          .getURL()).isEqualTo(System.getenv("SQL_URL_TEST"));
    }
  }

  @Test
  void flywayPlaceholderMigrationApplied() throws Exception {
    try (Connection connection = dataSource.getConnection();
        var statement = connection.createStatement();
        var resultSet = statement.executeQuery(
            "SELECT version, success FROM flyway_schema_history WHERE version = '1'")) {
      assertThat(resultSet.next()).isTrue();
      assertThat(resultSet.getString("version")).isEqualTo("1");
      assertThat(resultSet.getBoolean("success")).isTrue();
    }
  }
}
