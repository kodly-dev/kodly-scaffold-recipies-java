package com.template.base.shared.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.template.base.BaseIntegrationTest;
import java.util.UUID;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

class BaseMongoIntegrationTest
    extends
      BaseIntegrationTest {

  @Autowired
  private MongoTemplate mongoTemplate;

  @Autowired
  private Environment environment;

  @Test
  void contextLoadsAgainstTheConfiguredTestDatabase() {
    assertThat(mongoTemplate.getDb()
        .getName()).isEqualTo(environment.getRequiredProperty("MONGO_DATABASE_TEST"))
        .isNotEqualTo(environment.getProperty("MONGO_DATABASE"));
  }

  @Test
  void initialMigrationHasCompleted() {
    Document migration = mongoTemplate.getCollection("mongockChangeLog")
        .find(new Document("changeId", "initial-placeholder").append("state", "EXECUTED"))
        .first();
    assertThat(migration).isNotNull();
  }

  @Test
  void canReadAndWriteDocuments() {
    String id = "smoke-" + UUID.randomUUID();
    try {
      mongoTemplate.save(new Document("_id", id).append("message", "hello"), "smoke");
      Document found = mongoTemplate.findById(id, Document.class, "smoke");
      assertThat(found).isNotNull()
          .containsEntry("message", "hello");
    } finally {
      mongoTemplate.remove(Query.query(Criteria.where("_id")
          .is(id)), "smoke");
    }
  }
}
