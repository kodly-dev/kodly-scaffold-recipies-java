package com.template.base.testsupport;

import static org.assertj.core.api.Assertions.assertThat;
import com.template.base.BaseApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.core.MongoTemplate;

/** Qualification only: deliberately uses neither BaseIntegrationTest nor the test profile. */
@SpringBootTest(classes = BaseApplication.class, properties = {
    "spring.mongodb.uri=${MONGO_URI}", "spring.mongodb.database=${MONGO_DATABASE}"})
class UntaggedMongoIsolationTest {
  @Autowired
  private MongoTemplate mongo;
  @Autowired
  private Environment environment;
  @Test
  void ordinarySpringTestsCannotSelectTheApplicationDatabase() {
    assertThat(mongo.getDb()
        .getName()).isEqualTo(environment.getRequiredProperty("MONGO_DATABASE_TEST"))
        .isNotEqualTo(environment.getRequiredProperty("MONGO_DATABASE"));
  }
}
