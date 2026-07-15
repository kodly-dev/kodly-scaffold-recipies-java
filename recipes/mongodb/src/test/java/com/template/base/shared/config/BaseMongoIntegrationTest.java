package com.template.base.shared.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.template.base.BaseApplication;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(classes = BaseApplication.class)
@Testcontainers(disabledWithoutDocker = true)
class BaseMongoIntegrationTest {

  @Container
  @ServiceConnection
  static MongoDBContainer mongo = new MongoDBContainer(DockerImageName.parse("mongo:7"));

  @Autowired
  private MongoTemplate mongoTemplate;

  @Test
  void contextLoadsAgainstMongoContainer() {
    assertThat(mongoTemplate.getDb()
      .getName()).isNotBlank();
  }

  @Test
  void canReadAndWriteDocuments() {
    String id = "smoke-" + UUID.randomUUID();
    mongoTemplate.save(Map.of("_id", id, "message", "hello"), "smoke");

    @SuppressWarnings("unchecked")
    Map<String, Object> found = mongoTemplate.findById(id, Map.class, "smoke");

    assertThat(found).isNotNull()
      .containsEntry("message", "hello");
  }
}
