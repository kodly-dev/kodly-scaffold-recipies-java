package com.template.base.testsupport;

import com.mongodb.ConnectionString;
import com.template.base.BaseTestEnvironment;
import java.util.Map;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

/** Rejects unsafe configuration before application tests can write to MongoDB. */
@TestConfiguration(proxyBeanMethods = false)
public class MongoTestConfiguration {

  /** Validates the selected test database before application initialization. */
  public MongoTestConfiguration(Environment environment) {
    if (!StringUtils.hasText(environment.getProperty("MONGO_URI_TEST"))) {
      throw new IllegalStateException(
          "Set MONGO_URI_TEST for the test profile before running integration tests.");
    }
    String testDatabase = environment.getProperty("MONGO_DATABASE_TEST");
    Environment application = BaseTestEnvironment.application(environment);
    String normal = application.getProperty("spring.mongodb.database",
        application.getProperty("MONGO_DATABASE", "template-base"));
    if (!StringUtils.hasText(normal)) {
      normal = "template-base";
    }
    if (!StringUtils.hasText(testDatabase) || testDatabase.equals(normal)
        || testDatabase.equals(uriDatabase(application))) {
      throw new IllegalStateException(
          "MONGO_DATABASE_TEST must select a separate database "
              + "from the application MongoDB configuration.");
    }
    new ConnectionString(environment.getRequiredProperty("MONGO_URI_TEST"));
    ((ConfigurableEnvironment) environment).getPropertySources()
        .addFirst(new MapPropertySource("kodlyMongoTests", Map.of(
            "spring.mongodb.uri", environment.getRequiredProperty("MONGO_URI_TEST"),
            "spring.mongodb.database", testDatabase)));
  }

  private String uriDatabase(Environment environment) {
    try {
      String defaultUri = "mongodb://localhost:27017/template-base";
      String uri = environment.getProperty("spring.mongodb.uri",
          environment.getProperty("MONGO_URI", defaultUri));
      String uriDatabase = new ConnectionString(uri).getDatabase();
      return StringUtils.hasText(uriDatabase) ? uriDatabase : "template-base";
    } catch (IllegalArgumentException invalidUri) {
      throw new IllegalStateException("Check MONGO_URI before running integration tests.");
    }
  }
}
