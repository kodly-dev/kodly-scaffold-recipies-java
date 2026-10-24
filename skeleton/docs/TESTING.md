# Project verification

Run `sh gradlew build` for compilation, unit and integration tests, Spotless formatting
checks, Checkstyle and duplicate detection. These tools and their configuration belong to
this repository; the command is the same locally, in CI and in Kodly.

- `sh gradlew test` runs unit and HTTP contract tests without an external database.
- `sh gradlew spotlessCheck` checks formatting. Apply intentional changes with `spotlessApply`.
- `sh gradlew checkstyleMain checkstyleTest` uses the checked-in Google style configuration.
- `sh gradlew checkDuplicates` uses PMD CPD for identical Java blocks of at least 50 tokens.
- `sh gradlew integrationTest` runs full application/database tests using the `test` profile.

Add feature tests under `src/test/java`. Keep tool versions, rules and any new test
framework dependencies in the build. Do not import a checker or test suite from a
Kodly scratch directory. Optional recipe integrations supply their own tests.

Both test tasks attach Mockito as a Java agent through `gradle/testing.gradle`, using
the same Spring-managed version as the test dependencies. This supports final and
static mocks without dynamic agent attachment. Test workers disable class-data
sharing because inline mocking extends the bootstrap classpath; application JVMs
keep their normal defaults.

The MongoDB recipe uses `MONGO_URI_TEST` and `MONGO_DATABASE_TEST` from project secrets.
Kodly provides the same URI as the normal connection with a separate `<database>-test`
database. `src/test/resources/application-test.yml` selects those values; no Docker
container or manual database setup is needed in Kodly. Missing or unsafe settings
fail the checks. Outside Kodly, supply the same variables through your environment
or CI secret manager. Never commit credentials.

SQL and Redis recipes also use external providers configured through project secrets.
SQL needs `SQL_URL_TEST`, `SQL_USERNAME_TEST` and `SQL_PASSWORD_TEST`. Redis needs
`REDIS_URL_TEST`. Select a separate test database or server; tests refuse the normal
application database even when the credentials or URL query options differ. Neither
recipe starts Docker. The default `build` includes these integration tests and fails
with an actionable configuration error when test-provider secrets are missing.
Loopback host aliases (`localhost`, `localhost.`, 127/8 and IPv6 loopback) identify
the same provider. Redis cluster/sentinel configuration is refused by the bound
standalone test factory. MySQL Hibernate default catalog/schema overrides are
also refused. Explicit table catalogs or application-written SQL remain application
behavior: restrict the TEST database account to its own database instead of
granting it access to the application database.

All Spring application and data-slice tests install the isolation guard through
`META-INF/spring.factories`, including tests that do not extend `BaseIntegrationTest`.
Ordinary unit tests with mocked collaborators do not connect to these providers.

The guard resolves normal application configuration separately before installing any
test settings. It honors standard Spring connection properties, environment aliases
and application profiles (including default profiles), and excludes the `test`
profile, inlined/dynamic test properties and `@TestPropertySource` files from that
normal identity. Redis URL settings take precedence over standalone host/port/database
settings, as in Spring Boot. Keep application connection configuration in normal
configuration sources; a custom connection factory or programmatically created
datasource needs its own isolation checks.

For SQL, the TEST provider also overrides Hikari's pool URL/credentials and Flyway's
separate migration connection before either can open a connection. Settings that
can bypass those URLs (including JNDI, alternate datasource factories, arbitrary
driver property maps and pool initialization SQL) are refused before application
beans initialize. See the SQL recipe's testing notes for the supported settings.

Recipes declaring `requiresSkeleton` need the version in `.kodly/skeleton.json` to
match. For older generated projects, back up the project and review the skeleton's
`BaseIntegrationTest`, `BaseTestInfrastructure`, `BaseTestEnvironment`,
`META-INF/spring.factories` and Gradle
testing configuration with your existing tests. Apply the corresponding package and
class-prefix changes, run the isolated provider tests, then record the upgraded
skeleton version. Do not change the version marker without upgrading and verifying
the test infrastructure. Use a CLI release that enforces recipe compatibility.
