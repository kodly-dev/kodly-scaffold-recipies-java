package com.template.base.testsupport;

import com.template.base.BaseTestEnvironment;
import java.net.URI;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;
import org.springframework.util.StringUtils;

/** Identifies a single provider only when driver options cannot reroute that connection. */
final class SqlJdbcIdentity {
  private static final Set<String> ROUTING = Set.of("pgdbname", "dbname", "database",
      "databasename",
      "pghost", "pghostaddr", "pgport", "host", "hostname", "port", "servername", "server",
      "service", "servicefile", "pgservice", "propertiestransform", "useconfigs", "socketfactory",
      "socketfactoryarg", "sessionvariables", "initsql", "connectioninitsql", "protocol", "path",
      "namedpipepath");

  private SqlJdbcIdentity() {
  }

  static boolean sameDatabase(String left, String right) {
    Target a = database(left);
    Target b = database(right);
    if (!a.dialect()
        .equals(b.dialect()) || a.port() != b.port() || !a.name()
            .equals(b.name())) {
      return false;
    }
    try {
      return BaseTestEnvironment.sameProviderHost(a.host(), b.host());
    } catch (UnknownHostException unresolved) {
      throw new IllegalStateException(
          "Cannot verify separate database servers; use distinct database names "
              + "or resolvable provider hosts.",
          unresolved);
    }
  }

  private record Target(String dialect, String host, int port, String name) {
  }

  private static Target database(String url) {
    if (!url.startsWith("jdbc:postgresql://") && !url.startsWith("jdbc:mysql://")) {
      throw new IllegalStateException("Use a PostgreSQL or MySQL JDBC URL for SQL tests.");
    }
    URI uri = URI.create(url.substring(5));
    // JDBC drivers do not share URI's fragment/query rules. In particular pgjdbc can parse a
    // selector after '#' even though URI considers it part of the fragment.
    if (uri.getRawFragment() != null) {
      throw new IllegalStateException("SQL test JDBC URLs must not contain fragments.");
    }
    rejectRouting(uri.getRawQuery());
    if (uri.getHost() == null || !StringUtils.hasText(uri.getPath()) || "/".equals(uri.getPath())) {
      throw new IllegalStateException("SQL test URLs must name a host and database.");
    }
    int port = uri.getPort() == -1
        ? "postgresql".equals(uri.getScheme()) ? 5432 : 3306
        : uri.getPort();
    String host = BaseTestEnvironment.providerHost(uri.getHost());
    // Both supported drivers form-decode the raw database path once: '+' is a space, '%2B' a plus.
    String database = URLDecoder.decode(uri.getRawPath()
        .substring(1), StandardCharsets.UTF_8);
    // Connector/J additionally trims decoded database names; pgjdbc preserves their boundaries.
    if ("mysql".equals(uri.getScheme())) {
      // MySQL installations can use case-insensitive database identifiers. A case-only
      // difference cannot demonstrate isolation; the actual connection URL stays unchanged.
      database = database.trim()
          .toLowerCase(Locale.ROOT);
    }
    if (!StringUtils.hasText(database)) {
      throw new IllegalStateException("SQL test URLs must name a host and database.");
    }
    return new Target(uri.getScheme(), host, port, database);
  }

  private static void rejectRouting(String query) {
    if (query == null) {
      return;
    }
    for (String entry : query.split("&")) {
      String rawName = entry.split("=", 2)[0];
      String name = URLDecoder.decode(rawName, StandardCharsets.UTF_8)
          .strip()
          .toLowerCase(Locale.ROOT);
      if (ROUTING.contains(name)) {
        throw new IllegalStateException("Cannot isolate SQL tests with JDBC routing option " + name
            + "; select the database and host in the JDBC URL path and authority.");
      }
    }
  }
}
