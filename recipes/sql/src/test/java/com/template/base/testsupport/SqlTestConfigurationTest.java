package com.template.base.testsupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.mock.env.MockEnvironment;

class SqlTestConfigurationTest {
  @TempDir
  Path directory;

  @Test
  void rejectsTheEffectiveSpringDatasourceEvenWithoutCustomNormalVariables() {
    MockEnvironment environment =
        credentials().withProperty("spring.datasource.url", "jdbc:postgresql://db/app")
            .withProperty("SQL_URL_TEST", "jdbc:postgresql://db/app");
    assertThatThrownBy(() -> new SqlTestConfiguration(environment))
        .hasMessageContaining("separate database");
  }

  @Test
  void effectiveSpringPropertyTakesPrecedenceOverAnUnusedCustomVariable() {
    MockEnvironment environment =
        provider().withProperty("spring.datasource.url", "jdbc:postgresql://db/actual-app")
            .withProperty("SQL_URL_TEST", "jdbc:postgresql://db/app");
    new SqlTestConfiguration(environment);
    assertThat(environment.getProperty("spring.datasource.url"))
        .isEqualTo("jdbc:postgresql://db/app");
  }

  @Test
  void standardSpringEnvironmentVariablesAreProtected() {
    MockEnvironment environment =
        credentials().withProperty("SQL_URL_TEST", "jdbc:postgresql://db/app");
    environment.getPropertySources()
        .addFirst(new SystemEnvironmentPropertySource("fixture environment",
            Map.of("SPRING_DATASOURCE_URL", "jdbc:postgresql://db/app")));
    assertThatThrownBy(() -> new SqlTestConfiguration(environment))
        .hasMessageContaining("separate database");
  }

  @Test
  void testProfileCannotHideTheApplicationDefaultProfilesDatabase() throws IOException {
    Files.writeString(directory.resolve("application.yml"),
        "spring.profiles.default: dev\nspring.datasource.url: jdbc:postgresql://db/base\n");
    Files.writeString(directory.resolve("application-dev.yml"),
        "spring.datasource.url: jdbc:postgresql://db/app\n");
    Files.writeString(directory.resolve("application-test.yml"),
        "spring.datasource.url: ${SQL_URL_TEST}\n");
    MockEnvironment unsafe = profile("jdbc:postgresql://db/app");
    assertThatThrownBy(() -> new SqlTestConfiguration(unsafe))
        .hasMessageContaining("separate database");
    MockEnvironment safe = profile("jdbc:postgresql://db/app-test");
    new SqlTestConfiguration(safe);
    assertThat(safe.getProperty("spring.datasource.url"))
        .isEqualTo("jdbc:postgresql://db/app-test");
    new SqlTestConfiguration(safe);
  }

  private MockEnvironment profile(String testUrl) {
    MockEnvironment environment = credentials().withProperty("SQL_URL_TEST", testUrl)
        .withProperty("spring.config.location", directory.toUri()
            .toString());
    environment.setActiveProfiles("test");
    ConfigDataEnvironmentPostProcessor.applyTo(environment);
    return environment;
  }

  private static MockEnvironment credentials() {
    return new MockEnvironment().withProperty("SQL_USERNAME_TEST", "tests")
        .withProperty("SQL_PASSWORD_TEST", "synthetic");
  }

  @Test
  void missingProviderCredentialsFailBeforeApplicationInitialization() {
    for (String name : new String[]{"SQL_URL_TEST", "SQL_USERNAME_TEST", "SQL_PASSWORD_TEST"}) {
      MockEnvironment environment = provider();
      environment.setProperty(name, "");
      assertThatThrownBy(() -> new SqlTestConfiguration(environment)).hasMessageContaining(name);
    }
  }

  @Test
  void credentialsQueryOptionsAndDefaultPortsDoNotMakeTheNormalDatabaseSafe() {
    for (String url : new String[]{"jdbc:postgresql://DB:5432/app?ssl=true",
        "jdbc:postgresql://different-user:password@db/app", "jdbc:postgresql://db/%61pp"}) {
      assertThatThrownBy(
          () -> new SqlTestConfiguration(provider().withProperty("SQL_URL_TEST", url)))
          .hasMessageContaining("separate database");
    }
  }

  @Test
  void testProviderOverridesOrdinaryDatasourcePropertiesWithoutTheTestProfile() {
    MockEnvironment environment =
        provider().withProperty("spring.datasource.url", "jdbc:postgresql://db/app");
    new SqlTestConfiguration(environment);
    assertThat(environment.getProperty("spring.datasource.url"))
        .isEqualTo("jdbc:postgresql://db/app-test");
    assertThat(environment.getProperty("spring.datasource.username")).isEqualTo("tests");
  }

  private static MockEnvironment provider() {
    return new MockEnvironment().withProperty("SQL_URL", "jdbc:postgresql://db/app")
        .withProperty("SQL_URL_TEST", "jdbc:postgresql://db/app-test")
        .withProperty("SQL_USERNAME_TEST", "tests")
        .withProperty("SQL_PASSWORD_TEST", "synthetic");
  }
}
