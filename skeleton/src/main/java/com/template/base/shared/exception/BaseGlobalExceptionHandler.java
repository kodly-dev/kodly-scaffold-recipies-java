package com.template.base.shared.exception;

import com.template.base.shared.enums.BaseErrorCodeEnum;
import com.template.base.shared.filter.BaseCorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class BaseGlobalExceptionHandler extends ResponseEntityExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(BaseGlobalExceptionHandler.class);

  private final MessageSource messageSource;

  public BaseGlobalExceptionHandler(MessageSource messageSource) {
    this.messageSource = messageSource;
  }

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers,
      HttpStatusCode status, WebRequest request) {
    Map<String, String> fieldErrors = new LinkedHashMap<>();
    for (FieldError fieldError : ex.getBindingResult()
      .getFieldErrors()) {
      fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
    }
    ProblemDetail problem = buildProblem(HttpStatus.BAD_REQUEST, BaseErrorCodeEnum.VALIDATION, request);
    problem.setProperty("errors", fieldErrors);
    return handleExceptionInternal(ex, problem, headers, HttpStatus.BAD_REQUEST, request);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<Object> handleConstraintViolation(ConstraintViolationException ex, WebRequest request) {
    Map<String, String> fieldErrors = new LinkedHashMap<>();
    ex.getConstraintViolations()
      .forEach(violation -> fieldErrors.put(violation.getPropertyPath()
        .toString(), violation.getMessage()));
    ProblemDetail problem = buildProblem(HttpStatus.BAD_REQUEST, BaseErrorCodeEnum.VALIDATION, request);
    problem.setProperty("errors", fieldErrors);
    return handleExceptionInternal(ex, problem, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Object> handleUnhandled(Exception ex, WebRequest request) {
    log.error("Unhandled exception", ex);
    ProblemDetail problem = buildProblem(HttpStatus.INTERNAL_SERVER_ERROR, BaseErrorCodeEnum.INTERNAL, request);
    return handleExceptionInternal(ex, problem, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
  }

  @Override
  protected ResponseEntity<Object> handleExceptionInternal(Exception ex, @Nullable Object body, HttpHeaders headers,
      HttpStatusCode statusCode, WebRequest request) {
    if (body == null && ex instanceof ErrorResponse) {
      body = buildProblem(resolveStatus(statusCode), resolveCode(statusCode), request);
    } else if (body instanceof ProblemDetail problemDetail) {
      applyI18nIfDefault(problemDetail, statusCode);
      enrich(problemDetail, request);
    }
    return super.handleExceptionInternal(ex, body, headers, statusCode, request);
  }

  private ProblemDetail buildProblem(HttpStatus status, BaseErrorCodeEnum code, WebRequest request) {
    Locale locale = LocaleContextHolder.getLocale();
    String title = messageSource.getMessage(code.messageKey() + ".title", null, code.name(), locale);
    String detail = messageSource.getMessage(code.messageKey() + ".detail", null, code.name(), locale);

    ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
    problem.setTitle(title);
    problem.setType(URI.create(code.typeUri()));
    enrich(problem, request);
    return problem;
  }

  private void applyI18nIfDefault(ProblemDetail problem, HttpStatusCode statusCode) {
    if (!isDefaultStatusTitle(problem, statusCode)) {
      return;
    }
    BaseErrorCodeEnum code = resolveCode(statusCode);
    Locale locale = LocaleContextHolder.getLocale();
    problem.setTitle(messageSource.getMessage(code.messageKey() + ".title", null, code.name(), locale));
    problem.setDetail(messageSource.getMessage(code.messageKey() + ".detail", null, code.name(), locale));
    problem.setType(URI.create(code.typeUri()));
  }

  private void enrich(ProblemDetail problem, WebRequest request) {
    if (request instanceof ServletWebRequest servletWebRequest) {
      HttpServletRequest servletRequest = servletWebRequest.getRequest();
      problem.setInstance(URI.create(servletRequest.getRequestURI()));
    }
    String requestId = MDC.get(BaseCorrelationIdFilter.MDC_REQUEST_ID);
    if (requestId != null) {
      problem.setProperty("requestId", requestId);
    }
    String traceId = MDC.get("traceId");
    if (traceId != null) {
      problem.setProperty("traceId", traceId);
    }
  }

  private HttpStatus resolveStatus(HttpStatusCode statusCode) {
    HttpStatus status = HttpStatus.resolve(statusCode.value());
    return status != null ? status : HttpStatus.INTERNAL_SERVER_ERROR;
  }

  private BaseErrorCodeEnum resolveCode(HttpStatusCode statusCode) {
    if (statusCode.value() == HttpStatus.NOT_FOUND.value()) {
      return BaseErrorCodeEnum.NOT_FOUND;
    }
    if (statusCode.is4xxClientError()) {
      return BaseErrorCodeEnum.VALIDATION;
    }
    return BaseErrorCodeEnum.INTERNAL;
  }

  private boolean isDefaultStatusTitle(ProblemDetail problem, HttpStatusCode statusCode) {
    HttpStatus status = HttpStatus.resolve(statusCode.value());
    if (status == null) {
      return false;
    }
    String title = problem.getTitle();
    return title == null || title.equals(status.getReasonPhrase());
  }
}
