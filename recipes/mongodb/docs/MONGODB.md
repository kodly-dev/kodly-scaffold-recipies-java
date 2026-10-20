# MongoDB recipe

Adds **Spring Data MongoDB** and **Mongock** migrations. Pick this **or** the `sql` recipe — not both.

## What you get

- `spring-boot-starter-data-mongodb`
- Mongock standalone runner + MongoDB sync driver (Boot 4-compatible)
- `BaseMongoConfig` / `BaseMongockConfig`
- Placeholder change unit under `shared.mongock`
- `docker-compose.mongodb.yml` — MongoDB on `localhost:27017`
- `BaseMongoIntegrationTest` — Testcontainers + `@ServiceConnection`
- Default URI: `mongodb://localhost:27017/template-base`

## Kodly-managed database

The recipe uses Spring Boot 4's `spring.mongodb.uri` and `spring.mongodb.database`.
They read the existing Kodly secret names `MONGO_URI` and `MONGO_DATABASE` respectively.
Kodly resolves these secrets for the codebase when starting its server; no secret renaming
or checked-in credentials are needed. `MONGO_DATABASE` selects the codebase database even
when the URI contains another database or uses `authSource=admin`.

Outside Kodly, set both environment variables to use a remote database, or leave both
unset for the local Compose defaults. Store credentials in your runtime secret manager,
not source files. `BaseMongoEnvironmentTest` checks this configuration contract without
Testcontainers replacing it.

## Add to an existing codebase

Recipes can be added after the Java codebase has been created. From its root, run
`kodly scaffold list` to fetch the current published catalog, then
`kodly scaffold apply mongodb --dry-run`. Inspect the changes before running
`kodly scaffold apply mongodb`. Review the resulting diff: applying a recipe merges its
configuration and can replace matching files or settings. Preserve existing customizations.
Run `./gradlew build`, the configured lint and duplicate checks, and
`./gradlew integrationTest` with Docker available before accepting the changes.

## Run MongoDB locally

```bash
docker compose -f docker-compose.mongodb.yml up -d
./gradlew bootRun
```

## Tests

Use `./gradlew build` for portable compilation, formatting and unit tests. Start Docker before `./gradlew integrationTest`. The shared `BaseIntegrationTest` starts MongoDB through `MongoTestConfiguration` for both the skeleton and recipe tests. No database needs to run on localhost. Tests fail if Docker is unavailable.

## Add a document + repository

```java
@Document("orders")
public class Order {
  @Id
  private String id;
  // ...
}

public interface OrderRepository extends MongoRepository<Order, String> {
}
```

Put repositories under the feature package: `myfeature/repository/`.

## Add a Mongock migration

1. Create a class in `com.<your-group>.shared.mongock` (same package scanned by `BaseMongockConfig`).
2. Annotate with `@ChangeUnit(id = "...", order = "...", author = "...")`.
3. Implement `@Execution` / `@RollbackExecution` using `MongoDatabase` (not domain repositories).

Never edit a change unit that has already run in a shared environment — add a new ordered unit instead.

## Disable Mongock (local / tests)

```yaml
mongock:
  enabled: false
```

Or `@SpringBootTest(properties = "mongock.enabled=false")` when the context should not talk to Mongo.
