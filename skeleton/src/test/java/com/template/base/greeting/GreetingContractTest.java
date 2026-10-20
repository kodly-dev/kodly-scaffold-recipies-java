package com.template.base.greeting;

import static org.assertj.core.api.Assertions.assertThat;

import com.template.base.greeting.controller.BaseGreetingController;
import com.template.base.greeting.dto.CreateGreetingRequestDto;
import com.template.base.greeting.model.Greeting;
import com.template.base.greeting.service.impl.GreetingServiceImpl;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import tools.jackson.databind.json.JsonMapper;

class GreetingContractTest {

  private final BaseGreetingController controller =
      new BaseGreetingController(new GreetingServiceImpl());

  @Test
  void personalizedGreetingKeepsItsMessageInJson() {
    CreateGreetingRequestDto request = new CreateGreetingRequestDto();
    request.setName("Ada");
    ResponseEntity<Greeting> response = controller.create(request);
    JsonMapper mapper = JsonMapper.builder()
        .build();

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(mapper.readTree(mapper.writeValueAsString(response.getBody()))
        .get("message")
        .asText())
        .isEqualTo("Hello, Ada!");
  }

  @Test
  void rejectsEmptyAndOverlongNames() {
    try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
      Validator validator = factory.getValidator();
      for (String name : List.of("", "  ", "x".repeat(101))) {
        CreateGreetingRequestDto request = new CreateGreetingRequestDto();
        request.setName(name);
        assertThat(validator.validate(request)).isNotEmpty();
      }
    }
  }

  @Test
  void defaultGreetingHasAnOkResponseAndPlainTextBody() {
    ResponseEntity<String> response = controller.hello();
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isEqualTo("Hello from Kodly base skeleton");
  }
}
