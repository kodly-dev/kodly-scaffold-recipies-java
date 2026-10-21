package com.template.base.testsupport;

import com.mongodb.ConnectionString;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.core.env.Environment;
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
    if (!StringUtils.hasText(testDatabase) || testDatabase.equals(normalDatabase(environment))) {
      throw new IllegalStateException(
          "MONGO_DATABASE_TEST must select a separate database from MONGO_DATABASE.");
    }
    if (!testDatabase.equals(environment.getProperty("spring.mongodb.database"))) {
      throw new IllegalStateException(
          "Activate the test profile and use MONGO_DATABASE_TEST for spring.mongodb.database.");
    }
  }

  private String normalDatabase(Environment environment) {
    String database = environment.getProperty("MONGO_DATABASE");
    if (StringUtils.hasText(database)) {
      return database;
    }
    try {
      String uriDatabase = new ConnectionString(environment.getProperty("MONGO_URI",
          "mongodb://localhost:27017/template-base")).getDatabase();
      return StringUtils.hasText(uriDatabase) ? uriDatabase : "template-base";
    } catch (IllegalArgumentException invalidUri) {
      throw new IllegalStateException("Check MONGO_URI before running integration tests.");
    }
  }
}
