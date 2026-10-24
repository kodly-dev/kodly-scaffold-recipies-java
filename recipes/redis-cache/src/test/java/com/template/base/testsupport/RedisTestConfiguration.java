package com.template.base.testsupport;

import com.template.base.BaseTestEnvironment;
import java.net.URI;
import java.util.Map;
import java.util.List;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

/** Redis tests always use an explicitly separate external server or database. */
@TestConfiguration(proxyBeanMethods = false)
public class RedisTestConfiguration {

  /** Validates the isolated external provider before application initialization. */
  public RedisTestConfiguration(Environment environment) {
    String testUrl = environment.getProperty("REDIS_URL_TEST");
    if (!StringUtils.hasText(testUrl)) {
      throw new IllegalStateException(
          "Set REDIS_URL_TEST to an isolated external test provider before running tests.");
    }
    Environment application = BaseTestEnvironment.application(environment);
    rejectAlternateProviders(application);
    rejectAlternateProviders(environment);
    if (database(testUrl).equals(applicationDatabase(application))) {
      throw new IllegalStateException(
          "REDIS_URL_TEST must select a separate server or database "
              + "from the application Redis configuration.");
    }
    URI uri = URI.create(testUrl);
    ((ConfigurableEnvironment) environment).getPropertySources()
        .addFirst(new MapPropertySource("kodlyRedisTests", Map.of(
            "spring.data.redis.url", testUrl, "spring.data.redis.database", databaseNumber(uri))));
  }

  private static String applicationDatabase(Environment application) {
    String url = application.getProperty("spring.data.redis.url");
    if (StringUtils.hasText(url)) {
      return database(url);
    }
    if (url != null || application.containsProperty("spring.data.redis.host")
        || application.containsProperty("spring.data.redis.port")
        || application.containsProperty("spring.data.redis.database")) {
      return BaseTestEnvironment
          .providerHost(application.getProperty("spring.data.redis.host", "localhost"))
          + ":" + application.getProperty("spring.data.redis.port", Integer.class, 6379)
          + "/" + application.getProperty("spring.data.redis.database", Integer.class, 0);
    }
    return database(application.getProperty("REDIS_URL", "redis://localhost:6379/0"));
  }

  private static String database(String url) {
    URI uri = URI.create(url);
    if (!"redis".equals(uri.getScheme()) && !"rediss".equals(uri.getScheme())
        || uri.getHost() == null) {
      throw new IllegalStateException("Use a redis:// or rediss:// URL for Redis tests.");
    }
    return BaseTestEnvironment.providerHost(uri.getHost()) + ":"
        + (uri.getPort() == -1 ? 6379 : uri.getPort()) + "/"
        + databaseNumber(uri);
  }

  private static void rejectAlternateProviders(Environment environment) {
    for (String selector : List.of("spring.data.redis.cluster", "spring.data.redis.sentinel")) {
      if (!Binder.get(environment)
          .bind(selector, Bindable.mapOf(String.class, Object.class))
          .orElse(Map.of())
          .isEmpty()) {
        throw new IllegalStateException("Cannot isolate Redis tests with " + selector
            + "; use REDIS_URL_TEST without alternate connection selectors.");
      }
    }
  }

  private static int databaseNumber(URI uri) {
    String path = uri.getPath();
    if (path == null || path.isEmpty() || path.equals("/")) {
      return 0;
    }
    if (!path.matches("/[0-9]{1,6}")) {
      throw new IllegalStateException("Redis test URLs must select a valid database number.");
    }
    return Integer.parseInt(path.substring(1));
  }
}
