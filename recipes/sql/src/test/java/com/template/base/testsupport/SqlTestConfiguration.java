package com.template.base.testsupport;

import com.template.base.BaseTestEnvironment;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

/** Uses an explicitly configured provider and refuses the application's database. */
@TestConfiguration(proxyBeanMethods = false)
public class SqlTestConfiguration {

  /** Validates the isolated external provider before application initialization. */
  public SqlTestConfiguration(Environment environment) {
    final String testUrl = required(environment, "SQL_URL_TEST");
    Environment application = BaseTestEnvironment.application(environment);
    rejectUnverifiable(application, testUrl);
    rejectUnverifiable(environment, testUrl);
    String normalUrl = property(application, "spring.datasource.url");
    if (!StringUtils.hasText(normalUrl)) {
      normalUrl = application.getProperty("SQL_URL",
          "jdbc:postgresql://localhost:5432/template-base");
    }
    String testDatabase = database(testUrl);
    for (String candidate : List.of(normalUrl,
        property(application, "spring.datasource.hikari.jdbc-url"),
        property(application, "spring.flyway.url"))) {
      if (StringUtils.hasText(candidate) && testDatabase.equals(database(candidate))) {
        throw new IllegalStateException(
            "SQL_URL_TEST must select a separate database "
                + "from application and migration providers.");
      }
    }
    String username = required(environment, "SQL_USERNAME_TEST");
    String password = required(environment, "SQL_PASSWORD_TEST");
    // Boot binds Hikari after generic datasource properties; Flyway can create its own datasource.
    ((ConfigurableEnvironment) environment).getPropertySources()
        .addFirst(new MapPropertySource("kodlySqlTests", Map.of(
            "spring.datasource.url", testUrl, "spring.datasource.username", username,
            "spring.datasource.password", password,
            "spring.datasource.hikari.jdbc-url", testUrl,
            "spring.datasource.hikari.username", username,
            "spring.datasource.hikari.password", password,
            "spring.flyway.url", testUrl, "spring.flyway.user", username,
            "spring.flyway.password", password)));
  }

  private static String property(Environment environment, String name) {
    return Binder.get(environment)
        .bind(name, String.class)
        .orElse("");
  }

  private static void rejectUnverifiable(Environment environment, String testUrl) {
    String type = property(environment, "spring.datasource.type");
    if (StringUtils.hasText(type) && !type.equals("com.zaxxer.hikari.HikariDataSource")) {
      throw cannotIsolate("spring.datasource.type");
    }
    for (String name : List.of("spring.datasource.jndi-name",
        "spring.datasource.xa.data-source-class-name",
        "spring.datasource.hikari.data-source-class-name",
        "spring.datasource.hikari.data-source-jndi",
        "spring.datasource.hikari.credentials-provider-class-name",
        "spring.datasource.hikari.catalog", "spring.datasource.hikari.schema",
        "spring.datasource.hikari.connection-init-sql",
        "spring.datasource.hikari.connection-test-query")) {
      if (StringUtils.hasText(property(environment, name))) {
        throw cannotIsolate(name);
      }
    }
    for (String name : List.of("spring.datasource.hikari.data-source-properties",
        "spring.flyway.jdbc-properties")) {
      if (!Binder.get(environment)
          .bind(name, Bindable.mapOf(String.class, Object.class))
          .orElse(Map.of())
          .isEmpty()) {
        throw cannotIsolate(name);
      }
    }
    if (!Binder.get(environment)
        .bind("spring.flyway.init-sqls", Bindable.listOf(String.class))
        .orElse(List.of())
        .isEmpty()) {
      throw cannotIsolate("spring.flyway.init-sqls");
    }
    if (testUrl.startsWith("jdbc:mysql://")) {
      if (StringUtils.hasText(property(environment, "spring.flyway.default-schema"))) {
        throw cannotIsolate("spring.flyway.default-schema");
      }
      if (!Binder.get(environment)
          .bind("spring.flyway.schemas", Bindable.listOf(String.class))
          .orElse(List.of())
          .isEmpty()) {
        throw cannotIsolate("spring.flyway.schemas");
      }
    }
  }

  private static IllegalStateException cannotIsolate(String name) {
    return new IllegalStateException("Cannot isolate SQL tests with " + name
        + "; use the TEST JDBC URL and credentials without alternate connection selectors.");
  }

  private static String required(Environment environment, String name) {
    String value = environment.getProperty(name);
    if (!StringUtils.hasText(value)) {
      throw new IllegalStateException(
          "Set " + name + " to an isolated external test provider before running tests.");
    }
    return value;
  }

  private static String database(String url) {
    if (!url.startsWith("jdbc:postgresql://") && !url.startsWith("jdbc:mysql://")) {
      throw new IllegalStateException("Use a PostgreSQL or MySQL JDBC URL for SQL tests.");
    }
    URI uri = URI.create(url.substring(5));
    if (uri.getHost() == null || !StringUtils.hasText(uri.getPath()) || "/".equals(uri.getPath())) {
      throw new IllegalStateException("SQL test URLs must name a host and database.");
    }
    int port =
        uri.getPort() == -1 ? "postgresql".equals(uri.getScheme()) ? 5432 : 3306 : uri.getPort();
    return uri.getScheme() + "://" + uri.getHost()
        .toLowerCase(Locale.ROOT) + ":" + port + uri.getPath();
  }
}
