package com.template.base.testsupport;

import com.redis.testcontainers.RedisContainer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.utility.DockerImageName;

/** Supplies isolated Redis connections to all cache-enabled application tests. */
@TestConfiguration(proxyBeanMethods = false)
public class RedisTestConfiguration {

  @Bean
  @ServiceConnection
  RedisContainer redis() {
    return new RedisContainer(DockerImageName.parse("redis:8.10.2-alpine"));
  }
}
