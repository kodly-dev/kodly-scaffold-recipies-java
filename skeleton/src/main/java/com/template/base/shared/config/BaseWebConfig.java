package com.template.base.shared.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Default CORS for local/browser clients. Tighten allowed origins before production. */
@Configuration
public class BaseWebConfig implements WebMvcConfigurer {

  public static final String REQUEST_ID_HEADER = "X-Request-Id";

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/**")
      .allowedOriginPatterns("*")
      .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
      .allowedHeaders("*")
      .exposedHeaders(REQUEST_ID_HEADER)
      .allowCredentials(false)
      .maxAge(3600);
  }
}
