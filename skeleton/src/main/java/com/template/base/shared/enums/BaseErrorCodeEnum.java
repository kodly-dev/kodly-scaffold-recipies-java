package com.template.base.shared.enums;

/** Shared error codes used for Problem Details type URIs and i18n message keys. */
public enum BaseErrorCodeEnum {
  VALIDATION("error.validation", "https://api.template-base.local/problems/validation"),
  NOT_FOUND("error.not_found", "https://api.template-base.local/problems/not-found"),
  INTERNAL("error.internal", "https://api.template-base.local/problems/internal");

  private final String messageKey;
  private final String typeUri;

  BaseErrorCodeEnum(String messageKey, String typeUri) {
    this.messageKey = messageKey;
    this.typeUri = typeUri;
  }

  public String messageKey() {
    return messageKey;
  }

  public String typeUri() {
    return typeUri;
  }
}
