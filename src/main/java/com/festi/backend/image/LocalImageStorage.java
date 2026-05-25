package com.festi.backend.image;

import com.festi.backend.common.exception.BadRequestException;
import com.festi.backend.common.exception.PayloadTooLargeException;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;
import java.util.Locale;
import java.util.UUID;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Component
@RequiredArgsConstructor
public class LocalImageStorage implements ImageStorage {

    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;
    private static final int MAX_DIMENSION = 4096;

    private final ImageStorageProperties properties;

    @Override
    public StoredImage store(MultipartFile image, ImageDirectory directory) {
        String extension = validateAndResolveExtension(image);
        String filename = UUID.randomUUID() + "." + extension;
        Path destination = properties.storageRoot().resolve(directory.path()).resolve(filename).normalize();

        try {
            Files.createDirectories(destination.getParent());
            try (InputStream input = image.getInputStream()) {
                Files.copy(input, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to store image file.", exception);
        }

        return new StoredImage(properties.publicPath() + "/" + directory.path() + "/" + filename);
    }

    @Override
    public void deleteIfManaged(String publicUrl) {
        String managedPrefix = properties.publicPath() + "/";
        if (publicUrl == null || !publicUrl.startsWith(managedPrefix)) {
            return;
        }

        Path target = properties.storageRoot()
                .resolve(publicUrl.substring(managedPrefix.length()))
                .normalize();
        if (!target.startsWith(properties.storageRoot())) {
            return;
        }

        try {
            Files.deleteIfExists(target);
        } catch (IOException exception) {
            log.warn("Failed to delete managed image file: {}", publicUrl, exception);
        }
    }

    private String validateAndResolveExtension(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new BadRequestException("Image file must not be empty.");
        }
        if (image.getSize() > MAX_FILE_SIZE) {
            throw new PayloadTooLargeException("Image file must not exceed 5MB.");
        }

        try (InputStream input = image.getInputStream();
             ImageInputStream imageInput = ImageIO.createImageInputStream(input)) {
            if (imageInput == null) {
                throw new BadRequestException("Image file could not be decoded.");
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInput);
            if (!readers.hasNext()) {
                throw new BadRequestException("Only JPEG and PNG images are allowed.");
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(imageInput, false, false);
                String extension = resolveExtension(reader.getFormatName());
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width > MAX_DIMENSION || height > MAX_DIMENSION) {
                    throw new BadRequestException("Image dimensions must not exceed 4096x4096.");
                }
                BufferedImage decoded = reader.read(0);
                if (decoded == null) {
                    throw new BadRequestException("Image file could not be decoded.");
                }
                return extension;
            } finally {
                reader.dispose();
            }
        } catch (BadRequestException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new BadRequestException("Image file could not be decoded.");
        }
    }

    private String resolveExtension(String formatName) {
        return switch (formatName.toLowerCase(Locale.ROOT)) {
            case "jpeg", "jpg" -> "jpg";
            case "png" -> "png";
            default -> throw new BadRequestException("Only JPEG and PNG images are allowed.");
        };
    }
}
