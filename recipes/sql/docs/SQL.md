# SQL recipe

Adds **Spring Data JPA** and **Flyway** migrations (PostgreSQL defaults). Pick this **or** the `mongodb` recipe — not both.

## What you get

- `spring-boot-starter-data-jpa`
- `spring-boot-starter-flyway`
- PostgreSQL driver (`runtimeOnly`)
- Flyway's PostgreSQL database module (`runtimeOnly`)
- `BaseJpaConfig`
- Placeholder migration: `src/main/resources/db/migration/V1__init.sql`
- `docker-compose.sql.yml` — PostgreSQL on `localhost:5432`
- `BaseJpaIntegrationTest` — Testcontainers + `@ServiceConnection`
- `ddl-auto: validate` (schema owned by Flyway)
- `spring.jpa.open-in-view: false`

## Run PostgreSQL locally

```bash
docker compose -f docker-compose.sql.yml up -d
./gradlew bootRun
```

Credentials match `application.yml`: user/password `postgres` / `postgres`, database `template-base`.

## Tests

Use `./gradlew build` for portable compilation, formatting and unit tests. Start Docker before `./gradlew integrationTest`. The shared `BaseIntegrationTest` starts PostgreSQL through `SqlTestConfiguration` for both the skeleton and recipe tests. Flyway runs against that container, and the integration test verifies that migration `1` succeeded. Tests fail if Docker is unavailable.

## Add an entity + repository

```java
@Entity
@Table(name = "orders")
public class Order {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  // ...
}

public interface OrderRepository extends JpaRepository<Order, Long> {
}
```

Put repositories under the feature package: `myfeature/repository/`. Add matching DDL in a new Flyway script (e.g. `V2__create_orders.sql`) before relying on the entity.

## MySQL instead of Postgres

In `build.gradle`, swap the driver and Flyway database module:

```gradle
// runtimeOnly 'org.postgresql:postgresql'
// runtimeOnly 'org.flywaydb:flyway-database-postgresql'
runtimeOnly 'com.mysql:mysql-connector-j'
runtimeOnly 'org.flywaydb:flyway-mysql'
```

In `application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/template-base
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver
```

Update `docker-compose.sql.yml` (or add a MySQL service) and the test container in `SqlTestConfiguration` to match. The default recipe checks exercise PostgreSQL.

## Disable Flyway (local / tests)

```yaml
spring:
  flyway:
    enabled: false
  jpa:
    hibernate:
      ddl-auto: update   # only if you temporarily skip Flyway
```

Prefer keeping Flyway on and using Testcontainers (or Compose) for real runs.
