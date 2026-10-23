# Redis cache recipe

Adds **Spring Data Redis** with an external provider, optional local Compose service and isolated integration tests.

## What you get

- `spring-boot-starter-data-redis`
- `BaseCacheConfig` placeholder
- `docker-compose.redis.yml` — Redis on `localhost:6379`
- `BaseCacheIntegrationTest` — verifies a temporary key against your test provider
- `REDIS_URL` configures the application provider; default `redis://localhost:6379/0`

## Run Redis locally

```bash
docker compose -f docker-compose.redis.yml up -d
./gradlew bootRun
```

Stack with SQL or MongoDB:

```bash
docker compose -f docker-compose.sql.yml -f docker-compose.redis.yml up -d
# or
docker compose -f docker-compose.mongodb.yml -f docker-compose.redis.yml up -d
```

## Tests

Set `REDIS_URL` and `REDIS_URL_TEST` through project secrets or your CI environment.
Use `redis://` or `rediss://` URLs, with credentials when your provider requires them.
Tests must select a different server or logical database, for example database `1`
when the application uses `0`. Providers supporting only database `0` need a separate
test instance. Changing credentials or TLS alone does not isolate the data.

`./gradlew build` includes the real Redis integration tests;
`./gradlew integrationTest` runs them explicitly. Tests create a unique temporary key
and remove it afterward. Missing or unsafe test-provider settings fail before the
application initializes. No Docker is started by the tests; Compose is optional.
