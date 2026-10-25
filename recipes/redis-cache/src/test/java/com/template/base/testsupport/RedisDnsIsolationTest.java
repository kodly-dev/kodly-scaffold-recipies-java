package com.template.base.testsupport;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mockStatic;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.springframework.mock.env.MockEnvironment;

class RedisDnsIsolationTest {
  @TempDir
  Path directory;

  private MockEnvironment provider(String testUrl) {
    return new MockEnvironment().withProperty("REDIS_URL", "redis://application.example:6379/0")
        .withProperty("REDIS_URL_TEST", testUrl);
  }

  @Test
  void dnsAliasesCannotSelectTheApplicationDatabase() throws Exception {
    InetAddress same = InetAddress.getByAddress(new byte[]{(byte) 192, 0, 2, 1});
    try (MockedStatic<InetAddress> dns = mockStatic(InetAddress.class)) {
      dns.when(() -> InetAddress.getAllByName("application.example"))
          .thenReturn(new InetAddress[]{same});
      dns.when(() -> InetAddress.getAllByName("tests.example"))
          .thenReturn(new InetAddress[]{same});
      assertThatThrownBy(() -> new RedisTestConfiguration(provider("redis://tests.example:6379/0")))
          .hasMessageContaining("separate server or database");
    }
  }

  @Test
  void anyOverlappingAddressRefusesTheSameDatabase() throws Exception {
    InetAddress shared = InetAddress.getByAddress(new byte[]{(byte) 192, 0, 2, 1});
    InetAddress other = InetAddress.getByAddress(new byte[]{(byte) 192, 0, 2, 2});
    try (MockedStatic<InetAddress> dns = mockStatic(InetAddress.class)) {
      dns.when(() -> InetAddress.getAllByName("application.example"))
          .thenReturn(new InetAddress[]{shared});
      dns.when(() -> InetAddress.getAllByName("tests.example"))
          .thenReturn(new InetAddress[]{other, shared});
      assertThatThrownBy(() -> new RedisTestConfiguration(provider("redis://tests.example:6379/0")))
          .hasMessageContaining("separate server or database");
    }
  }

  @Test
  void unresolvableServersCannotProveSeparation() throws Exception {
    InetAddress address = InetAddress.getByAddress(new byte[]{(byte) 192, 0, 2, 1});
    try (MockedStatic<InetAddress> dns = mockStatic(InetAddress.class)) {
      dns.when(() -> InetAddress.getAllByName("application.example"))
          .thenReturn(new InetAddress[]{address});
      dns.when(() -> InetAddress.getAllByName("tests.example"))
          .thenThrow(new UnknownHostException("fixture"));
      assertThatThrownBy(() -> new RedisTestConfiguration(provider("redis://tests.example:6379/0")))
          .hasMessageContaining("Cannot verify separate Redis servers");
    }
  }

  @Test
  void distinctResolvedServersCanKeepTheSameDatabaseNumber() throws Exception {
    InetAddress app = InetAddress.getByAddress(new byte[]{(byte) 192, 0, 2, 1});
    InetAddress tests = InetAddress.getByAddress(new byte[]{(byte) 192, 0, 2, 2});
    try (MockedStatic<InetAddress> dns = mockStatic(InetAddress.class)) {
      dns.when(() -> InetAddress.getAllByName("application.example"))
          .thenReturn(new InetAddress[]{app});
      dns.when(() -> InetAddress.getAllByName("tests.example"))
          .thenReturn(new InetAddress[]{tests});
      new RedisTestConfiguration(provider("redis://tests.example:6379/0"));
    }
  }

  @Test
  void anExplicitlyDistinctDatabaseOrPortDoesNotNeedDns() {
    try (MockedStatic<InetAddress> dns = mockStatic(InetAddress.class)) {
      for (String url : new String[]{"redis://tests.example:6379/1",
          "redis://tests.example:6380/0"}) {
        new RedisTestConfiguration(provider(url));
      }
      dns.verifyNoInteractions();
    }
  }

  @Test
  void invalidShortUrlsStillFailBeforeBinding() {
    assertThatThrownBy(() -> new RedisTestConfiguration(provider("redis://127.1:6379/0")))
        .hasMessageContaining("Use a redis:// or rediss:// URL");
  }

  @Test
  void standaloneSpringPropertiesUseTheSameResolvedHostPolicy() throws Exception {
    Files.writeString(directory.resolve("application.yml"),
        "spring.application.name: redis-dns-fixture\n");
    InetAddress same = InetAddress.getByAddress(new byte[]{(byte) 192, 0, 2, 1});
    try (MockedStatic<InetAddress> dns = mockStatic(InetAddress.class)) {
      dns.when(() -> InetAddress.getAllByName("application.example"))
          .thenReturn(new InetAddress[]{same});
      dns.when(() -> InetAddress.getAllByName("tests.example"))
          .thenReturn(new InetAddress[]{same});
      MockEnvironment environment =
          new MockEnvironment().withProperty("spring.config.location", directory.toUri()
              .toString())
              .withProperty("spring.data.redis.host", "application.example")
              .withProperty("spring.data.redis.port", "6379")
              .withProperty("spring.data.redis.database", "0")
              .withProperty("REDIS_URL_TEST", "redis://tests.example:6379/0");
      assertThatThrownBy(() -> new RedisTestConfiguration(environment))
          .hasMessageContaining("separate server or database");
    }
  }
}
