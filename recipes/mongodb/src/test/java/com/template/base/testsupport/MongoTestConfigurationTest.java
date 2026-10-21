package com.template.base.testsupport;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class MongoTestConfigurationTest {

  private MockEnvironment environment() {
    return new MockEnvironment()
        .withProperty("MONGO_URI", "mongodb://example.invalid/app")
        .withProperty("MONGO_DATABASE", "app")
        .withProperty("MONGO_URI_TEST", "mongodb://example.invalid/app")
        .withProperty("MONGO_DATABASE_TEST", "app-test")
        .withProperty("spring.mongodb.database", "app-test");
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
  void rejectsAnInactiveOrOverriddenTestProfile() {
    assertThatThrownBy(() -> new MongoTestConfiguration(environment()
        .withProperty("spring.mongodb.database", "app")))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("test profile");
  }

  @Test
  void alsoProtectsTheUriDatabaseWhenTheNormalDatabaseVariableIsAbsent() {
    MockEnvironment environment = environment();
    environment.setProperty("MONGO_DATABASE", "");
    assertThatThrownBy(() -> new MongoTestConfiguration(environment
        .withProperty("MONGO_DATABASE_TEST", "app")))
        .isInstanceOf(IllegalStateException.class);
  }
}
