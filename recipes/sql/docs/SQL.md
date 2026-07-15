# SQL recipe

Adds **Spring Data JPA** and **Flyway** migrations (PostgreSQL defaults). Pick this **or** the `mongodb` recipe — not both.

## What you get

- `spring-boot-starter-data-jpa`
- `spring-boot-starter-flyway`
- PostgreSQL driver (`runtimeOnly`)
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

`BaseJpaIntegrationTest` starts a PostgreSQL container automatically (skipped when Docker is unavailable). Flyway runs against that container.

Skeleton `*ApplicationTests` still expect Postgres on localhost — start Compose before `./gradlew test`, or wire `@ServiceConnection` there as well.

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

In `build.gradle`, swap the driver:

```gradle
// runtimeOnly 'org.postgresql:postgresql'
runtimeOnly 'com.mysql:mysql-connector-j'
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

Update `docker-compose.sql.yml` (or add a MySQL service) to match.

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
