package com.template.base.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.redis.testcontainers.RedisContainer;
import com.template.base.BaseApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(classes = BaseApplication.class)
@Testcontainers(disabledWithoutDocker = true)
class BaseCacheIntegrationTest {

  @Container
  @ServiceConnection
  static RedisContainer redis = new RedisContainer(DockerImageName.parse("redis:7-alpine"));

  @Autowired
  private StringRedisTemplate redisTemplate;

  @Test
  void contextLoadsAgainstRedisContainer() {
    assertThat(redisTemplate.getConnectionFactory()).isNotNull();
  }

  @Test
  void canReadAndWriteKeys() {
    redisTemplate.opsForValue()
      .set("smoke:ping", "pong");

    assertThat(redisTemplate.opsForValue()
      .get("smoke:ping")).isEqualTo("pong");
  }
}
