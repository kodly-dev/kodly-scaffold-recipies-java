package com.template.base.greeting.controller;

import com.template.base.greeting.dto.CreateGreetingRequestDto;
import com.template.base.greeting.model.Greeting;
import com.template.base.greeting.service.GreetingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Exposes the default greeting and validated personalized greetings. */
@RestController
@RequestMapping
@Tag(name = "Greeting")
public class BaseGreetingController {

  private final GreetingService greetingService;

  /** Creates the controller with its greeting service. */
  public BaseGreetingController(GreetingService greetingService) {
    this.greetingService = greetingService;
  }

  /** Returns the default greeting as plain text. */
  @GetMapping(value = "/", produces = MediaType.TEXT_PLAIN_VALUE)
  @Operation(summary = "Default greeting")
  public ResponseEntity<String> hello() {
    return ResponseEntity.ok(greetingService.defaultGreeting()
        .message());
  }

  /** Creates a greeting from validated request data. */
  @PostMapping(value = "/greetings", consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(summary = "Create a personalized greeting")
  public ResponseEntity<Greeting> create(@Valid @RequestBody CreateGreetingRequestDto request) {
    Greeting greeting = greetingService.create(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(greeting);
  }
}
