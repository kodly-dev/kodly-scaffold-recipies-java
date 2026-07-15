package com.template.base.greeting.service;

import com.template.base.greeting.dto.CreateGreetingRequestDto;
import com.template.base.greeting.model.Greeting;

public interface GreetingService {

  Greeting defaultGreeting();

  Greeting create(CreateGreetingRequestDto request);
}
