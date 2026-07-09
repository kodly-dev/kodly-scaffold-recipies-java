package com.template.base.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class BaseActuatorConfigTest {

  public BaseActuatorConfigTest() {}

  @Test
  void recipeMainSourcesCompileAndInstantiate() {
    assertNotNull(new BaseActuatorConfig());
  }
}
