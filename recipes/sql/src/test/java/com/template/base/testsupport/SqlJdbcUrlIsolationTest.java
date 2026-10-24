package com.template.base.testsupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Properties;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.util.ClassUtils;

/** Exercises installed driver URL parsing without opening a connection. */
class SqlJdbcUrlIsolationTest {
  private static final boolean POSTGRES = ClassUtils.isPresent("org.postgresql.Driver", null);
  private static final String PREFIX = POSTGRES
      ? "jdbc:postgresql://fixture.invalid/"
      : "jdbc:mysql://fixture.invalid/";

  @Test
  void actualDriverDatabaseOverrideIsRefusedBeforeBeansInitialize() throws Exception {
    String name = POSTGRES ? "PGDBNAME" : "DBNAME";
    String url = PREFIX + "application_test?" + name + "=application";
    assertThat(driverDatabase(url)).isEqualTo("application");
    assertThatThrownBy(() -> new SqlTestConfiguration(environment(url)))
        .hasMessageContaining("routing");
  }

  @ParameterizedTest
  @ValueSource(strings = {"PGDBNAME", "pgdbname", "DBNAME", "dbname", "%44BNAME",
      "%50GDBNAME", "PGHOST", "PGPORT", "HOST", "PORT", "propertiesTransform", "useConfigs",
      "protocol", "path", "namedPipePath"})
  void encodedAndCaseVariantsCannotOverrideTheTestProvider(String selector) {
    assertThatThrownBy(() -> new SqlTestConfiguration(
        environment(PREFIX + "application_test?" + selector + "=application")))
        .hasMessageContaining("routing");
  }

  @ParameterizedTest
  @ValueSource(strings = {"#?PGDBNAME=application", "?ordinary=true#&PGDBNAME=application"})
  void uriFragmentsCannotHideDriverRoutingOptions(String suffix) throws Exception {
    String url = PREFIX + "application_test" + suffix;
    if (POSTGRES) {
      assertThat(driverDatabase(url)).isEqualTo("application");
    }
    assertThatThrownBy(() -> new SqlTestConfiguration(environment(url)))
        .hasMessageContaining("fragment");
  }

  @ParameterizedTest
  @ValueSource(strings = {"SQL_URL", "spring.datasource.url",
      "spring.datasource.hikari.jdbc-url", "spring.flyway.url"})
  void applicationPoolAndMigrationCandidatesAlsoRejectRoutingOverrides(String property) {
    String name = POSTGRES ? "PGDBNAME" : "DBNAME";
    assertThatThrownBy(() -> new SqlTestConfiguration(environment(PREFIX + "application_test")
        .withProperty(property, PREFIX + "unrelated?" + name + "=application_test")))
        .hasMessageContaining("routing");
  }

  @Test
  void ordinaryConnectionOptionsAndSeparateTestProvidersStillWork() throws Exception {
    String options =
        POSTGRES ? "sslmode=prefer&connectTimeout=3" : "useSSL=false&connectTimeout=3000";
    String url = PREFIX + "application_test?" + options;
    assertThat(driverDatabase(url)).isEqualTo("application_test");
    MockEnvironment environment = environment(url);
    assertThatCode(() -> new SqlTestConfiguration(environment)).doesNotThrowAnyException();
    assertThat(environment.getProperty("spring.datasource.hikari.jdbc-url")).isEqualTo(url);
    assertThat(environment.getProperty("spring.flyway.url")).isEqualTo(url);
  }

  @ParameterizedTest
  @CsvSource({"application+data,application%20data", "application%20data,application+data",
      "application%2Bdata,application%2bdata", "application%2bdata,application%2Bdata",
      "application%2B+data,application%2B%20data", "application%2B%20data,application%2B+data"})
  void equivalentDriverPathsCannotSelectAnyApplicationDatabase(String normal, String test)
      throws Exception {
    String normalUrl = PREFIX + normal;
    String testUrl = PREFIX + test;
    assertThat(driverDatabase(normalUrl)).isEqualTo(driverDatabase(testUrl));
    for (String property : normalUrlProperties()) {
      assertThatThrownBy(() -> new SqlTestConfiguration(
          environment(testUrl).withProperty(property, normalUrl)))
          .as("The installed driver selects the same database through %s", property)
          .hasMessageContaining("separate database");
    }
  }

