# Kodly Scaffold Recipes

Community recipe monorepo for the Kodly deterministic scaffolder (`kodly scaffold`).

## Contents

| Path | Description |
|------|-------------|
| `skeleton/` | Fetchable Spring Boot 4.1 production web skeleton (Java 27, vertical-slice layout) |
| `recipes/redis-cache/` | Redis / Spring Data Redis starter + Compose + Testcontainers (optional add-on) |
| `recipes/mongodb/` | Spring Data MongoDB + Mongock + Compose + Testcontainers (optional; mutually exclusive with `sql`) |
| `recipes/sql/` | Spring Data JPA + Flyway + Compose + Testcontainers, Postgres defaults (optional; mutually exclusive with `mongodb`) |

## Base skeleton (`skeleton@1.0.5`)

- **Spring Boot** 4.1.1
- **Java** 27 (via [jenv](https://github.com/jenv/jenv))
- **Gradle** 9.8.2, pinned by the included wrapper with the official distribution checksum.
- **Starters** web, validation, actuator; plus springdoc OpenAPI and Micrometer Tracing (Brave)
- **Package** `com.template.base` with `Base*` class prefix
- **Layout** vertical slice / package-by-feature: `shared/` (cross-cutting) + sample `greeting/` feature
- **Naming** layman suffixes: `XxxController`, `XxxService` / `XxxServiceImpl`, `XxxRequestDto`, model types, `XxxEnum`, `XxxConfig`, `XxxFilter`
- **Conventions** ship with the scaffold: [`skeleton/docs/FOLDER_AND_NAMING_CONVENTIONS.md`](skeleton/docs/FOLDER_AND_NAMING_CONVENTIONS.md)
- **Markers** required by scaffolder:
  - `// KODLY_RECIPE_PLUGINS_MARKER`
  - `// KODLY_RECIPE_DEPENDENCIES_MARKER`

Sample packages:

```
greeting/                         # feature vertical slice
  controller/   # BaseGreetingController
  service/      # GreetingService (interface)
  service/impl/ # GreetingServiceImpl
  dto/          # CreateGreetingRequestDto
  model/        # Greeting

shared/                           # cross-cutting only
  enums/        # BaseErrorCodeEnum
  config/       # BaseWebConfig, BaseI18nConfig, BaseObservabilityConfig
  filter/       # BaseCorrelationIdFilter
  exception/    # BaseGlobalExceptionHandler
```
Built-in production foundations:

- RFC 7807 Problem Details + global exception handler
- i18n (`MessageSource`, `Accept-Language`, `messages*.properties`)
- Bean validation wired to Problem Details
- Correlation ID (`X-Request-Id` → MDC)
- CORS defaults, graceful shutdown, `dev` / `prod` profiles
- Actuator (`/actuator/health`, info, metrics)
- OpenAPI / Swagger UI (`/v3/api-docs`, `/swagger-ui.html`)
- Micrometer Tracing (Brave)

Endpoints out of the box:

- `GET /` → hello message
- `POST /greetings` → personalized greeting (`@Valid` body)
- `GET /actuator/health` → Actuator health

## Java 27 setup (jenv)

This project expects **Java 27** on your machine, managed with jenv. A `.java-version` file is included at the repo root and in `skeleton/`.

```bash
# 1. Install JDK 27 (macOS example)
brew install openjdk@27

# 2. Register it with jenv
jenv add "$(brew --prefix openjdk@27)/libexec/openjdk.jdk/Contents/Home"

# 3. Enable jenv shims (add to ~/.zshrc if not already)
eval "$(jenv init -)"

# 4. Use Java 27 in this repo
cd kodly-scaffold-recipes
jenv local 27.0.0

# 5. Verify
java -version
```

Gradle uses the JDK from your environment (`JAVA_HOME` set by jenv) together with the Java 27 toolchain declared in `build.gradle`.

## Quick test with Kodly CLI

### 1. Tag skeleton and recipes (one-time per clone)

The scaffolder fetches templates by git tag name:

```bash
chmod +x scripts/tag-recipes.sh
./scripts/tag-recipes.sh
```

This reads each `manifest.json` and creates its versioned tag, such as `skeleton@1.0.5` and `recipes/redis-cache@1.0.4`. There is no separate version list in the script. Commit and review the templates before tagging; existing tags are never moved. Push the reviewed tags to make them available to the CLI/API.

### 2. Point CLI at this repo

```bash
export KODLY_RECIPES_REPO_URL="file:///absolute/path/to/kodly-scaffold-recipes"
```

### 3. Initialize a project

```bash
mkdir -p /tmp/my-app
kodly scaffold init --group com.rental.app --name rental-app --path /tmp/my-app
cd /tmp/my-app
```

Package (`com.rental.app`), class prefix (`RentalApp`), and Gradle project name (`rental-app`) are rewritten from the template tokens (`com.template.base`, `Base*`, `template-base`).

### 4. Apply optional recipes

Recipes can be added to an existing compatible codebase at any time; they are not
limited to project creation. Discover what is currently published instead of
maintaining a separate list in agent prompts or application code:

```bash
kodly scaffold list
kodly scaffold apply --help
```

Read the selected recipe's manifest and documentation from its published Git tag
to understand compatibility and required services. Then, from the existing
codebase root, preview and apply that selection:

```bash
kodly scaffold apply <recipe-from-catalog> --dry-run
kodly scaffold apply <recipe-from-catalog>
```

The CLI merges Gradle dependencies and application YAML and copies recipe files.
Matching files/settings can be replaced, so start from a saved revision, inspect
the dry run, preserve customizations and review the final diff. Do not recreate the
codebase to add a capability. Check the runtime's existing secret names against the
recipe placeholders without displaying their values. Installing source does not
itself provision every external service. Run the configured build, lint,
duplicate checks and relevant integration tests before accepting the change.

### 5. Local infra + build

Each infra recipe ships a Compose file and Testcontainers integration tests:

```bash
# Examples (use the file(s) for recipes you applied):
docker compose -f docker-compose.sql.yml up -d
# docker compose -f docker-compose.mongodb.yml up -d
# docker compose -f docker-compose.redis.yml up -d

./gradlew compileJava
./gradlew test
./gradlew integrationTest
./gradlew bootRun
```

Compose is for `bootRun`. `build` includes portable unit tests and runs without Docker, including inside harness sandboxes. `integrationTest` retains the complete application assertions and discovers installed recipes through Spring service connections; it needs Docker for SQL, MongoDB or Redis and never depends on localhost database ports. CI and release qualification run both suites. Integration failures are not skipped or treated as passing builds.

### Version selection

Use Kodly CLI 1.0.18 or newer with scaffolder 1.0.2 or newer. Omitted versions
select the latest published stable tag; `--skeleton-version <version>` and
`recipe@<version>` remain available for reproducible manual runs. Publishing a
new tag affects new scaffold requests only. Existing projects are not upgraded.
For local development, commit and tag the local clone, then use its `file://` URL.

## Retire a recipe

`kodly scaffold list` shows the latest stable published tag for every recipe. Unversioned init/create/apply use the same catalog; a manifest change is unavailable until its tag is published. To fully retire a recipe:

1. Remove `recipes/<name>/` from `main` (and drop it from `settings.gradle`).
2. Delete its remote tags so apply also fails:

```bash
chmod +x scripts/untag-recipes.sh
./scripts/untag-recipes.sh recipes/my-recipe@1.0.0
# or: ./scripts/untag-recipes.sh my-recipe@1.0.0
```

## Automated smoke test

From this repo (requires `kodly` on PATH and tagged recipes):

```bash
chmod +x scripts/test-scaffold.sh
./scripts/test-scaffold.sh /tmp/kodly-scaffold-test
```

## Contributor DX (compilable recipes)

Recipes are **full Gradle modules** in this monorepo — not just loose files. Each recipe:

- Has its own `build.gradle` and `src/test/` (for contributor verification)
- Uses `compileOnly project(':core-skeleton')` so recipe code compiles against the base skeleton APIs
- Ships a `manifest.json` that tells the scaffolder **exactly** which files to extract into a target project

Scaffold copies only paths listed in `manifest.includes` (plus YAML merge / Gradle anchors). Recipe `build.gradle` stays DX-only; integration tests and Compose files are listed in `includes` so they ship into target projects.

```bash
# Compile and test everything from repo root
jenv local 27.0.0
./gradlew check integrationTest
```

Formatting uses [Spotless](https://github.com/diffplug/spotless) with the **Eclipse JDT** formatter (2-space indent, 100-column wrap). Edit rules in [`skeleton/spotless/eclipse-java-formatter.xml`](skeleton/spotless/eclipse-java-formatter.xml). The same profile is wired for Cursor/VS Code via `.vscode/settings.json` (shipped in the skeleton so scaffolded apps get it too).

```bash
./gradlew spotlessApply   # rewrite sources to the Eclipse profile
./gradlew spotlessCheck   # fail if formatting drifts (also runs via check)
```

| Module | Gradle task |
|--------|-------------|
| Base skeleton | `:core-skeleton:bootRun` / `:core-skeleton:check` |
| Redis recipe | `:redis-cache:check` |
| MongoDB + Mongock | `:mongodb:check` |
| SQL + Flyway | `:sql:check` |

## Recipe manifest contract

Each recipe includes a root `manifest.json`:

```json
{
  "name": "redis-cache",
  "version": "1.0.0",
  "description": "...",
  "includes": [
    "src/main/java/com/template/base/config/BaseCacheConfig.java"
  ],
  "config": {
    "application": "application.yml"
  },
  "anchors": {
    "gradle": {
      "dependencies": [
        "implementation 'org.springframework.boot:spring-boot-starter-data-redis'"
      ]
    }
  }
}
```

| Field | Purpose |
|-------|---------|
| `includes` | **Required.** Files copied into the target project (with token mapping) |
| `config.application` | Optional YAML file to deep-merge into `application.yml` |
| `anchors.gradle.dependencies` | String or string array injected at `// KODLY_RECIPE_DEPENDENCIES_MARKER` |
| `anchors.gradle.plugins` | Optional string or string array injected at `// KODLY_RECIPE_PLUGINS_MARKER` |

Recipe Java sources use `com.template.base` and `Base*` names — the scaffolder rewrites them to match the target project. Recipe `build.gradle` stays in this repo only; tests listed in `includes` are scaffolded.

## Verify skeleton compiles

```bash
jenv local 27.0.0
./gradlew :core-skeleton:bootRun
```

## Repository-owned checks

Generated projects run `sh gradlew build`: unit/contract and integration tests, Spotless, Checkstyle
and PMD copy/paste detection are declared in their own build and checked-in configuration.
Full database checks use `sh gradlew integrationTest` with Docker. See the generated
`docs/TESTING.md`. Kodly executes these native commands without installing fallback checkers.

The harness recipe qualification covers all database combinations. After generating
a skeleton, verify that its native rules reject deliberately invalid source with
`KODLY_JAVA_FIXTURE=/absolute/path/to/generated-app node --test scripts/test-native-checks.mjs`.
This test uses a disposable copy and leaves the generated project unchanged.

Set `KODLY_LONG_NAME_FIXTURE` to a generated MongoDB project named
`clean-stack-final-api-20261021` to verify that identifier substitution preserves
Checkstyle and formatter compliance. Set `KODLY_MONGO_FIXTURE` to a generated
MongoDB project to check startup, migration receipts and warning-free integration
test reports; supply the Mongo test variables described in `docs/TESTING.md`.
