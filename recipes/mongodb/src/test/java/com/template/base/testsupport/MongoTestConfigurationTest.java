package com.template.base.testsupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
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

class MongoTestConfigurationTest {
  @TempDir
  Path directory;

  @Test
  void rejectsTheEffectiveSpringDatabaseAndUriWithoutCustomNormalVariables() {
    MockEnvironment environment = new MockEnvironment()
        .withProperty("spring.mongodb.uri", "mongodb://example.invalid/app")
        .withProperty("spring.mongodb.database", "app")
        .withProperty("MONGO_URI_TEST", "mongodb://example.invalid/app")
        .withProperty("MONGO_DATABASE_TEST", "app");
    assertThatThrownBy(() -> new MongoTestConfiguration(environment))
        .hasMessageContaining("separate database");
  }

  @Test
  void effectiveSpringPropertyTakesPrecedenceOverAnUnusedCustomVariable() {
    MockEnvironment environment = environment().withProperty("MONGO_DATABASE", "unused")
        .withProperty("spring.mongodb.database", "app")
        .withProperty("MONGO_DATABASE_TEST", "unused");
    new MongoTestConfiguration(environment);
    assertThat(environment.getProperty("spring.mongodb.database")).isEqualTo("unused");
  }

  @Test
  void standardSpringEnvironmentVariablesAreProtected() {
    MockEnvironment environment =
        new MockEnvironment().withProperty("MONGO_URI_TEST", "mongodb://example.invalid")
            .withProperty("MONGO_DATABASE_TEST", "app");
    environment.getPropertySources()
        .addFirst(new SystemEnvironmentPropertySource("fixture environment",
            Map.of("SPRING_MONGODB_URI", "mongodb://example.invalid", "SPRING_MONGODB_DATABASE",
                "app")));
    assertThatThrownBy(() -> new MongoTestConfiguration(environment))
        .hasMessageContaining("separate database");
  }

  @Test
  void testProfileCannotHideTheApplicationDefaultProfilesDatabase() throws IOException {
    Files.writeString(directory.resolve("application.yml"),
        "spring.profiles.default: dev\nspring.mongodb.uri: mongodb://example.invalid\n"
            + "spring.mongodb.database: base\n");
    Files.writeString(directory.resolve("application-dev.yml"), "spring.mongodb.database: app\n");
    Files.writeString(directory.resolve("application-test.yml"),
        "spring.mongodb.uri: ${MONGO_URI_TEST}\nspring.mongodb.database: ${MONGO_DATABASE_TEST}\n");
    assertThatThrownBy(() -> new MongoTestConfiguration(profile("app")))
        .hasMessageContaining("separate database");
    MockEnvironment safe = profile("app-test");
    new MongoTestConfiguration(safe);
    assertThat(safe.getProperty("spring.mongodb.database")).isEqualTo("app-test");
    new MongoTestConfiguration(safe);
  }

  private MockEnvironment profile(String database) {
    MockEnvironment environment =
        new MockEnvironment().withProperty("MONGO_URI_TEST", "mongodb://example.invalid")
            .withProperty("MONGO_DATABASE_TEST", database)
            .withProperty("spring.config.location", directory.toUri()
                .toString());
    environment.setActiveProfiles("test");
    ConfigDataEnvironmentPostProcessor.applyTo(environment);
    return environment;
  }

  private MockEnvironment environment() {
    return new MockEnvironment()
        .withProperty("MONGO_URI", "mongodb://example.invalid/app")
        .withProperty("MONGO_DATABASE", "app")
        .withProperty("MONGO_URI_TEST", "mongodb://example.invalid/app")
        .withProperty("MONGO_DATABASE_TEST", "app-test");
  }

  @Test
  void acceptsTheSharedUriWithAnIsolatedTestDatabase() {
    assertThatCode(() -> new MongoTestConfiguration(environment())).doesNotThrowAnyException();
  }

  @Test
  void rejectsTheNormalDatabaseEvenWhenTheUriIsShared() {
    assertThatThrownBy(() -> new MongoTestConfiguration(environment()
        .withProperty("MONGO_DATABASE_TEST", "app")))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("MONGO_DATABASE_TEST");
  }

  @Test
  void rejectsMissingTestSecrets() {
    assertThatThrownBy(() -> new MongoTestConfiguration(new MockEnvironment()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("MONGO_URI_TEST");
  }

  @Test
  void selectsTheTestDatabaseEvenWithoutAnExplicitTestProfile() {
    MockEnvironment environment = environment().withProperty("spring.mongodb.database", "app");
    new MongoTestConfiguration(environment);
    assertThat(environment.getProperty("spring.mongodb.database")).isEqualTo("app-test");
  }

  @Test
  void alsoProtectsTheUriDatabaseWhenTheNormalDatabaseVariableIsAbsent() {
    MockEnvironment environment = environment();
    environment.setProperty("MONGO_DATABASE", "");
    assertThatThrownBy(() -> new MongoTestConfiguration(environment
        .withProperty("MONGO_DATABASE_TEST", "app")))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void alsoProtectsTheApplicationsDefaultDatabaseWhenOnlyTheUriNamesAnotherDatabase() {
    MockEnvironment environment = environment();
    environment.setProperty("MONGO_DATABASE", "");
    environment.setProperty("MONGO_URI", "mongodb://example.invalid/uri-database");
    environment.setProperty("MONGO_DATABASE_TEST", "template-base");
    environment.setProperty("spring.mongodb.database", "template-base");
    assertThatThrownBy(() -> new MongoTestConfiguration(environment))
        .isInstanceOf(IllegalStateException.class);
  }
}
