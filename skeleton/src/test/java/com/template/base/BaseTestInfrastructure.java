package com.template.base;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.util.ClassUtils;

/** Installs only the test infrastructure belonging to recipes present in this application. */
public class BaseTestInfrastructure implements ImportSelector {

  /** Enables isolation for every Spring test, including tests without the base class. */
  public static class Factory implements ContextCustomizerFactory {
    @Override
    public ContextCustomizer createContextCustomizer(Class<?> testClass,
        List<ContextConfigurationAttributes> attributes) {
      return new TestIsolation();
    }
  }

  private record TestIsolation() implements ContextCustomizer {
    @Override
    public void customizeContext(ConfigurableApplicationContext context,
        MergedContextConfiguration configuration) {
      String[] imports = new BaseTestInfrastructure().selectImports(null);
      if (imports.length > 0) {
        BaseTestEnvironment.capture(context.getEnvironment(), configuration);
      }
      for (String name : imports) {
        try {
          ClassUtils.forName(name, getClass().getClassLoader())
              .getConstructor(Environment.class)
              .newInstance(context.getEnvironment());
        } catch (InvocationTargetException invalid) {
          throw new IllegalStateException(invalid.getCause()
              .getMessage(), invalid.getCause());
        } catch (ReflectiveOperationException invalid) {
          throw new IllegalStateException("Cannot configure isolated recipe tests: " + name
              + ". This data recipe is incompatible with the skeleton test infrastructure. "
              + "Update the recipe to a version declaring requiresSkeleton, "
              + "following docs/TESTING.md.", invalid);
        }
      }
    }
  }

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
