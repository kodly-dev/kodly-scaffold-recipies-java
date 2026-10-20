package com.template.base;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Bootstraps the application and its Spring configuration. */
@SpringBootApplication
public class BaseApplication {

  /** Starts the Spring Boot application with the supplied command-line arguments. */
  public static void main(String[] args) {
    SpringApplication.run(BaseApplication.class, args);
  }
}
