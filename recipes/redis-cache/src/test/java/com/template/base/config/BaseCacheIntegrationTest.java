package com.template.base.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.template.base.BaseIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

class BaseCacheIntegrationTest
    extends
      BaseIntegrationTest {

  @Autowired
  private StringRedisTemplate redisTemplate;

  @Test
  void contextLoadsAgainstTheConfiguredTestProvider() {
    assertThat(redisTemplate.getConnectionFactory()).isNotNull();
  }

  @Test
  void canReadAndWriteKeys() {
    String key = "smoke:" + UUID.randomUUID();
    try {
      redisTemplate.opsForValue()
          .set(key, "pong");
      assertThat(redisTemplate.opsForValue()
          .get(key)).isEqualTo("pong");
    } finally {
      redisTemplate.delete(key);
    }
  }
}
