package com.template.base;

import java.util.List;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ClassUtils;

/** Installs only the test infrastructure belonging to recipes present in this application. */
public class BaseTestInfrastructure implements ImportSelector {

  @Override
  public String[] selectImports(AnnotationMetadata metadata) {
    String packageName = BaseApplication.class.getPackageName()
        + ".testsupport.";
    return List.of("MongoTestConfiguration", "SqlTestConfiguration", "RedisTestConfiguration")
        .stream()
        .map(name -> packageName + name)
        .filter(name -> ClassUtils.isPresent(name, getClass().getClassLoader()))
        .toArray(String[]::new);
  }
}
