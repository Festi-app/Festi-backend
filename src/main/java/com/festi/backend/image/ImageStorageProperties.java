package com.festi.backend.image;

import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "festi.images")
public record ImageStorageProperties(Path storageRoot, String publicPath) {

    public ImageStorageProperties {
        storageRoot = storageRoot.toAbsolutePath().normalize();
        publicPath = normalizePublicPath(publicPath);
    }

    private static String normalizePublicPath(String publicPath) {
        String normalized = publicPath.startsWith("/") ? publicPath : "/" + publicPath;
        while (normalized.endsWith("/") && normalized.length() > 1) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
