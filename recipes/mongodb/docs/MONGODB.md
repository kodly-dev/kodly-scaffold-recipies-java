# MongoDB recipe

Adds **Spring Data MongoDB** and **Mongock** migrations. Pick this **or** the `sql` recipe — not both.

## What you get

- `spring-boot-starter-data-mongodb`
- Mongock standalone runner + MongoDB sync driver (Boot 4-compatible)
- `BaseMongoConfig` / `BaseMongockConfig`
- Placeholder change unit under `shared.mongock`
- `docker-compose.mongodb.yml` — MongoDB on `localhost:27017`
- `BaseMongoIntegrationTest` — real read/write checks against the configured test database
- `src/test/resources/application-test.yml` — isolated `test` profile
- Default URI: `mongodb://localhost:27017/template-base`

## Kodly-managed database

The recipe uses Spring Boot 4's `spring.mongodb.uri` and `spring.mongodb.database`.
They read the Kodly secret names `MONGO_URI` and `MONGO_DATABASE` respectively.
Kodly resolves these secrets for the codebase when starting its server; no secret renaming
or checked-in credentials are needed. `MONGO_DATABASE` selects the codebase database even
when the URI contains another database or uses `authSource=admin`.

Outside Kodly, set both environment variables to use a remote database, or leave both
unset for the local Compose defaults. Store credentials in your runtime secret manager,
not source files. `BaseMongoEnvironmentTest` checks this configuration contract.

New Kodly backends receive only these Mongo defaults:

| Secret | Purpose |
| --- | --- |
| `MONGO_DATABASE` | Normal application database |
| `MONGO_DATABASE_TEST` | The normal name followed by `-test` |
| `MONGO_URI` | Kodly's provisioned MongoDB connection |
| `MONGO_URI_TEST` | The same connection, consumed only by the test profile |

Kodly grants the connection access to both databases. The explicit database property
overrides the database embedded in the URI. Application startup uses the normal values;
integration tests activate `test` and use the test values. The test configuration refuses
to use the normal database, and the read/write test removes only its own temporary record.

## Add to an existing codebase

Recipes can be added after the Java codebase has been created. From its root, run
`kodly scaffold list` to fetch the current published catalog, then
`kodly scaffold apply mongodb --dry-run`. Inspect the changes before running
`kodly scaffold apply mongodb`. Review the resulting diff: applying a recipe merges its
configuration and can replace matching files or settings. Preserve existing customizations.
Configure the four Mongo secrets and run `./gradlew build` before accepting the changes.

## Run MongoDB locally

```bash
docker compose -f docker-compose.mongodb.yml up -d
./gradlew bootRun
```

## Tests

`./gradlew build` runs compilation, quality checks, unit tests and integration tests.
The shared `BaseIntegrationTest` activates `test` for both application and recipe tests.
`MongoTestConfiguration` validates the database separation; Spring Boot connects using
the checked-in test YAML. Docker is not needed when using Kodly's database.

Outside Kodly, set `MONGO_URI_TEST` and `MONGO_DATABASE_TEST` in your environment or CI
secret manager. When using the optional local Compose database, the URI may be the same
as `MONGO_URI`, but the database must differ. `./gradlew test` runs unit tests alone;
`./gradlew integrationTest` runs integration tests alone. Neither substitutes for the full build.

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
