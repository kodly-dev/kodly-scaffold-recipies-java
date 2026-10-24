package com.template.base.testsupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.zaxxer.hikari.HikariDataSource;
import java.util.concurrent.atomic.AtomicBoolean;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.util.ClassUtils;

/** Verifies Boot's bound beans without opening a database connection. */
class SqlBoundDataSourceTest {
  private static final String JDBC_PREFIX = jdbcPrefix();
  private static final String NORMAL = JDBC_PREFIX + "application";
  private static final String TEST = JDBC_PREFIX + "application-test";

  private static String jdbcPrefix() {
    // Use the project's installed driver; MySQL projects do not need PostgreSQL for these tests.
    if (ClassUtils.isPresent("org.postgresql.Driver", null)) {
      return "jdbc:postgresql://fixture.invalid/";
    }
    if (ClassUtils.isPresent("com.mysql.cj.jdbc.Driver", null)) {
      return "jdbc:mysql://fixture.invalid/";
    }
    throw new IllegalStateException("SQL guard tests require a PostgreSQL or MySQL JDBC driver");
  }

  private ApplicationContextRunner runner(String... properties) {
    return new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class))
        .withPropertyValues("spring.datasource.url=" + NORMAL, "SQL_URL_TEST=" + TEST,
            "SQL_USERNAME_TEST=test-user", "SQL_PASSWORD_TEST=test-password")
        .withPropertyValues(properties)
        .withInitializer(context -> new SqlTestConfiguration(context.getEnvironment()));
  }

  @ParameterizedTest
  @ValueSource(strings = {"spring.flyway.schemas", "spring.flyway.default-schema"})
  void mysqlMigrationSchemasCannotSelectAnotherDatabase(String property) {
    MockEnvironment environment = new MockEnvironment()
        .withProperty("spring.datasource.url", "jdbc:mysql://fixture.invalid/application")
        .withProperty("SQL_URL_TEST", "jdbc:mysql://fixture.invalid/application-test")
        .withProperty("SQL_USERNAME_TEST", "test-user")
        .withProperty("SQL_PASSWORD_TEST", "test-password")
        .withProperty(property, "application");
    assertThatThrownBy(() -> new SqlTestConfiguration(environment))
        .hasMessageContaining("Cannot isolate")
        .hasMessageContaining(property);
  }

  @Test
  void ordinaryDatasourceUsesTheTestProviderWithoutStartingThePool() {
    runner().run(context -> {
      assertThat(context).hasNotFailed();
      HikariDataSource dataSource = context.getBean(HikariDataSource.class);
      assertThat(dataSource.getJdbcUrl()).isEqualTo(TEST);
      assertThat(dataSource.getUsername()).isEqualTo("test-user");
      assertThat(dataSource.getPassword()).isEqualTo("test-password");
      assertThat(dataSource.getHikariPoolMXBean()).isNull();
    });
  }

  @ParameterizedTest
  @ValueSource(strings = {"spring.datasource.hikari.jdbc-url", "spring.datasource.hikari.jdbcUrl"})
  void laterHikariBindingCannotReplaceTheTestUrlOrCredentials(String urlProperty) {
    runner(urlProperty + "=" + NORMAL,
        "spring.datasource.hikari.username=normal-user",
        "spring.datasource.hikari.password=normal-password",
        "spring.datasource.hikari.maximum-pool-size=7").run(context -> {
          assertThat(context).hasNotFailed();
          HikariDataSource dataSource = context.getBean(HikariDataSource.class);
          assertThat(dataSource.getJdbcUrl()).isEqualTo(TEST);
          assertThat(dataSource.getUsername()).isEqualTo("test-user");
          assertThat(dataSource.getPassword()).isEqualTo("test-password");
          assertThat(dataSource.getMaximumPoolSize()).isEqualTo(7);
          assertThat(dataSource.getHikariPoolMXBean()).isNull();
        });
  }

  @ParameterizedTest
  @ValueSource(strings = {"spring.datasource.hikari.jdbc-url", "spring.flyway.url"})
  void testUrlCannotSelectAnApplicationPoolOrMigrationDatabase(String urlProperty) {
    runner("spring.datasource.url=" + JDBC_PREFIX + "other",
        urlProperty + "=" + NORMAL, "SQL_URL_TEST=" + NORMAL).run(context -> {
          assertThat(context).hasFailed();
          assertThat(context.getStartupFailure()).hasMessageContaining("separate database");
        });
  }

  @Test
  void separateFlywayConnectionUsesTheTestProviderBeforeMigrationsRun() {
    AtomicBoolean initialized = new AtomicBoolean();
    runner("spring.flyway.url=" + NORMAL,
        "spring.flyway.user=normal-user", "spring.flyway.password=normal-password")
        .withConfiguration(AutoConfigurations.of(FlywayAutoConfiguration.class))
        .withBean(FlywayMigrationStrategy.class, () -> flyway -> {
          SimpleDriverDataSource migration =
              (SimpleDriverDataSource) flyway.getConfiguration()
                  .getDataSource();
          assertThat(migration.getUrl()).isEqualTo(TEST);
          assertThat(migration.getUsername()).isEqualTo("test-user");
          assertThat(migration.getPassword()).isEqualTo("test-password");
          initialized.set(true);
        })
        .run(context -> {
          assertThat(context).hasNotFailed();
          assertThat(context).hasSingleBean(Flyway.class);
          assertThat(initialized).isTrue();
          assertThat(context.getBean(HikariDataSource.class)
              .getHikariPoolMXBean()).isNull();
        });
  }

  @ParameterizedTest
  @CsvSource({
      "spring.datasource.jndi-name,java:comp/env/jdbc/application",
      "spring.datasource.type,org.springframework.jdbc.datasource.SimpleDriverDataSource",
      "spring.datasource.xa.data-source-class-name,org.postgresql.xa.PGXADataSource",
      "spring.datasource.hikari.data-source-class-name,org.postgresql.ds.PGSimpleDataSource",
      "spring.datasource.hikari.data-source-jndi,java:comp/env/jdbc/application",
      "spring.datasource.hikari.credentials-provider-class-name,java.lang.String",
      "spring.datasource.hikari.data-source-properties.databaseName,application",
      "spring.datasource.hikari.catalog,application",
      "spring.datasource.hikari.schema,application",
      "spring.datasource.hikari.connection-init-sql,USE application",
      "spring.datasource.hikari.connection-test-query,USE application",
      "spring.flyway.jdbc-properties.databaseName,application",
      "spring.flyway.init-sqls,USE application"
  })
  void unverifiableConnectionSelectorsFailBeforeAnyApplicationBean(String property, String value) {
    AtomicBoolean beanCreated = new AtomicBoolean();
    runner(property + "=" + value).withBean("applicationEffect", Object.class, () -> {
      beanCreated.set(true);
      return new Object();
    })
        .run(context -> {
          assertThat(context).hasFailed();
          assertThat(context.getStartupFailure()).hasMessageContaining("Cannot isolate")
              .hasMessageContaining(property.substring(0, property.lastIndexOf('.')));
          assertThat(beanCreated).isFalse();
        });
  }
}
