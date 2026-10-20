package com.template.base.shared.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Describes the application in the generated OpenAPI document. */
@Configuration
public class BaseObservabilityConfig {

  /** Creates the API title, description and version metadata. */
  @Bean
  public OpenAPI baseOpenApi(
      @Value("${spring.application.name:template-base}") String applicationName) {
    return new OpenAPI().info(new Info().title(applicationName + " API")
        .description("Production web skeleton with Problem Details, i18n, and observability")
        .version("1.0.1"));
  }
}
