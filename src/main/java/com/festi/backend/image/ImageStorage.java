package com.festi.backend.image;

import org.springframework.web.multipart.MultipartFile;

public interface ImageStorage {

    StoredImage store(MultipartFile image, ImageDirectory directory);

    void deleteIfManaged(String publicUrl);

    enum ImageDirectory {
        BOOTHS("booths"),
        MENUS("menus");

        private final String path;

        ImageDirectory(String path) {
            this.path = path;
        }

        public String path() {
            return path;
        }
    }

    record StoredImage(String publicUrl) {
    }
}
