package com.template.base.greeting.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateGreetingRequestDto {

  @NotBlank(message = "{greeting.name.not_blank}")
  @Size(max = 100, message = "{greeting.name.size}")
  private String name;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}
