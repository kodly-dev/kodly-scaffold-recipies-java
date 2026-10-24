package com.template.base;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.support.PropertySourceDescriptor;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.context.MergedContextConfiguration;

class BaseTestEnvironmentTest {
  @TempDir
  Path directory;

  @Test
  void bootTestLoadersIndexedActiveProfilesDoNotBecomeApplicationProfiles() throws IOException {
    Files.writeString(directory.resolve("application.yml"), "spring.profiles.default: dev\n");
    Files.writeString(directory.resolve("application-dev.yml"),
        "spring.data.redis.url: redis://normal/4\n");
    Files.writeString(directory.resolve("application-test.yml"),
        "spring.data.redis.url: redis://tests/2\n");
    MockEnvironment environment = new MockEnvironment()
        .withProperty("spring.config.location", directory.toUri()
            .toString());
    environment.setActiveProfiles("test");
    environment.getPropertySources()
        .addFirst(new MapPropertySource("active-test-profiles",
            Map.of("spring.profiles.active[0]", "test")));
    assertThat(BaseTestEnvironment.application(environment)
        .getProperty("spring.data.redis.url"))
        .isEqualTo("redis://normal/4");
  }

  @Test
  void ignoresTestOnlyOverridesAndKeepsTheIndependentSnapshot() throws IOException {
    Files.writeString(directory.resolve("application.yml"),
        "spring.datasource.url: jdbc:postgresql://normal/app\n");
    MockEnvironment environment = new MockEnvironment()
        .withProperty("spring.config.location", directory.toUri()
            .toString());
    for (String name : List.of("Inlined Test Properties", "Dynamic Test Properties",
        "kodlySqlTests")) {
      environment.getPropertySources()
          .addFirst(new MapPropertySource(name,
              Map.of("spring.datasource.url", "jdbc:postgresql://tests/app")));
    }
    Environment normal = BaseTestEnvironment.application(environment);
    assertThat(normal.getProperty("spring.datasource.url"))
        .isEqualTo("jdbc:postgresql://normal/app");
    assertThat(environment.getProperty("spring.datasource.url"))
        .isEqualTo("jdbc:postgresql://tests/app");
    assertThat(BaseTestEnvironment.application(environment)).isSameAs(normal);
  }

  @Test
  void excludesFileBasedTestPropertySourcesFromTheNormalProvider() throws IOException {
    Files.writeString(directory.resolve("application.yml"), "spring.mongodb.database: normal\n");
    Path override = directory.resolve("test-provider.properties");
    Files.writeString(override, "spring.mongodb.database=tests\n");
    String location = override.toUri()
        .toString();
    String name = new DefaultResourceLoader().getResource(location)
        .getDescription();
    MockEnvironment environment = new MockEnvironment()
        .withProperty("spring.config.location", directory.toUri()
            .toString());
    environment.getPropertySources()
        .addFirst(new MapPropertySource(name,
            Map.of("spring.mongodb.database", "tests")));
    MergedContextConfiguration configuration = new MergedContextConfiguration(getClass(),
        new String[0], new Class<?>[0], Set.of(), new String[0],
        List.of(new PropertySourceDescriptor(location)), new String[0], Set.of(), null, null, null);
    assertThat(BaseTestEnvironment.capture(environment, configuration)
        .getProperty("spring.mongodb.database"))
        .isEqualTo("normal");
  }

  @Test
  void keepsAnExplicitApplicationProfileWhileRemovingTheTestProfile() throws IOException {
    Files.writeString(directory.resolve("application.yml"),
        "spring.data.redis.url: redis://normal/0\n");
    Files.writeString(directory.resolve("application-prod.yml"),
        "spring.data.redis.url: redis://normal/4\n");
    Files.writeString(directory.resolve("application-test.yml"),
        "spring.data.redis.url: redis://tests/2\n");
    MockEnvironment environment = new MockEnvironment()
        .withProperty("spring.config.location", directory.toUri()
            .toString())
        .withProperty("spring.profiles.active", "prod,test");
    environment.setActiveProfiles("prod", "test");
    ConfigDataEnvironmentPostProcessor.applyTo(environment);
    assertThat(BaseTestEnvironment.application(environment)
        .getProperty("spring.data.redis.url"))
        .isEqualTo("redis://normal/4");
  }
}
