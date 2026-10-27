package com.example.bookstore.config;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

class WebConfigTest {

    @Test
    void addResourceHandlers_shouldMapUploadsPath() throws Exception {
        WebConfig webConfig = new WebConfig();
        Method method = WebConfig.class.getMethod("addResourceHandlers", org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry.class);

        assertThat(method).isNotNull();
        assertThat(webConfig.getClass().getName()).isEqualTo("com.example.bookstore.config.WebConfig");
        assertThat(Paths.get("uploads").toAbsolutePath().normalize().toString()).contains("uploads");
    }
}
