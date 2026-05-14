package com.festi.backend;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class FestiBackendApplicationTests {

    @Test
    void applicationEntrypointExists() {
        assertThat(FestiBackendApplication.class).isNotNull();
    }
}
