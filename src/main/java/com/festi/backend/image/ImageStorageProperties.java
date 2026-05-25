package com.festi.backend.image;

import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

@ConfigurationProperties(prefix = "festi.images")
public record ImageStorageProperties(
        Path storageRoot,
        String publicPath,
        DataSize maxFileSize,
        int maxWidth,
        int maxHeight
) {

    public ImageStorageProperties {
        storageRoot = storageRoot.toAbsolutePath().normalize();
        publicPath = normalizePublicPath(publicPath);
        if (maxFileSize == null || maxFileSize.toBytes() <= 0 || maxWidth <= 0 || maxHeight <= 0) {
            throw new IllegalArgumentException("Image storage limits must be positive.");
        }
    }

    private static String normalizePublicPath(String publicPath) {
        String normalized = publicPath.startsWith("/") ? publicPath : "/" + publicPath;
        while (normalized.endsWith("/") && normalized.length() > 1) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
