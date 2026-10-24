package com.template.base.testsupport;

import static org.assertj.core.api.Assertions.assertThat;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.util.ClassUtils;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestContextManager;
import org.springframework.test.context.TestPropertySource;

/** Exercises real Spring dynamic-property ordering without opening a provider connection. */
class SqlDynamicProviderTest {
  private static final String PREFIX = ClassUtils.isPresent("org.postgresql.Driver", null)
      ? "jdbc:postgresql://fixture.invalid/"
      : "jdbc:mysql://fixture.invalid/";
  @Test
  void lateDynamicHikariBindingCannotEscapeTheSelectedTestProvider() {
    HikariDataSource bound = new TestContextManager(UnsafeDynamicTest.class)
        .getTestContext()
        .getApplicationContext()
        .getBean(HikariDataSource.class);
    assertThat(bound.getJdbcUrl()).isEqualTo(PREFIX + "application-test");
    assertThat(bound.getHikariPoolMXBean()).isNull();
  }

  @ContextConfiguration(classes = Fixture.class)
  @TestPropertySource(properties = {"SQL_URL_TEST=jdbc:mysql://fixture.invalid/application-test",
      "SQL_USERNAME_TEST=test-user", "SQL_PASSWORD_TEST=test-password"})
  static class UnsafeDynamicTest {
    @DynamicPropertySource
    static void applicationOverride(DynamicPropertyRegistry properties) {
      properties.add("SQL_URL_TEST", () -> PREFIX + "application-test");
      properties.add("spring.datasource.hikari.jdbc-url",
          () -> PREFIX + "application");
    }
  }

  @Configuration(proxyBeanMethods = false)
  @EnableConfigurationProperties(DataSourceProperties.class)
  static class Fixture {
    @Bean
    @ConfigurationProperties("spring.datasource.hikari")
    HikariDataSource dataSource(DataSourceProperties properties) {
      return properties.initializeDataSourceBuilder()
          .type(HikariDataSource.class)
          .build();
    }
  }
}
