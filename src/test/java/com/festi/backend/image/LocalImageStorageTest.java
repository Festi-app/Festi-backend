package com.festi.backend.image;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.festi.backend.common.exception.BadRequestException;
import com.festi.backend.common.exception.PayloadTooLargeException;
import com.festi.backend.image.ImageStorage.ImageDirectory;
import com.festi.backend.image.ImageStorage.StoredImage;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.unit.DataSize;

class LocalImageStorageTest {

    @TempDir
    Path tempDir;

    private LocalImageStorage storage;

    @BeforeEach
    void setUp() {
        storage = new LocalImageStorage(
                new ImageStorageProperties(tempDir, "/media/images", DataSize.ofMegabytes(5), 4096, 4096));
    }

    @Test
    void storesPngAndJpegWithGeneratedFileNames() throws IOException {
        StoredImage boothImage = storage.store(image("booth.png", "image/png", "png", 10, 10), ImageDirectory.BOOTHS);
        StoredImage menuImage = storage.store(image("menu.jpg", "image/jpeg", "jpg", 10, 10), ImageDirectory.MENUS);

        assertThat(boothImage.publicUrl()).matches("/media/images/booths/[0-9a-f-]{36}\\.png");
        assertThat(menuImage.publicUrl()).matches("/media/images/menus/[0-9a-f-]{36}\\.jpg");
        assertThat(fileFor(boothImage)).exists();
        assertThat(fileFor(menuImage)).exists();
    }

    @Test
    void rejectsEmptyCorruptedAndUnsupportedImages() throws IOException {
        MockMultipartFile empty = new MockMultipartFile("image", "empty.png", "image/png", new byte[0]);
        MockMultipartFile corrupted = new MockMultipartFile("image", "broken.png", "image/png", new byte[]{1, 2, 3});
        MockMultipartFile gif = image("image.gif", "image/gif", "gif", 10, 10);

        assertThatThrownBy(() -> storage.store(null, ImageDirectory.BOOTHS))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> storage.store(empty, ImageDirectory.BOOTHS))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> storage.store(corrupted, ImageDirectory.BOOTHS))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> storage.store(gif, ImageDirectory.BOOTHS))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void rejectsImagesAboveSizeAndResolutionLimits() throws IOException {
        MockMultipartFile tooLarge = new MockMultipartFile("image", "large.png", "image/png",
                new byte[(5 * 1024 * 1024) + 1]);
        MockMultipartFile tooWide = image("wide.png", "image/png", "png", 4097, 1);

        assertThatThrownBy(() -> storage.store(tooLarge, ImageDirectory.BOOTHS))
                .isInstanceOf(PayloadTooLargeException.class);
        assertThatThrownBy(() -> storage.store(tooWide, ImageDirectory.BOOTHS))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void appliesConfiguredFileSizeAndResolutionLimits() throws IOException {
        LocalImageStorage fileSizeLimitedStorage = new LocalImageStorage(
                new ImageStorageProperties(tempDir, "/media/images", DataSize.ofBytes(10), 4096, 4096));
        LocalImageStorage resolutionLimitedStorage = new LocalImageStorage(
                new ImageStorageProperties(tempDir, "/media/images", DataSize.ofMegabytes(5), 2, 3));
        MockMultipartFile largerThanConfiguredSize = new MockMultipartFile(
                "image", "large.png", "image/png", new byte[11]);
        MockMultipartFile widerThanConfiguredResolution = image("wide.png", "image/png", "png", 3, 3);

        assertThatThrownBy(() -> fileSizeLimitedStorage.store(largerThanConfiguredSize, ImageDirectory.BOOTHS))
                .isInstanceOf(PayloadTooLargeException.class);
        assertThatThrownBy(() -> resolutionLimitedStorage.store(widerThanConfiguredResolution, ImageDirectory.BOOTHS))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void deletesOnlyUrlsOwnedByLocalStorage() throws IOException {
        StoredImage managed = storage.store(image("booth.png", "image/png", "png", 10, 10), ImageDirectory.BOOTHS);
        Path managedFile = fileFor(managed);

        storage.deleteIfManaged("https://cdn.example.com/legacy.png");
        assertThat(managedFile).exists();

        storage.deleteIfManaged(managed.publicUrl());
        assertThat(managedFile).doesNotExist();
    }

    private MockMultipartFile image(String filename, String contentType, String format, int width, int height)
            throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, format, output);
        return new MockMultipartFile("image", filename, contentType, output.toByteArray());
    }

    private Path fileFor(StoredImage storedImage) {
        return tempDir.resolve(storedImage.publicUrl().replace("/media/images/", ""));
    }
}
