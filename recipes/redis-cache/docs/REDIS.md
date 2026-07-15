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

`BaseCacheIntegrationTest` starts a Redis container automatically (skipped when Docker is unavailable).

Skeleton `*ApplicationTests` still expect Redis on localhost — start Compose before `./gradlew test`, or add `@ServiceConnection` Redis there as well.
