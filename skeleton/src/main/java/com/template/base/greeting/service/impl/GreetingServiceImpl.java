package com.template.base.greeting.service.impl;

import com.template.base.greeting.dto.CreateGreetingRequestDto;
import com.template.base.greeting.model.Greeting;
import com.template.base.greeting.service.GreetingService;
import org.springframework.stereotype.Service;

@Service
public class GreetingServiceImpl implements GreetingService {

  @Override
  public Greeting defaultGreeting() {
    return new Greeting("Hello from Kodly base skeleton");
  }

  @Override
  public Greeting create(CreateGreetingRequestDto request) {
    return new Greeting("Hello, " + request.getName() + "!");
  }
}
