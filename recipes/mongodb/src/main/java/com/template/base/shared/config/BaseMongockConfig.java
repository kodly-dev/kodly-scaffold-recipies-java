package com.template.base.shared.config;

import com.mongodb.client.MongoClient;
import io.mongock.driver.mongodb.sync.v4.driver.MongoSync4Driver;
import io.mongock.runner.standalone.MongockStandalone;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;

/**
 * Wires Mongock via the standalone runner. Required on Spring Boot 4.x because
 * {@code mongock-springboot-v3} is bound to Boot 3 and silently skips migrations.
 */
@Configuration
@ConditionalOnProperty(name = "mongock.enabled", havingValue = "true", matchIfMissing = true)
public class BaseMongockConfig {

  private static final String MIGRATION_SCAN_PACKAGE = "com.template.base.shared.mongock";

  @Bean
  ApplicationListener<ApplicationReadyEvent> mongockRunner(MongoClient mongoClient,
      MongoDatabaseFactory databaseFactory) {
    String databaseName = databaseFactory.getMongoDatabase()
        .getName();
    MongoSync4Driver driver = MongoSync4Driver.withDefaultLock(mongoClient, databaseName);
    // The default recipe supports standalone MongoDB as well as replica sets.
    driver.disableTransaction();
    return event -> MongockStandalone.builder()
        .setDriver(driver)
        .addMigrationScanPackage(MIGRATION_SCAN_PACKAGE)
        .setTransactional(false)
        .buildRunner()
        .execute();
  }
}
