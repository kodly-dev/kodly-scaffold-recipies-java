# Redis cache recipe

Adds **Spring Data Redis** with a local Docker Compose service and Testcontainers-backed integration tests.

## What you get

- `spring-boot-starter-data-redis`
- `BaseCacheConfig` placeholder
- `docker-compose.redis.yml` — Redis on `localhost:6379`
- `BaseCacheIntegrationTest` — Testcontainers + `@ServiceConnection`
- Defaults: `spring.data.redis.host=localhost`, `port=6379`

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

Use `./gradlew build` for portable compilation, formatting and unit tests. Start Docker before `./gradlew integrationTest`. The shared `BaseIntegrationTest` starts Redis through `RedisTestConfiguration` for both the skeleton and recipe tests. No Redis instance needs to run on localhost. Tests fail if Docker is unavailable.
