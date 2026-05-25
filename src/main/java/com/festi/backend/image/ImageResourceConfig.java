package com.festi.backend.image;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableConfigurationProperties(ImageStorageProperties.class)
public class ImageResourceConfig implements WebMvcConfigurer {

    private final ImageStorageProperties properties;

    public ImageResourceConfig(ImageStorageProperties properties) {
        this.properties = properties;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String resourceLocation = properties.storageRoot().toUri().toString();
        if (!resourceLocation.endsWith("/")) {
            resourceLocation += "/";
        }
        registry.addResourceHandler(properties.publicPath() + "/**")
                .addResourceLocations(resourceLocation);
    }
}
