package com.template.base.testsupport;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.mongodb.autoconfigure.MongoProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestContextManager;
import org.springframework.test.context.TestPropertySource;

class MongoDynamicProviderTest {
  @Test
  void lateDynamicPropertiesKeepTheBoundProviderIsolated() {
    assertThat(new TestContextManager(DynamicTest.class).getTestContext()
        .getApplicationContext()
        .getBean(MongoProperties.class)
        .getDatabase())
        .isEqualTo("application-test");
  }

  @ContextConfiguration(classes = Fixture.class)
  @TestPropertySource(properties = {"MONGO_URI_TEST=mongodb://localhost:27017/",
      "MONGO_DATABASE_TEST=application-test"})
  static class DynamicTest {
    @DynamicPropertySource
    static void applicationOverride(DynamicPropertyRegistry properties) {
      properties.add("spring.mongodb.database", () -> "application");
    }
  }

  @Configuration(proxyBeanMethods = false)
  @EnableConfigurationProperties(MongoProperties.class)
  static class Fixture {
  }
}
