package com.template.base.greeting.model;

public final class Greeting {

  private final String message;

  public Greeting(String message) {
    this.message = message;
  }

  public String message() {
    return message;
  }
}
