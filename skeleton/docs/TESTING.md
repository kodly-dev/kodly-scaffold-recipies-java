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

Optional SQL and Redis recipes still supply their integration infrastructure through
Testcontainers and require Docker. See the installed recipe documentation.
