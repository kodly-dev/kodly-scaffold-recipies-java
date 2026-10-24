package com.template.base.testsupport;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mockStatic;

import java.net.InetAddress;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.mock.env.MockEnvironment;

class SqlDnsIsolationTest {
  private MockEnvironment provider() {
    return new MockEnvironment()
        .withProperty("SQL_URL", "jdbc:postgresql://application.example/app")
        .withProperty("SQL_URL_TEST", "jdbc:postgresql://tests.example/app")
        .withProperty("SQL_USERNAME_TEST", "tests")
        .withProperty("SQL_PASSWORD_TEST", "fixture-only");
  }

  @Test
  void dnsAliasesCannotProveThatIdenticalDatabaseNamesAreIsolated() throws Exception {
    InetAddress address = InetAddress.getByAddress(new byte[]{(byte) 192, 0, 2, 1});
    try (MockedStatic<InetAddress> dns = mockStatic(InetAddress.class)) {
      dns.when(() -> InetAddress.getAllByName("application.example"))
          .thenReturn(new InetAddress[]{address});
      dns.when(() -> InetAddress.getAllByName("tests.example"))
          .thenReturn(new InetAddress[]{address});
      assertThatThrownBy(() -> new SqlTestConfiguration(provider()))
          .hasMessageContaining("separate database");
    }
  }

  @Test
  void distinctServersCanRetainIdenticalDatabaseNames() throws Exception {
    InetAddress application = InetAddress.getByAddress(new byte[]{(byte) 192, 0, 2, 1});
    InetAddress tests = InetAddress.getByAddress(new byte[]{(byte) 192, 0, 2, 2});
    try (MockedStatic<InetAddress> dns = mockStatic(InetAddress.class)) {
      dns.when(() -> InetAddress.getAllByName("application.example"))
          .thenReturn(new InetAddress[]{application});
      dns.when(() -> InetAddress.getAllByName("tests.example"))
          .thenReturn(new InetAddress[]{tests});
      new SqlTestConfiguration(provider());
    }
  }
}
