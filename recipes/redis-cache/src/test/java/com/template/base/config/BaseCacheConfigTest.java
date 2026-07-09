package com.template.base.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class BaseCacheConfigTest {

    @Test
    void recipeMainSourcesCompileAndInstantiate() {
        assertNotNull(new BaseCacheConfig());
    }
}
