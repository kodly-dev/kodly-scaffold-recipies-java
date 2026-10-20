package com.template.base.testsupport;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.mongodb.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

/** Supplies an isolated MongoDB instance to every application integration test. */
@TestConfiguration(proxyBeanMethods = false)
public class MongoTestConfiguration {

  @Bean
  @ServiceConnection
  MongoDBContainer mongoContainer() {
    return new MongoDBContainer(DockerImageName.parse("mongo:7"));
  }
}
