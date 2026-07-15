package com.template.base;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class BaseApplicationTests {

  @Autowired
  private MockMvc mockMvc;

  @Test
  void contextLoads() {
  }

  @Test
  void defaultGreetingReturnsHello() throws Exception {
    mockMvc.perform(get("/"))
      .andExpect(status().isOk())
      .andExpect(content().string(containsString("Hello from Kodly")));
  }

  @Test
  void createGreetingRejectsBlankName() throws Exception {
    mockMvc.perform(post("/greetings").contentType(MediaType.APPLICATION_JSON)
      .content("{\"name\":\"\"}"))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.title").value("Validation failed"))
      .andExpect(jsonPath("$.errors.name").exists())
      .andExpect(jsonPath("$.requestId").exists());
  }

  @Test
  void createGreetingAcceptsValidName() throws Exception {
    mockMvc.perform(post("/greetings").contentType(MediaType.APPLICATION_JSON)
      .content("{\"name\":\"Ada\"}"))
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.message").value("Hello, Ada!"));
  }

  @Test
  void unknownPathReturnsProblemNotFound() throws Exception {
    mockMvc.perform(get("/no-such-route"))
      .andExpect(status().isNotFound())
      .andExpect(jsonPath("$.title").value("Resource not found"))
      .andExpect(jsonPath("$.detail").value(containsString("not found")));
  }

  @Test
  void correlationIdIsEchoedOnResponse() throws Exception {
    mockMvc.perform(get("/").header("X-Request-Id", "test-request-123"))
      .andExpect(status().isOk())
      .andExpect(header().string("X-Request-Id", "test-request-123"));
  }

  @Test
  void acceptLanguageAffectsProblemDetail() throws Exception {
    mockMvc.perform(get("/no-such-route").header("Accept-Language", "es"))
      .andExpect(status().isNotFound())
      .andExpect(jsonPath("$.title").value("Recurso no encontrado"))
      .andExpect(jsonPath("$.detail").value(containsString("encontrado")))
      .andExpect(jsonPath("$.detail", not(containsString("not found"))));
  }

  @Test
  void actuatorHealthIsUp() throws Exception {
    mockMvc.perform(get("/actuator/health"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.status").value("UP"));
  }

  @Test
  void openApiDocsAreAvailable() throws Exception {
    mockMvc.perform(get("/v3/api-docs"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.openapi").exists())
      .andExpect(jsonPath("$.paths./").exists());
  }
}
