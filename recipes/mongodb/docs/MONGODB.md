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

## Run MongoDB locally

```bash
docker compose -f docker-compose.mongodb.yml up -d
./gradlew bootRun
```

## Tests

`BaseMongoIntegrationTest` starts a MongoDB container automatically (skipped when Docker is unavailable).

Skeleton `*ApplicationTests` still expect Mongo on localhost — start Compose before `./gradlew test`, or wire `@ServiceConnection` there as well.

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
