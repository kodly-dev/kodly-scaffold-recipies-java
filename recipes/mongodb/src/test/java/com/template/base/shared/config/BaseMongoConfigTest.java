package com.template.base.shared.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.template.base.shared.mongock.BaseInitialChangeUnit;
import org.junit.jupiter.api.Test;

class BaseMongoConfigTest {

  @Test
  void recipeMainSourcesCompileAndInstantiate() {
    assertNotNull(new BaseMongoConfig());
    assertNotNull(new BaseMongockConfig());
    assertNotNull(new BaseInitialChangeUnit());
  }
}
