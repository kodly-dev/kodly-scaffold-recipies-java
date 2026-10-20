package com.template.base.shared.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.template.base.BaseIntegrationTest;
import java.util.UUID;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;

class BaseMongoIntegrationTest extends BaseIntegrationTest {

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
    mongoTemplate.save(new Document("_id", id).append("message", "hello"), "smoke");

    Document found = mongoTemplate.findById(id, Document.class, "smoke");

    assertThat(found).isNotNull()
        .containsEntry("message", "hello");
  }
}
