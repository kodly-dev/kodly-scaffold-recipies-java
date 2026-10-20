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

  private MongoProperties properties(Map<String, Object> variables) throws IOException {
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
    return Binder.get(environment)
        .bind("spring.mongodb", MongoProperties.class)
        .get();
  }
}
