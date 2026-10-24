package com.template.base;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.test.context.MergedContextConfiguration;

/** Resolves the application's configuration independently of test-only overrides. */
public final class BaseTestEnvironment {
  private static final String SNAPSHOT = "kodlyApplicationEnvironment";

  private BaseTestEnvironment() {
  }

  /** Compare known local aliases without a DNS lookup or opening a provider connection. */
  public static String providerHost(String value) {
    String host = value.toLowerCase(Locale.ROOT);
    if (host.startsWith("[") && host.endsWith("]")) {
      host = host.substring(1, host.length() - 1);
    }
    if (host.endsWith(".")) {
      host = host.substring(0, host.length() - 1);
    }
    if (host.contains(":")) {
      try {
        // A colon identifies an IPv6 literal, so this never performs a hostname lookup.
        InetAddress literal = InetAddress.getByName(host);
        if (literal.isLoopbackAddress() || literal.isAnyLocalAddress()) {
          return "loopback";
        }
        return literal.getHostAddress();
      } catch (UnknownHostException invalid) {
        throw new IllegalStateException("Invalid provider IP address", invalid);
      }
    }
    if (host.equals("0.0.0.0") || host.equals("localhost") || host.equals("localhost.")
        || host.matches("127(?:\\.[0-9]{1,3}){3}")
        || host.equals("::1") || host.equals("0:0:0:0:0:0:0:1")) {
      return "loopback";
    }
    return host;
  }

  /** DNS aliases are not separate providers; unresolved hosts cannot prove separation. */
  public static boolean sameProviderHost(String left, String right) throws UnknownHostException {
    String a = providerHost(left);
    String b = providerHost(right);
    if (a.equals(b)) {
      return true;
    }
    Set<String> application = providerAddresses(a);
    return providerAddresses(b).stream()
        .anyMatch(application::contains);
  }

  private static Set<String> providerAddresses(String host) throws UnknownHostException {
    if (host.equals("loopback")) {
      return Set.of("loopback");
    }
    InetAddress[] addresses = InetAddress.getAllByName(host);
    if (addresses.length == 0) {
      throw new UnknownHostException("No provider addresses returned");
    }
    return Arrays.stream(addresses)
        .map(address -> providerHost(address.getHostAddress()))
        .collect(Collectors.toSet());
  }

  /** Captures once, before any recipe installs its test provider. */
  public static Environment application(Environment environment) {
    return capture((ConfigurableEnvironment) environment, null);
  }

  static Environment capture(ConfigurableEnvironment environment,
      MergedContextConfiguration configuration) {
    PropertySource<?> saved = environment.getPropertySources()
        .get(SNAPSHOT);
    if (saved != null) {
      return (Environment) saved.getSource();
    }
    Set<String> testSources = testSources(configuration);
    StandardEnvironment normal = new StandardEnvironment();
    normal.getPropertySources()
        .remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
    normal.getPropertySources()
        .remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
    for (PropertySource<?> source : environment.getPropertySources()) {
      String name = source.getName();
      if (!name.equals("configurationProperties") && !name.startsWith("Config resource '")
          && !name.startsWith("applicationConfig: [") && !name.startsWith("kodly")
          && !testSources.contains(name)) {
        normal.getPropertySources()
            .addLast(source);
      }
    }
    normal.setActiveProfiles(withoutTest(environment.getActiveProfiles()));
    normal.setDefaultProfiles(withoutTest(environment.getDefaultProfiles()));
    String active = normal.getProperty("spring.profiles.active", "");
    if (Arrays.asList(active.split(","))
        .stream()
        .anyMatch(value -> value.trim()
            .equals("test"))) {
      normal.getPropertySources()
          .addFirst(new MapPropertySource("kodlyNormalProfiles", Map.of(
              "spring.profiles.active",
              String.join(",", withoutTest(environment.getActiveProfiles())))));
    }
    ConfigDataEnvironmentPostProcessor.applyTo(normal);
    if (Arrays.asList(normal.getActiveProfiles())
        .contains("test")
        || normal.getActiveProfiles().length == 0
            && Arrays.asList(normal.getDefaultProfiles())
                .contains("test")) {
      throw new IllegalStateException(
          "Cannot resolve the normal application provider with the test profile active.");
    }
    environment.getPropertySources()
        .addLast(new PropertySource<Environment>(SNAPSHOT, normal) {
          @Override
          public Object getProperty(String name) {
            return null;
          }
        });
    return normal;
  }

  private static String[] withoutTest(String[] profiles) {
    return Arrays.stream(profiles)
        .filter(profile -> !profile.equals("test"))
        .toArray(String[]::new);
  }

  private static Set<String> testSources(MergedContextConfiguration configuration) {
    Set<String> names = new HashSet<>(Set.of(
        "Inlined Test Properties", "Dynamic Test Properties", "active-test-profiles"));
    if (configuration != null) {
      DefaultResourceLoader resources = new DefaultResourceLoader();
      configuration.getPropertySourceDescriptors()
          .forEach(descriptor -> {
            if (descriptor.name() != null) {
              names.add(descriptor.name());
            }
            descriptor.locations()
                .forEach(location -> names.add(resources.getResource(location)
                    .getDescription()));
          });
    }
    return names;
  }
}
