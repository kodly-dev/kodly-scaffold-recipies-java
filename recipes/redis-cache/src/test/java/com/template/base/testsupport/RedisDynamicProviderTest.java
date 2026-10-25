package com.template.base.testsupport;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.data.redis.autoconfigure.DataRedisProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestContextManager;
import org.springframework.test.context.TestPropertySource;

class RedisDynamicProviderTest {
  @Test
  void lateDynamicPropertiesKeepTheBoundProviderIsolated() {
    assertThat(new TestContextManager(DynamicTest.class).getTestContext()
        .getApplicationContext()
        .getBean(DataRedisProperties.class)
        .getUrl())
        .isEqualTo("redis://localhost:6379/1");
  }

  @ContextConfiguration(classes = Fixture.class)
  @TestPropertySource(properties = {"REDIS_URL_TEST=redis://localhost:6379/1"})
  static class DynamicTest {
    @DynamicPropertySource
    static void applicationOverride(DynamicPropertyRegistry properties) {
      properties.add("spring.data.redis.url", () -> "redis://localhost:6379/0");
    }
  }

  @Configuration(proxyBeanMethods = false)
  @EnableConfigurationProperties(DataRedisProperties.class)
  static class Fixture {
  }
}
