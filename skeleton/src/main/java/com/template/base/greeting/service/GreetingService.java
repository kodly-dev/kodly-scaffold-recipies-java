package com.template.base.greeting.service;

import com.template.base.greeting.dto.CreateGreetingRequestDto;
import com.template.base.greeting.model.Greeting;

/** Creates greetings without coupling the domain to HTTP transport. */
public interface GreetingService {

  /** Returns the default application greeting. */
  Greeting defaultGreeting();

  /** Returns a personalized greeting for the supplied request. */
  Greeting create(CreateGreetingRequestDto request);
}
