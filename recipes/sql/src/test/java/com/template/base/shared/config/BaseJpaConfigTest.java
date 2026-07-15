package com.template.base.shared.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class BaseJpaConfigTest {

  @Test
  void recipeMainSourcesCompileAndInstantiate() {
    assertNotNull(new BaseJpaConfig());
  }
}
