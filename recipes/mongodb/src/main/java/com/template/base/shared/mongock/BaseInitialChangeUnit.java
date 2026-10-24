package com.template.base.shared.mongock;

import com.mongodb.client.MongoDatabase;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;

/**
 * Placeholder Mongock change unit. Replace or add siblings for real migrations. Prefer raw
 * {@link MongoDatabase} operations over domain repositories.
 */
@ChangeUnit(id = "initial-placeholder", order = "001", author = "kodly")
public class BaseInitialChangeUnit {

  /** Placeholder for forward migrations such as creating indexes. */
  @Execution
  public void execute(MongoDatabase mongoDatabase) {
    // no-op placeholder — add indexes/collections here
  }

  /** Placeholder for reversing changes made by the forward migration. */
  @RollbackExecution
  public void rollback(MongoDatabase mongoDatabase) {
    // no-op placeholder
  }
}
