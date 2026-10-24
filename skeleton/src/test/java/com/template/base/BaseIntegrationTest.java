package com.template.base;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/** Starts the complete application with the infrastructure supplied by installed recipes. */
@SpringBootTest(classes = BaseApplication.class)
@Import(BaseTestInfrastructure.class)
@ActiveProfiles("test")
@Tag("integration")
public abstract class BaseIntegrationTest {
}
