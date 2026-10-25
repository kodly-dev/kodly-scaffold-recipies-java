package com.template.base.testsupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.mock.env.MockEnvironment;

class RedisTestConfigurationTest {
  @TempDir
  Path directory;

  @Test
  void loopbackAliasesCannotMakeTheApplicationDatabaseLookSeparate() {
    for (String host : new String[]{"localhost", "localhost.", "127.0.0.1", "127.0.0.2", "[::1]",
        "0.0.0.0", "[::ffff:127.0.0.1]", "[0::1]"}) {
      MockEnvironment environment =
          new MockEnvironment().withProperty("REDIS_URL", "redis://localhost:6379/0")
              .withProperty("REDIS_URL_TEST", "redis://" + host + ":6379/0");
      assertThatThrownBy(() -> new RedisTestConfiguration(environment))
          .hasMessageContaining("separate server or database");
    }
  }

  @Test
  void standardSpringClusterAndSentinelSettingsCannotOverrideTheTestUrl() {
    for (String selector : new String[]{"cluster", "sentinel", "masterreplica"}) {
      new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(DataRedisAutoConfiguration.class))
          .withUserConfiguration(RedisTestConfiguration.class)
          .withPropertyValues("REDIS_URL=redis://app-cache:6379/0",
              "REDIS_URL_TEST=redis://test-cache:6379/1",
              "spring.data.redis." + selector + ".nodes=app-cache:6379")
          .withPropertyValues(selector.equals("sentinel")
              ? new String[]{"spring.data.redis.sentinel.master=app-master"}
              : new String[0])
          .run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure())
                .hasRootCauseMessage("Cannot isolate Redis tests with spring.data.redis."
                    + selector + "; use REDIS_URL_TEST without alternate connection selectors.");
          });
    }
  }

  @Test
  void protectsSpringHostPortAndDatabaseWhenNoUrlIsConfigured() throws IOException {
    Files.writeString(directory.resolve("application.yml"),
        "spring.application.name: host-settings-fixture\n");
    MockEnvironment environment = new MockEnvironment()
        .withProperty("spring.config.location", directory.toUri()
            .toString())
        .withProperty("spring.data.redis.host", "db")
        .withProperty("spring.data.redis.port", "6380")
        .withProperty("spring.data.redis.database", "4")
        .withProperty("REDIS_URL_TEST", "redis://db:6380/4");
    assertThatThrownBy(() -> new RedisTestConfiguration(environment))
        .hasMessageContaining("separate server or database");
  }

  @Test
  void springUrlTakesPrecedenceOverStandaloneSettings() {
    MockEnvironment environment = new MockEnvironment()
        .withProperty("spring.data.redis.url", "redis://normal/4")
        .withProperty("spring.data.redis.host", "db")
        .withProperty("spring.data.redis.database", "2")
        .withProperty("REDIS_URL_TEST", "redis://db/2");
    new RedisTestConfiguration(environment);
    assertThat(environment.getProperty("spring.data.redis.url")).isEqualTo("redis://db/2");
  }

  @Test
  void rejectsTheEffectiveSpringUrlWithoutCustomNormalVariables() {
    MockEnvironment environment =
        new MockEnvironment().withProperty("spring.data.redis.url", "redis://db/1")
            .withProperty("REDIS_URL_TEST", "redis://db/1");
    assertThatThrownBy(() -> new RedisTestConfiguration(environment))
        .hasMessageContaining("separate server or database");
  }

  @Test
  void effectiveSpringPropertyTakesPrecedenceOverAnUnusedCustomVariable() {
    MockEnvironment environment = provider().withProperty("spring.data.redis.url", "redis://db/2")
        .withProperty("REDIS_URL_TEST", "redis://db/0");
    new RedisTestConfiguration(environment);
    assertThat(environment.getProperty("spring.data.redis.url")).isEqualTo("redis://db/0");
  }

  @Test
  void standardSpringEnvironmentVariablesAreProtected() {
    MockEnvironment environment =
        new MockEnvironment().withProperty("REDIS_URL_TEST", "redis://db/1");
    environment.getPropertySources()
        .addFirst(new SystemEnvironmentPropertySource("fixture environment",
            Map.of("SPRING_DATA_REDIS_URL", "redis://db/1")));
    assertThatThrownBy(() -> new RedisTestConfiguration(environment))
        .hasMessageContaining("separate server or database");
  }

  @Test
  void testProfileCannotHideTheApplicationDefaultProfilesDatabase() throws IOException {
    Files.writeString(directory.resolve("application.yml"),
        "spring.profiles.default: dev\nspring.data.redis.url: redis://db/0\n");
    Files.writeString(directory.resolve("application-dev.yml"),
        "spring.data.redis.url: redis://db/1\n");
    Files.writeString(directory.resolve("application-test.yml"),
        "spring.data.redis.url: ${REDIS_URL_TEST}\n");
    assertThatThrownBy(() -> new RedisTestConfiguration(profile("redis://db/1")))
        .hasMessageContaining("separate server or database");
    MockEnvironment safe = profile("redis://db/2");
    new RedisTestConfiguration(safe);
    assertThat(safe.getProperty("spring.data.redis.url")).isEqualTo("redis://db/2");
    new RedisTestConfiguration(safe);
  }

  private MockEnvironment profile(String url) {
    MockEnvironment environment = new MockEnvironment().withProperty("REDIS_URL_TEST", url)
        .withProperty("spring.config.location", directory.toUri()
            .toString());
    environment.setActiveProfiles("test");
    ConfigDataEnvironmentPostProcessor.applyTo(environment);
    return environment;
  }

  @Test
  void missingTestProviderFailsBeforeApplicationInitialization() {
    assertThatThrownBy(() -> new RedisTestConfiguration(new MockEnvironment()))
        .hasMessageContaining("REDIS_URL_TEST");
  }

  @Test
  void tlsCredentialsAndDefaultPortsDoNotIsolateTheNormalDatabase() {
    for (String url : new String[]{"rediss://DB:6379/0", "redis://different:password@db/",
        "redis://db"}) {
      assertThatThrownBy(
          () -> new RedisTestConfiguration(provider().withProperty("REDIS_URL_TEST", url)))
          .hasMessageContaining("separate server or database");
    }
  }

  @Test
  void testDatabaseOverridesOrdinaryRedisSettingsWithoutTheTestProfile() {
    MockEnvironment environment = provider().withProperty("spring.data.redis.database", "0");
    new RedisTestConfiguration(environment);
    assertThat(environment.getProperty("spring.data.redis.url")).isEqualTo("redis://db/1");
    assertThat(environment.getProperty("spring.data.redis.database", Integer.class)).isEqualTo(1);
  }

  private static MockEnvironment provider() {
    return new MockEnvironment().withProperty("REDIS_URL", "redis://db/0")
        .withProperty("REDIS_URL_TEST", "redis://db/1");
  }
}
