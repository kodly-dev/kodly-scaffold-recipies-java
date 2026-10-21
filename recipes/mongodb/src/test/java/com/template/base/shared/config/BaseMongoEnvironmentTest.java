package com.template.base.shared.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.mongodb.autoconfigure.MongoProperties;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.core.io.FileSystemResource;

/** Checks the shipped configuration without Testcontainers overriding its connection settings. */
class BaseMongoEnvironmentTest {

  @Test
  void kodlySecretsConfigureTheConnectionAndOverrideTheUriDatabase() throws IOException {
    MongoProperties properties = properties(Map.of(
        "MONGO_URI", "mongodb://recipe-host:27019/uri-database?authSource=admin",
        "MONGO_DATABASE", "project-database"));

    assertThat(properties.getUri()).isEqualTo(
        "mongodb://recipe-host:27019/uri-database?authSource=admin");
    assertThat(properties.getDatabase()).isEqualTo("project-database");
  }

  @Test
  void standaloneDevelopmentRetainsLocalDefaults() throws IOException {
    MongoProperties properties = properties(Map.of());

    assertThat(properties.getUri()).startsWith("mongodb://localhost:27017/");
    assertThat(properties.getDatabase()).isNotBlank();
    assertThat(properties.getUri()).endsWith("/" + properties.getDatabase());
  }

  @Test
  void testProfileUsesTheSameServerAndOverridesItsUriDatabase() throws IOException {
    String uri = "mongodb://recipe-host:27019/project-database?authSource=admin";
    MongoProperties properties = properties(Map.of("MONGO_URI", uri,
        "MONGO_DATABASE", "project-database", "MONGO_URI_TEST", uri,
        "MONGO_DATABASE_TEST", "project-database-test"), true);

    assertThat(properties.getUri()).isEqualTo(uri);
    assertThat(properties.getDatabase()).isEqualTo("project-database-test");
  }

  @Test
  void testProfileDoesNotFallBackToTheNormalDatabase() throws IOException {
    MongoProperties properties = properties(Map.of("MONGO_URI", "mongodb://host/normal",
        "MONGO_DATABASE", "normal"), true);
    // Binding strings leaves missing placeholders intact; the test configuration rejects them.
    assertThat(properties.getUri()).isEqualTo("${MONGO_URI_TEST}");
    assertThat(properties.getDatabase()).isEqualTo("${MONGO_DATABASE_TEST}");
  }

  private MongoProperties properties(Map<String, Object> variables) throws IOException {
    return properties(variables, false);
  }

  private MongoProperties properties(Map<String, Object> variables, boolean test)
      throws IOException {
    StandardEnvironment environment = new StandardEnvironment();
    environment.getPropertySources()
        .remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
    environment.getPropertySources()
        .remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
    environment.getPropertySources()
        .addFirst(new SystemEnvironmentPropertySource("kodly", variables));
    Path yaml = Files.exists(Path.of("application.yml"))
        ? Path.of("application.yml")
        : Path.of("src/main/resources/application.yml");
    new YamlPropertySourceLoader().load("recipe", new FileSystemResource(yaml))
        .forEach(source -> environment.getPropertySources()
            .addLast(source));
    if (test) {
      new YamlPropertySourceLoader()
          .load("test", new FileSystemResource("src/test/resources/application-test.yml"))
          .forEach(source -> environment.getPropertySources()
              .addFirst(source));
    }
    return Binder.get(environment)
        .bind("spring.mongodb", MongoProperties.class)
        .get();
  }
}
