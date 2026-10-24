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
- `BaseJpaIntegrationTest` — real Flyway and database assertions against your test provider
- `ddl-auto: validate` (schema owned by Flyway)
- `spring.jpa.open-in-view: false`

## Run PostgreSQL locally

```bash
docker compose -f docker-compose.sql.yml up -d
./gradlew bootRun
```

Credentials match `application.yml`: user/password `postgres` / `postgres`, database `template-base`.

## Tests

Configure the application provider with `SQL_URL`, `SQL_USERNAME` and `SQL_PASSWORD`.
Provide a separate test database through `SQL_URL_TEST`, `SQL_USERNAME_TEST` and
`SQL_PASSWORD_TEST` in project secrets or your CI environment. PostgreSQL and MySQL
JDBC URLs are supported. Credentials and connection options alone do not make a
database separate: the host, port and database identify it.

`./gradlew build` runs compilation, formatting, unit tests and the real database
integration tests. `./gradlew integrationTest` runs the latter explicitly. Both use
the configured external provider; no Docker is started. Missing or unsafe test
settings fail before the application initializes. Flyway runs against the test
database, and the integration test verifies that migration `1` succeeded. Local
Compose is an optional provider; create a separate test database before testing.

The test guard pins the URL and credentials for both the generic datasource and
Hikari's later property binding. It also pins Flyway's separate migration
connection to the TEST provider. The normal datasource, Hikari URL and Flyway URL
are all checked for collisions before application beans initialize. Pool tuning
such as maximum pool size is retained.

The supplied guard supports the recipe's JDBC-backed Hikari pool. It refuses
JNDI/XA, alternate datasource types/classes, Hikari credential providers,
datasource property maps, catalog/schema overrides and pool initialization or
validation SQL. Flyway JDBC property maps and initialization SQL are refused too:
these settings can select another target independently of the TEST JDBC URL.
For MySQL, explicit Flyway schemas/default-schema are also refused; use the
database named in the TEST URL instead.
The error names the setting to review. Put necessary driver connection options in
the TEST JDBC URL; custom datasource/migration beans need their own isolation
checks. Tests inspect actual Boot-bound Hikari and Flyway datasources without
opening connections, in addition to the real provider integration checks.

Name the database and provider in the URL path and authority. JDBC query selectors
such as PostgreSQL `PGDBNAME`/`PGHOST`/`PGPORT` and MySQL `DBNAME` are refused,
including aliases, case variants and encoded names. Configuration transforms,
alternate socket factories and connection initialization SQL are also refused.
These checks cover the TEST URL and normal datasource/Hikari/Flyway URLs.
JDBC URLs containing fragments (`#`) are refused because driver parsing differs
from ordinary URI parsing. Normal options such as TLS and connection timeouts
remain supported. The guard tests run against the installed PostgreSQL or MySQL
driver to verify these rules without opening a connection.

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

Configure `SQL_URL` and `SQL_URL_TEST` with MySQL JDBC URLs naming separate databases,
and set their corresponding username/password variables. Update the optional
`docker-compose.sql.yml` if you use it locally. No changes to `SqlTestConfiguration`
or additional PostgreSQL test dependency are needed: the bound datasource/Flyway
guard tests use the installed driver and do not open database connections.
Run `./gradlew build` with the MySQL test provider to exercise real migrations too.

The recipe repository's normal `check` runs the bound guard suite again with a
MySQL-only classpath through `:sql:testMysql`. Additional recipe qualification
generates fresh projects for both the default PostgreSQL dependencies and this
MySQL substitution. See the repository's
`scripts/test-sql-drivers.mjs` instructions in its README.

## Disable Flyway (local / tests)

```yaml
spring:
  flyway:
    enabled: false
  jpa:
    hibernate:
      ddl-auto: update   # only if you temporarily skip Flyway
```

Keep Flyway enabled and use your configured external provider for real runs.
