# Folder and naming conventions

This scaffold uses **package-by-feature** (vertical slices) with plain Spring type names. Copy `greeting/` when adding a new feature.

## Principles

1. **One feature = one top-level package** under the application root (e.g. `com.template.base.greeting`).
2. **`shared/` is only for cross-cutting code** used by multiple features (config, filters, exception handling, shared enums).
3. Prefer **role packages inside a feature** (`controller`, `service`, `dto`, `model`) over hexagonal layers (`application`, `infrastructure`, `domain`).

## Feature package layout

```
myfeature/
  controller/     # HTTP adapters
  service/        # interfaces
  service/impl/   # @Service implementations
  dto/            # request/response bodies
  model/          # domain / value objects
  enums/          # feature-specific enums (optional)
  repository/     # Spring Data repos (MongoRepository / JpaRepository) when a DB recipe is applied
```

Optional DB recipes add migrations outside the feature package:

- **sql** — Flyway scripts under `src/main/resources/db/migration/`
- **mongodb** — Mongock `@ChangeUnit` classes under `shared/mongock/`

## Type naming

| Role | Pattern | Example |
|------|---------|---------|
| Controller | `XxxController` | `BaseGreetingController` |
| Service interface | `XxxService` | `GreetingService` |
| Service impl | `XxxServiceImpl` | `GreetingServiceImpl` |
| Request body | `XxxRequestDto` | `CreateGreetingRequestDto` |
| Response body | `XxxResponseDto` | `GreetingResponseDto` |
| Model | `Xxx` | `Greeting` |
| Enum | `XxxEnum` | `BaseErrorCodeEnum` |
| Config | `XxxConfig` | `BaseWebConfig` |
| Filter | `XxxFilter` | `BaseCorrelationIdFilter` |
| Exception handler | `XxxExceptionHandler` / `XxxGlobalExceptionHandler` | `BaseGlobalExceptionHandler` |

## Shared kernel layout

```
shared/
  enums/       # cross-cutting enums (e.g. BaseErrorCodeEnum)
  config/      # Web, i18n, observability, etc.
  filter/      # servlet filters
  exception/   # global / shared exception handlers
```

## How to add a new feature

1. Create a package next to `greeting/` (e.g. `orders/`).
2. Add `controller/`, `service/`, `service/impl/`, `dto/`, and `model/` as needed.
3. Define a service interface and `@Service` implementation.
4. Expose HTTP via a controller; use `@Valid` request DTOs for input.
5. Keep feature-specific types inside the feature package; only put truly shared types in `shared/`.

## What not to do

- Do not introduce hexagonal folders (`infrastructure/`, `application/`, `domain/`) for new features.
- Do not dump feature logic into `shared/`.
- Do not build “god” services that span unrelated features.

## Kodly scaffold note

On `kodly scaffold init`, class prefixes like `Base*` are rewritten to your project prefix. Keep the **suffix conventions** (`Controller`, `Service`, `ServiceImpl`, `RequestDto`, `Enum`, `Config`, `Filter`) intact so generated apps stay consistent.
