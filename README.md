# Kodly Scaffold Recipes

Community recipe monorepo for the Kodly deterministic scaffolder (`kodly scaffold`).

## Contents

| Path | Description |
|------|-------------|
| `skeleton/` | Fetchable Spring Boot 4.1 web skeleton (Java 25) with Kodly Gradle markers |
| `recipes/redis-cache/` | Redis / Spring Data Redis starter |
| `recipes/actuator/` | Spring Boot Actuator health + info |
| `recipes/validation/` | Bean validation starter + sample DTO |

## Base skeleton

- **Spring Boot** 4.1.0
- **Java** 25 (via [jenv](https://github.com/jenv/jenv))
- **Starter** `spring-boot-starter-web`
- **Package** `com.template.base` with `Base*` class prefix
- **Markers** required by scaffolder:
  - `// KODLY_RECIPE_PLUGINS_MARKER`
  - `// KODLY_RECIPE_DEPENDENCIES_MARKER`

Endpoints out of the box:

- `GET /` → hello message
- `GET /health` → simple OK response

## Java 25 setup (jenv)

This project expects **Java 25** on your machine, managed with jenv. A `.java-version` file is included at the repo root and in `skeleton/`.

```bash
# 1. Install JDK 25 (macOS example)
brew install openjdk@25

# 2. Register it with jenv
jenv add "$(brew --prefix openjdk@25)/libexec/openjdk.jdk/Contents/Home"

# 3. Enable jenv shims (add to ~/.zshrc if not already)
eval "$(jenv init -)"

# 4. Use Java 25 in this repo
cd kodly-scaffold-recipes
jenv local 25.0.3

# 5. Verify
java -version
```

Gradle uses the JDK from your environment (`JAVA_HOME` set by jenv) together with the Java 25 toolchain declared in `build.gradle`.

## Quick test with Kodly CLI

### 1. Tag skeleton and recipes (one-time per clone)

The scaffolder fetches templates by git tag name:

```bash
chmod +x scripts/tag-recipes.sh
./scripts/tag-recipes.sh
```

This creates tags like `skeleton@1.0.0` and `recipes/redis-cache@1.0.0`.

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

### 4. Apply recipes

```bash
kodly scaffold redis-cache@1.0.0
kodly scaffold actuator@1.0.0
kodly scaffold validation@1.0.0

kodly scaffold list
```

### 5. Build

```bash
./gradlew compileJava
./gradlew test
./gradlew bootRun
```

### Alternative: cache recipes without git clone

```bash
mkdir -p ~/.kodly/cache/skeleton/1.0.0
cp -R skeleton/* ~/.kodly/cache/skeleton/1.0.0/

kodly scaffold init --group com.rental.app --name rental-app --path /tmp/my-app
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

Scaffold does **not** copy `build.gradle`, tests, or other DX-only files — only paths listed in `manifest.includes` (plus YAML merge / Gradle anchors).

```bash
# Compile and test everything from repo root
jenv local 25.0.3
./gradlew check
```

| Module | Gradle task |
|--------|-------------|
| Base skeleton | `:core-skeleton:bootRun` |
| All recipes | `:redis-cache:check`, `:actuator:check`, `:validation:check` |

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
      "dependencies": "implementation 'org.springframework.boot:spring-boot-starter-data-redis'"
    }
  }
}
```

| Field | Purpose |
|-------|---------|
| `includes` | **Required.** Files copied into the target project (with token mapping) |
| `config.application` | Optional YAML file to deep-merge into `application.yml` |
| `anchors.gradle` | Dependencies/plugins injected at Kodly markers in `build.gradle` |

Recipe Java sources use `com.template.base` and `Base*` names — the scaffolder rewrites them to match the target project. Recipe `build.gradle` and `src/test/**` stay in this repo only.

## Verify skeleton compiles

```bash
jenv local 25.0.3
./gradlew :core-skeleton:bootRun
```