  @ParameterizedTest
  @CsvSource({"application+data,application%2Bdata", "application%2Bdata,application+data",
      "application%20data,application%2Bdata", "application%2Bdata,application%20data",
      "application%2520data,application%20data", "application%20data,application%2520data"})
  void distinctDriverPathsRemainSeparateWithoutDoubleDecoding(String normal, String test)
      throws Exception {
    String normalUrl = PREFIX + normal;
    String testUrl = PREFIX + test;
    assertThat(driverDatabase(normalUrl)).isNotEqualTo(driverDatabase(testUrl));
    for (String property : normalUrlProperties()) {
      MockEnvironment configured = environment(testUrl).withProperty(property, normalUrl);
      assertThatCode(() -> new SqlTestConfiguration(configured)).doesNotThrowAnyException();
      assertThat(configured.getProperty("spring.datasource.hikari.jdbc-url")).isEqualTo(testUrl);
      assertThat(configured.getProperty("spring.flyway.url")).isEqualTo(testUrl);
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"+application", "application+", "%20application", "application%20",
      "application%09", "application%00"})
  void boundaryAliasesFollowTheInstalledProvidersDatabaseRules(String spelling) throws Exception {
    assertBoundaryIdentity(spelling);
  }

  @ParameterizedTest
  @MethodSource("boundaryControls")
  void everyAsciiBoundaryControlFollowsTheInstalledProvider(String spelling) throws Exception {
    assertBoundaryIdentity(spelling);
  }

  private static Stream<String> boundaryControls() {
    return IntStream.rangeClosed(0, 32)
        .mapToObj(value -> String.format("%%%02X", value))
        .flatMap(encoded -> Stream.of(encoded + "application", "application" + encoded));
  }

  private void assertBoundaryIdentity(String spelling) throws Exception {
    String canonical = PREFIX + "application";
    String alias = PREFIX + spelling;
    if (POSTGRES) {
      assertThat(driverDatabase(alias)).isNotEqualTo(driverDatabase(canonical));
    } else {
      assertThat(driverDatabase(alias)).isEqualTo(driverDatabase(canonical));
    }
    for (String property : normalUrlProperties()) {
      for (boolean reversed : List.of(false, true)) {
        String normal = reversed ? alias : canonical;
        String test = reversed ? canonical : alias;
        MockEnvironment configured = environment(test).withProperty("SQL_URL", PREFIX + "unrelated")
            .withProperty(property, normal);
        if (POSTGRES) {
          assertThatCode(() -> new SqlTestConfiguration(configured)).doesNotThrowAnyException();
        } else {
          assertThatThrownBy(() -> new SqlTestConfiguration(configured))
              .hasMessageContaining("separate database");
        }
      }
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"%2Bapplication", "application%2B", "%2520application",
      "application%2520", "%E2%80%83application", "application%E2%80%83"})
  void literalPlusesPercentEscapesAndUnicodeSpacesRemainDistinct(String spelling) throws Exception {
    String canonical = PREFIX + "application";
    String other = PREFIX + spelling;
    assertThat(driverDatabase(other)).isNotEqualTo(driverDatabase(canonical));
    for (String property : normalUrlProperties()) {
      assertThatCode(() -> new SqlTestConfiguration(environment(other)
          .withProperty("SQL_URL", PREFIX + "unrelated")
          .withProperty(property, canonical)))
          .doesNotThrowAnyException();
      assertThatCode(() -> new SqlTestConfiguration(environment(canonical)
          .withProperty("SQL_URL", PREFIX + "unrelated")
          .withProperty(property, other)))
          .doesNotThrowAnyException();
    }
  }

  private List<String> normalUrlProperties() {
    return List.of("SQL_URL", "spring.datasource.url", "spring.datasource.hikari.jdbc-url",
        "spring.flyway.url");
  }

  private MockEnvironment environment(String testUrl) {
    return new MockEnvironment().withProperty("SQL_URL", PREFIX + "application")
        .withProperty("SQL_URL_TEST", testUrl)
        .withProperty("SQL_USERNAME_TEST", "fixture")
        .withProperty("SQL_PASSWORD_TEST", "fixture-only");
  }

  private String driverDatabase(String url) throws Exception {
    if (POSTGRES) {
      Properties properties = (Properties) Class.forName("org.postgresql.Driver")
          .getMethod("parseURL", String.class, Properties.class)
          .invoke(null, url, new Properties());
      return properties.getProperty("PGDBNAME");
    }
    Class<?> type = Class.forName("com.mysql.cj.conf.ConnectionUrl");
    Object parsed = type.getMethod("getConnectionUrlInstance", String.class, Properties.class)
        .invoke(null, url, new Properties());
    Object host = type.getMethod("getMainHost")
        .invoke(parsed);
    return (String) host.getClass()
        .getMethod("getDatabase")
        .invoke(host);
  }
}
