package com.template.base.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BaseSampleRequestTest {

  private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

  @Test
  void rejectsBlankName() {
    BaseSampleRequest request = new BaseSampleRequest();
    request.setName("");

    Set<ConstraintViolation<BaseSampleRequest>> violations = validator.validate(request);
    assertFalse(violations.isEmpty());
  }

  @Test
  void acceptsValidName() {
    BaseSampleRequest request = new BaseSampleRequest();
    request.setName("kodly");

    Set<ConstraintViolation<BaseSampleRequest>> violations = validator.validate(request);
    assertTrue(violations.isEmpty());
  }
}
