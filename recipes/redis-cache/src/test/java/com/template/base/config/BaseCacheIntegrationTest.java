package com.template.base.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.template.base.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

class BaseCacheIntegrationTest extends BaseIntegrationTest {

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
