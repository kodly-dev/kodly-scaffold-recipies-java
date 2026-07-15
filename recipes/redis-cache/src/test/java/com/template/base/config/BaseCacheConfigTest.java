package com.template.base.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class BaseCacheConfigTest {

  @Test
  void recipeMainSourcesCompileAndInstantiate() {
    assertNotNull(new BaseCacheConfig());
  }
}
